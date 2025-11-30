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
        <input id="roomCode" v-model="roomCode" @input="handleRoomCodeInput" placeholder="e.g. ABC123" />
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

const normalizeRoomCode = (value: string | null | undefined) => (value ? value.trim().toUpperCase() : '')

onMounted(() => {
  username.value = (route.query.username as string) || localStorage.getItem('xo_username') || null
  // if routed back from a finished game, restore joined state without forcing reconnect
  const qgid = route.query.gameId as string | undefined
  if (qgid) {
    roomCode.value = normalizeRoomCode(qgid)
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
    if (lastMessage.value && normalizeRoomCode(lastMessage.value.gameId) === roomCode.value && lastMessage.value.players) {
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

function handleRoomCodeInput(e: Event) {
  const target = e.target as HTMLInputElement | null
  if (!target) return
  roomCode.value = normalizeRoomCode(target.value)
}

function joinRoom() {
  if (!username.value || username.value.trim() === '') {
    const el = document.getElementById('username') as HTMLInputElement | null
    el?.focus()
    return
  }
  roomCode.value = normalizeRoomCode(roomCode.value)
  if (!roomCode.value) {
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
      // if lastMessage indicates the room was closed, or we were joined to this room, route to online silently
      const lm = msg.lastMessage
      if ((lm && lm.type === 'room_closed' && lm.gameId === roomCode.value) || joined.value) {
        try { close() } catch (e) {}
        router.push({ name: 'online' })
      }
      return
    }
    if (msg.type === 'error') {
      // server reported unknown game or other error - handle silently
      console.debug('[JoinRoom] Error from server:', msg)
      const fatalErrors = ['unknown_game', 'unknown game', 'game_full']
      if (fatalErrors.some(e => msg.message?.includes(e))) {
        router.push({ name: 'online' })
      } else if (msg.message === 'already_in_game' && msg.gameId) {
        // Navigate to existing game silently
        router.push({ name: 'game', query: { gameId: msg.gameId, playerId: username.value } })
      } else {
        // Non-fatal errors - just log
        console.debug('[JoinRoom] Non-fatal error ignored:', msg.message)
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
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  padding: 2rem;
  position: relative;
  overflow: hidden;
}

.join-room-container::before {
  content: '';
  position: absolute;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 70% 30%, rgba(255, 51, 102, 0.15) 0%, transparent 50%),
              radial-gradient(circle at 30% 70%, rgba(0, 229, 255, 0.1) 0%, transparent 50%);
  animation: pulse-bg 8s ease-in-out infinite;
}

.content-wrapper {
  text-align: center;
  animation: zoomIn 0.6s ease-out;
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 500px;
}

.back-button {
  position: absolute;
  top: -3rem;
  left: 0;
  background: rgba(30, 30, 50, 0.8);
  border: 2px solid rgba(138, 43, 226, 0.4);
  color: #e0e0f0;
  padding: 0.8rem 1.5rem;
  border-radius: 50px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
}

.back-button:hover {
  background: rgba(138, 43, 226, 0.3);
  border-color: rgba(138, 43, 226, 0.6);
  transform: translateX(-5px);
  box-shadow: 0 4px 20px rgba(138, 43, 226, 0.4);
}

.page-title {
  color: #ffffff;
  font-size: 3rem;
  font-weight: 800;
  margin-bottom: 2rem;
  text-shadow: 0 0 20px rgba(138, 43, 226, 0.6),
               3px 3px 6px rgba(0, 0, 0, 0.5);
}

.player-row {
  color: #b8b8d1;
  font-size: 1rem;
  margin-bottom: 1.5rem;
}

.player-row strong {
  color: #ffffff;
  font-weight: 700;
}

.room-card {
  background: rgba(30, 30, 50, 0.8);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(138, 43, 226, 0.3);
  border-radius: 20px;
  padding: 3rem 2rem;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5),
              inset 0 1px 0 rgba(255, 255, 255, 0.1);
  text-align: left;
}

.room-card label {
  display: block;
  color: #e0e0f0;
  font-size: 1.2rem;
  font-weight: 700;
  margin-bottom: 0.8rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.room-card input {
  width: 100%;
  padding: 1rem;
  border-radius: 10px;
  border: 2px solid rgba(138, 43, 226, 0.3);
  font-size: 1.1rem;
  box-sizing: border-box;
  background: rgba(20, 20, 35, 0.8);
  color: #e0e0f0;
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  text-align: center;
  font-weight: 700;
}

.room-card input:focus {
  outline: none;
  border-color: rgba(138, 43, 226, 0.6);
  background: rgba(30, 30, 50, 0.9);
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.3),
              0 0 0 3px rgba(138, 43, 226, 0.3);
}

.room-card input::placeholder {
  color: rgba(184, 184, 209, 0.5);
  text-transform: none;
  letter-spacing: normal;
}

.room-code {
  color: #ffffff;
  font-size: 1.5rem;
  margin-bottom: 1rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.room-code code {
  display: inline-block;
  background: rgba(138, 43, 226, 0.3);
  border: 2px solid rgba(138, 43, 226, 0.5);
  padding: 0.3rem 1rem;
  border-radius: 8px;
  font-family: 'Courier New', monospace;
  font-size: 1.3rem;
  color: #00e5ff;
  text-shadow: 0 0 10px rgba(0, 229, 255, 0.6);
  letter-spacing: 0.15em;
}

.note {
  color: #b8b8d1;
  font-size: 0.95rem;
  margin: 1rem 0;
  line-height: 1.5;
}

.players-list {
  background: rgba(20, 20, 35, 0.6);
  border: 2px solid rgba(138, 43, 226, 0.2);
  border-radius: 12px;
  padding: 1.2rem;
  margin: 1.5rem 0;
  min-height: 60px;
}

.player-item {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 0.8rem;
  margin-bottom: 0.5rem;
  background: rgba(30, 30, 50, 0.6);
  border-radius: 8px;
  border: 1px solid rgba(138, 43, 226, 0.3);
  transition: all 0.3s ease;
}

.player-item:last-child {
  margin-bottom: 0;
}

.player-item:hover {
  background: rgba(138, 43, 226, 0.2);
  border-color: rgba(138, 43, 226, 0.5);
  transform: translateX(5px);
}

.player-item strong {
  color: #ffffff;
  font-size: 1.1rem;
}

.tag {
  background: rgba(0, 229, 255, 0.2);
  color: #00e5ff;
  padding: 0.2rem 0.6rem;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  border: 1px solid rgba(0, 229, 255, 0.4);
}

.actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  align-items: center;
}

.btn {
  padding: 0.9rem 2rem;
  border-radius: 12px;
  border: none;
  cursor: pointer;
  font-weight: 700;
  font-size: 1rem;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.btn:hover::before {
  width: 300px;
  height: 300px;
}

.btn.primary {
  background: linear-gradient(135deg, #ff3366 0%, #8a2be2 100%);
  color: white;
  box-shadow: 0 8px 20px rgba(138, 43, 226, 0.4);
  border: 2px solid rgba(138, 43, 226, 0.5);
}

.btn.primary:hover {
  box-shadow: 0 12px 30px rgba(138, 43, 226, 0.6);
  transform: translateY(-2px);
}

.btn.primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.btn.ghost {
  background: rgba(30, 30, 50, 0.8);
  color: #e0e0f0;
  border: 2px solid rgba(138, 43, 226, 0.4);
}

.btn.ghost:hover {
  background: rgba(138, 43, 226, 0.2);
  border-color: rgba(138, 43, 226, 0.6);
}

.icon-container {
  margin-bottom: 2rem;
}

.join-icon {
  font-size: 5rem;
  animation: swing 2s ease-in-out infinite;
  filter: drop-shadow(0 0 10px rgba(138, 43, 226, 0.5));
}

.description {
  color: #e0e0f0;
  font-size: 1.3rem;
  margin-bottom: 1rem;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.hint {
  color: #b8b8d1;
  font-size: 0.9rem;
  margin: 0;
  text-align: center;
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

@keyframes pulse-bg {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
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
