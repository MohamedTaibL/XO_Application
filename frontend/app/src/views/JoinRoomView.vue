<template>
  <div class="join-room-container">
    <div class="content-wrapper">
      <button class="back-button" @click="$router.push('/online')">
        ← Back
      </button>

      <h1 class="page-title">Join Room</h1>

      <div class="player-row">Player: <strong>{{ username || '—' }}</strong></div>

      <div v-if="!joined" class="room-card">
        <label for="roomCode">Room Code</label>
        <input id="roomCode" v-model="roomCode" placeholder="e.g. ABC123" />
        <div class="actions" style="margin-top:1rem;">
          <button class="btn primary" :disabled="!canProceed" @click="joinRoom">Join Room</button>
        </div>
        <p class="hint">Enter the room code shared with you and press Join.</p>
      </div>

      <div v-else class="room-card">
        <h3 class="room-code">Joined Room: <code>{{ roomCode }}</code></h3>
        <p class="note">Waiting for game or other players. You'll receive updates below.</p>
        <div class="players-list">
          <div v-for="p in players" :key="p.id" class="player-item">
            <strong>{{ p.name || p.id }}</strong>
            <span class="tag" v-if="p.id === username">(you)</span>
          </div>
        </div>
        <div class="actions" style="margin-top:1rem;">
          <button class="btn ghost" @click="leaveRoom">Leave</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useWebSocket } from '@/composables/useWebSocket'

const route = useRoute()
const router = useRouter()
const { connect, send, connected, onMessage, close, lastMessage } = useWebSocket()

const username = ref<string | null>(null)
const roomCode = ref<string>('')
const joined = ref(false)
const players = ref<Array<{ id: string; name?: string }>>([])

onMounted(() => {
  username.value = (route.query.username as string) || localStorage.getItem('xo_username') || null
  // if routed back from a finished game, restore joined state without forcing reconnect
  const qgid = route.query.gameId as string | undefined
  if (qgid) {
    roomCode.value = qgid
    connect()
    // reuse the same onMessage handler as when joining so UI stays in sync
    const off = onMessage((msg: any) => {
      if (!msg || !msg.type) return
      if (msg.type === 'synced' && msg.gameId === roomCode.value) {
        if (msg.players) {
          players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name }))
          console.debug('[JoinRoom] Synced players:', players.value)
        }
      }
      if (msg.type === 'joined' && msg.gameId === roomCode.value) {
        joined.value = true
        if (msg.players) players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name }))
      }
      if (msg.type === 'player_joined' && msg.gameId === roomCode.value) {
        if (msg.players) players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name }))
        else players.value.push({ id: msg.playerId, name: msg.name || msg.playerId })
      }
      if (msg.type === 'player_left' && msg.gameId === roomCode.value) {
        if (msg.players) players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name }))
        else players.value = players.value.filter(p => p.id !== msg.playerId)
      }
    })

    // populate UI from lastMessage if available (useful immediately after game_over)
    if (lastMessage.value && lastMessage.value.gameId === qgid && lastMessage.value.players) {
      players.value = Object.values(lastMessage.value.players).map((p: any) => ({ id: p.id, name: p.name }))
      joined.value = true
    }

    // Request fresh game state from server (in case lastMessage was stale)
    const requestSync = () => {
      if (connected.value && roomCode.value && username.value) {
        send({ type: 'sync', gameId: roomCode.value, playerId: username.value })
        console.debug('[JoinRoom] Sent sync request for gameId:', roomCode.value)
      } else {
        setTimeout(requestSync, 200)
      }
    }
    requestSync()

    // cleanup when leaving the view
    onBeforeUnmount(() => {
      try { off() } catch (e) {}
    })
  }
})

function persistName(name: string) {
  localStorage.setItem('xo_username', name)
}

