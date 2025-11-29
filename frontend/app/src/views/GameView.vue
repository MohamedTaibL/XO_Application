<template>
  <div class="game-container">
    <div class="content-wrapper">
      <button class="back-button" @click="leaveEarly">← Back</button>
      <h1 class="page-title">Online Game</h1>

      <div class="hud">
        <div class="hud-title">Players</div>
        <ul>
          <li v-for="p in players" :key="p.id" class="hud-item">
            <strong>{{ p.name || p.id }}</strong>
            <span class="mark" :class="p.mark === 'X' ? 'x' : 'o'">{{ p.mark || '—' }}</span>
            <span v-if="p.id === playerId" class="you-tag">(you)</span>
          </li>
        </ul>
      </div>

      <div class="game-card">
        <GameBoard ref="gameBoard" :is-bot-game="false" @cell-click="onLocalCellClick" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import GameBoard from '@/components/GameBoard.vue'
import { useWebSocket } from '@/composables/useWebSocket'

const route = useRoute()
const router = useRouter()
const { onMessage, send, connected, close } = useWebSocket()

const gameBoard = ref<InstanceType<typeof GameBoard> | null>(null)
const players = ref<Array<{ id: string; name?: string; mark?: string }>>([])

const gameId = (route.query.gameId as string) || ''
const playerId = (route.query.playerId as string) || ''
const role = (route.query.role as string) || 'player'

let off: (() => void) | null = null
const onLocalCellClick = (index: number) => {
  const x = index % 3
  const y = Math.floor(index / 3)
  if (connected.value) send({ type: 'move', gameId, playerId, x, y })
}

onMounted(() => {
  off = onMessage((msg: any) => {
    if (!msg) return
    // support socket_closed pseudo-message from the composable
    if (msg.type === 'socket_closed') {
      const lm = msg.lastMessage
      if (lm && lm.type === 'room_closed' && lm.gameId === gameId) {
        alert('Room closed by host — returning to lobby.')
        try { close() } catch (e) {}
        router.push({ name: 'online' })
        return
      }
      // generic socket close: route back to lobby
      try { close() } catch (e) {}
      router.push({ name: 'online' })
      return
    }
    if (!msg || msg.gameId !== gameId) return

    switch (msg.type) {
      case 'game_started': {
        // authoritative state
        if (gameBoard.value && typeof gameBoard.value.setState === 'function') {
          gameBoard.value.setState({ board: msg.board, currentTurn: msg.currentTurn })
        }
        if (msg.players) players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name, mark: p.mark }))
        break
      }
      case 'move': {
        if (gameBoard.value && typeof gameBoard.value.setState === 'function') {
          gameBoard.value.setState({ board: msg.board, currentTurn: msg.currentTurn })
        }
        if (msg.players) players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name, mark: p.mark }))
        break
      }
      case 'game_over': {
        if (gameBoard.value && typeof gameBoard.value.setState === 'function') {
          gameBoard.value.setState({ board: msg.board, currentTurn: null, winner: msg.winner === 'DRAW' ? null : msg.winner, draw: msg.winner === 'DRAW' })
        }
        // after a short delay, route back to the appropriate view
        setTimeout(() => {
          // keep the websocket connection open and route back while keeping room association
          if (role === 'creator') router.push({ name: 'create-room', query: { gameId, role: 'creator' } })
          else router.push({ name: 'join-room', query: { gameId, role: 'player' } })
        }, 2000)
        break
      }
      case 'room_closed': {
        // room closed by creator while in-game
        alert('Room closed by host. Returning to lobby.')
        try { close() } catch (e) {}
        router.push({ name: 'online' })
        break
      }
    }
  })
})

onBeforeUnmount(() => {
  if (off) try { off() } catch (e) {}
})

function leaveEarly() {
  try { send({ type: 'leave', gameId, playerId }) } catch (e) {}
  try { close() } catch (e) {}
  if (role === 'creator') router.push({ name: 'create-room' })
  else router.push({ name: 'join-room' })
}
</script>


<style scoped>
/* mimic BotView styles for consistent look */
.game-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  padding: 2rem;
}

.content-wrapper {
  text-align: center;
  animation: fadeIn 0.8s ease-out;
  position: relative;
}

.back-button {
  position: absolute;
  top: -3rem;
  left: 0;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
  color: white;
  padding: 0.8rem 1.5rem;
  border-radius: 50px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.game-card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border-radius: 30px;
  padding: 2rem;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.3);
  border: 2px solid rgba(255, 255, 255, 0.2);
}

.page-title { color: white; font-size: 2rem; margin-bottom: 1rem }

.hud {
  margin: 1rem auto 1.6rem auto;
  background: rgba(255,255,255,0.06);
  padding: 0.8rem 1.2rem;
  border-radius: 12px;
  max-width: 520px;
  color: #fff;
  text-align: left;
}
.hud-title { font-weight:700; margin-bottom:0.5rem }
.hud ul { list-style:none; padding:0; margin:0; display:flex; gap:1rem; align-items:center }
.hud-item { display:flex; gap:0.6rem; align-items:center }
.mark { padding:0.15rem 0.5rem; border-radius:6px; font-weight:700 }
.mark.x { background: rgba(255,107,107,0.15); color: #ff6b6b }
.mark.o { background: rgba(78,205,196,0.12); color: #4ecdc4 }
.you-tag { margin-left:0.4rem; opacity:0.9 }
</style>