function joinRoom() {
  if (!username.value || username.value.trim() === '') {
    const el = document.getElementById('username') as HTMLInputElement | null
    el?.focus()
    return
  }
  if (!roomCode.value || roomCode.value.trim() === '') {
    const el = document.getElementById('roomCode') as HTMLInputElement | null
    el?.focus()
    return
  }

  persistName(username.value.trim())
  connect()

  const off = onMessage((msg: any) => {
    if (!msg || !msg.type) return
    // handle socket closed event forwarded by composable
    if (msg.type === 'socket_closed') {
      // if lastMessage indicates the room was closed, or we were joined to this room, route to online
      const lm = msg.lastMessage
      if ((lm && lm.type === 'room_closed' && lm.gameId === roomCode.value) || joined.value) {
        alert('Disconnected: the room was closed by the host.')
        try { close() } catch (e) {}
        router.push({ name: 'online' })
      }
      return
    }
    if (msg.type === 'error' && msg.gameId === roomCode.value) {
      // server reported unknown game or other error
      console.warn('WS error', msg)
      if (msg.message === 'unknown game') {
        alert('Room not found. Check the code and try again.')
      }
      return
    }

    if (msg.type === 'joined' && msg.gameId === roomCode.value) {
      // our join ack
      joined.value = true
      if (msg.players) players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name }))
    }

    if (msg.type === 'game_started' && msg.gameId === roomCode.value) {
      // server started the game; route into game view
      router.push({ name: 'game', query: { gameId: roomCode.value, playerId: username.value, role: 'player' } })
    }

    if (msg.type === 'player_joined' && msg.gameId === roomCode.value) {
      if (msg.players) {
        players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name }))
      } else {
        players.value.push({ id: msg.playerId, name: msg.name || msg.playerId })
      }
    }

    if (msg.type === 'player_left' && msg.gameId === roomCode.value) {
      if (msg.players) {
        players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name }))
      } else {
        players.value = players.value.filter(p => p.id !== msg.playerId)
      }
    }
  })

  const trySend = () => {
    if (connected.value) {
      send({ type: 'join', gameId: roomCode.value, playerId: username.value, name: username.value })
    } else {
      setTimeout(trySend, 200)
    }
  }
  trySend()

  // cleanup when component unmounts
  onBeforeUnmount(() => {
    try { off() } catch (e) {}
  })
}

function leaveRoom() {
  // notify server that we're leaving so peers (creator) receive the update
  try {
    if (connected.value) send({ type: 'leave', gameId: roomCode.value, playerId: username.value })
  } catch (e) { /* ignore */ }

  // close our websocket and reset local UI state
  try { close() } catch (e) {}
  joined.value = false
  players.value = []
}

const canProceed = computed(() => !!username.value && username.value.trim() !== '' && roomCode.value.trim() !== '')
</script>

<style scoped>
.join-room-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  padding: 2rem;
}

.content-wrapper {
  text-align: center;
  animation: zoomIn 0.6s ease-out;
  position: relative;
  width: 100%;
  max-width: 500px;
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

.back-button:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateX(-5px);
}

.page-title {
  color: white;
  font-size: 3rem;
  font-weight: 800;
  margin-bottom: 2rem;
  text-shadow: 3px 3px 6px rgba(0, 0, 0, 0.3);
}

.room-card {
  background: white;
  border-radius: 20px;
  padding: 3rem 2rem;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.icon-container {
  margin-bottom: 2rem;
}

.join-icon {
  font-size: 5rem;
  animation: swing 2s ease-in-out infinite;
}

.description {
  color: #333;
  font-size: 1.3rem;
  margin-bottom: 1rem;
  font-weight: 600;
}

.hint {
  color: #666;
  font-size: 1rem;
  margin: 0;
}

@keyframes zoomIn {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes swing {
  0%, 100% {
    transform: rotate(-10deg);
  }
  50% {
    transform: rotate(10deg);
  }
}

@media (max-width: 768px) {
  .page-title {
    font-size: 2.5rem;
  }
  
  .back-button {
    position: static;
    margin-bottom: 2rem;
  }
}
</style>
