<template>
  <div class="create-room-container">
    <div class="content-wrapper">
      <button class="back-button" @click="$router.back()">← Back</button>

      <h1 class="page-title">Create Room</h1>

      <div class="username-card">
        <label for="username">Your display name</label>
        <input id="username" v-model="username" placeholder="e.g. Alice" />
        <p class="username-hint">This name will be used in the room. You can change it anytime.</p>
      </div>

      <div v-if="!roomId" class="form-card">
        <label for="roomName">Room name (optional)</label>
        <input id="roomName" v-model="roomName" placeholder="Friendly room name" />
        <div class="actions">
          <button class="btn primary" :disabled="!canProceed" @click="createRoom">
            Create Room
          </button>
          <button class="btn ghost" @click="clearName">Clear Name</button>
        </div>
        <p class="note">Tip: Share the room code with friends to let them join.</p>
      </div>

      <div v-else class="form-card room-card">
        <h3 class="room-code">Room Code: <code>{{ roomId }}</code></h3>
        <p class="note">Share this code with someone to let them join. When another player connects you'll see them appear below.</p>

        <div class="players-list">
          <div v-for="p in players" :key="p.id" class="player-item">
            <strong>{{ p.name || p.id }}</strong>
            <span class="tag" v-if="p.id === username">(you)</span>
          </div>
        </div>

          <div v-if="isCreator && players.length >= 2" class="start-controls" style="margin-top:1rem;">
            <p class="note">Start the game and choose who goes first:</p>
            <div class="actions">
              <button class="btn primary" @click="startGame(true)">Start — You First</button>
              <button class="btn ghost" @click="startGame(false)">Start — You Second</button>
            </div>
          </div>

        <div class="actions" style="margin-top:1rem;">
          <button class="btn primary" @click="copyCode">Copy Code</button>
          <button class="btn ghost" @click="closeRoom">Close Room</button>
        </div>

        <p class="note" v-if="waiting">Waiting for players to join…</p>
      </div>
    </div>
  </div>
  <div class="debug-panel">
    <h4>Debug</h4>
    <div>Status: <strong>{{ connected ? 'connected' : 'disconnected' }}</strong></div>
    <div>Last message: <pre class="raw">{{ JSON.stringify(lastMessage, null, 2) }}</pre></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useWebSocket } from '@/composables/useWebSocket'

const route = useRoute()
const router = useRouter()

const { connect, send, connected, onMessage, close, lastMessage } = useWebSocket()

const username = ref<string>('')
const roomName = ref<string>('')

const roomId = ref<string | null>(null)
const isCreator = ref(false)
const waiting = ref(false)
const players = ref<Array<{ id: string; name?: string }>>([])
let offHandle: (() => void) | null = null

onMounted(() => {
  // prefer query, fall back to localStorage
  username.value = (route.query.username as string) || localStorage.getItem('xo_username') || ''
  if (username.value) localStorage.setItem('xo_username', username.value)
  // if returning from a game, restore roomId and connected state if possible
  const qgid = route.query.gameId as string | undefined
  if (qgid) {
    roomId.value = qgid
    isCreator.value = (route.query.role as string) === 'creator' || true
    // try to populate players from last message if available (use .value to access the ref)
    if (lastMessage.value && lastMessage.value.gameId === qgid && lastMessage.value.players) {
      players.value = Object.values(lastMessage.value.players).map((p: any) => ({ id: p.id, name: p.name }))
    }
    // ensure we're connected so creator can start again
    connect()
    offHandle = onMessage((msg: any) => {
      console.debug('[CreateRoom] incoming', msg)
      handleIncoming(msg)
    })
    // Request fresh game state from server (in case lastMessage was stale)
    const requestSync = () => {
      if (connected.value && roomId.value && username.value) {
        send({ type: 'sync', gameId: roomId.value, playerId: username.value })
        console.debug('[CreateRoom] Sent sync request for gameId:', roomId.value)
      } else {
        setTimeout(requestSync, 200)
      }
    }
    requestSync()
  }
})

function persistName(name: string) {
  localStorage.setItem('xo_username', name)
}

function createRoom() {
  if (!username.value || username.value.trim() === '') {
    const el = document.getElementById('username') as HTMLInputElement | null
    el?.focus()
    return
  }
  persistName(username.value.trim())

  // Server will generate room ID, so we don't need to do it here
  isCreator.value = true
  waiting.value = true
  players.value = [{ id: 'you', name: username.value }]

  connect()

  offHandle = onMessage((msg: any) => {
    console.debug('[CreateRoom] incoming', msg)
    handleIncoming(msg)
  })

  // once connected, send create message (without gameId)
  const trySend = () => {
    if (connected.value) {
      // creator: request server to create the room and register the creator
      // Server will generate and return the gameId
      send({ type: 'create', playerId: username.value, name: username.value })
    } else {
      // wait a bit and retry
      setTimeout(trySend, 200)
    }
  }
  trySend()

}

onBeforeUnmount(() => {
  if (offHandle) {
    try { offHandle() } catch (e) {}
    offHandle = null
  }
  // do not close socket here; keep connection alive for room creator until they leave
})

function clearName() {
  username.value = ''
  localStorage.removeItem('xo_username')
}

function handleIncoming(msg: any) {
  console.debug('[CreateRoom] handleIncoming', msg)
  if (!msg || !msg.type) return
  switch (msg.type) {
    case 'synced': {
      // Received fresh game state from server
      if (msg.players) {
        players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name }))
        console.debug('[CreateRoom] Synced players:', players.value)
        if (players.value.length >= 2) {
          waiting.value = false
        }
      }
      if (msg.gameId && !roomId.value) {
        roomId.value = msg.gameId
      }
      break
    }
    case 'created': {
      // Server has created the room and returned the gameId
      if (msg.playerId === username.value) {
        if (msg.gameId) {
          roomId.value = msg.gameId
          console.debug('[CreateRoom] Room created with gameId:', roomId.value)
        }
        if (msg.players) {
          players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name }))
        }
      }
      break
    }
    case 'joined': {
      // confirmation for our own join
      if (msg.playerId === username.value) {
        if (msg.players) {
          players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name }))
        }
      }
      break
    }
    case 'player_joined': {
      // prefer full players map if provided; otherwise add single player
      if (msg.players) {
        players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name }))
      } else {
        const pid = msg.playerId
        if (pid && pid !== username.value) {
          players.value.push({ id: pid, name: msg.name || pid })
        }
      }
      // creator: when second player joins, you can proceed to start the game
      if (isCreator.value && players.value.length >= 2) {
        waiting.value = false
        // here you could navigate to the game view or signal start
        // e.g. router.push({ name: 'game', query: { gameId: roomId.value } })
      }
      break
    }
    case 'game_started': {
      if (msg.gameId === roomId.value) {
        // navigate to game view when server starts the match
        router.push({ name: 'game', query: { gameId: roomId.value, playerId: username.value, role: 'creator' } })
      }
      break
    }
    case 'player_left': {
      // prefer authoritative players map if provided, otherwise remove by id
      if (msg.players) {
        players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name }))
      } else {
        const pid = msg.playerId
        players.value = players.value.filter(p => p.id !== pid)
      }
      break
    }
    case 'error': {
      console.error('[CreateRoom] Error from server:', msg)
      // Handle different error types
      if (msg.message === 'unknown_game' || msg.message === 'game_does_not_exist') {
        alert('Game does not exist. Redirecting to online menu...')
        router.push({ name: 'online' })
      } else if (msg.message === 'game_full') {
        alert('Room is full. Redirecting to online menu...')
        router.push({ name: 'online' })
      } else if (msg.message === 'already_in_game') {
        alert('You are already in a game. Please close that room first.')
        // Optionally navigate to the existing game
        if (msg.gameId) {
          router.push({ name: 'game', query: { gameId: msg.gameId, playerId: username.value } })
        }
      } else {
        alert(`Error: ${msg.message || 'Unknown error'}`)
        router.push({ name: 'online' })
      }
      break
    }
  }
}

const canProceed = computed(() => username.value && username.value.trim() !== '')

function copyCode() {
  if (roomId.value) {
    try { navigator.clipboard.writeText(roomId.value) } catch (e) { /* ignore */ }
  }
}

function closeRoom() {
  // notify server to destroy the room so no one can join it again
  try {
    if (connected.value && roomId.value) {
      send({ type: 'close', gameId: roomId.value, playerId: username.value })
    }
  } catch (e) { /* ignore */ }

  // close socket and clear local UI
  try { close() } catch (e) {}
  roomId.value = null
  isCreator.value = false
  waiting.value = false
  players.value = []
}

function startGame(creatorStarts: boolean) {
  if (!roomId.value) return
  try {
    const payload = { type: 'start', gameId: roomId.value, playerId: username.value, creatorStarts }
    if (connected.value) send(payload)
    else {
      // wait until connected
      const t = setInterval(() => {
        if (connected.value) {
          send(payload)
          clearInterval(t)
        }
      }, 200)
    }
  } catch (e) {
    console.error('startGame error', e)
  }
}
</script>

<style scoped>
.debug-panel {
  position: fixed;
  right: 1rem;
  bottom: 1rem;
  background: rgba(0,0,0,0.6);
  color: #fff;
  padding: 0.8rem 1rem;
  border-radius: 8px;
  max-width: 320px;
  font-size: 0.85rem;
}
.debug-panel .raw {
  max-height: 240px;
  overflow: auto;
  background: rgba(255,255,255,0.03);
  padding: 0.5rem;
  border-radius: 6px;
}
</style>

<style scoped>
.create-room-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  position: relative;
  overflow: hidden;
}

.create-room-container::before {
  content: '';
  position: absolute;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 30% 20%, rgba(255, 51, 102, 0.1) 0%, transparent 50%),
              radial-gradient(circle at 70% 70%, rgba(138, 43, 226, 0.15) 0%, transparent 50%);
  animation: pulse-bg 8s ease-in-out infinite;
}

.content-wrapper {
  width: 100%;
  max-width: 760px;
  text-align: center;
  position: relative;
  z-index: 1;
  animation: floatIn 0.6s ease;
}

.back-button {
  position: absolute;
  top: -3rem;
  left: 0;
  background: rgba(30, 30, 50, 0.8);
  border: 2px solid rgba(138, 43, 226, 0.4);
  color: #e0e0f0;
  padding: 0.7rem 1.2rem;
  border-radius: 40px;
  cursor: pointer;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
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
  margin-bottom: 2rem;
  font-weight: 800;
  text-shadow: 0 0 20px rgba(138, 43, 226, 0.6),
               0 8px 24px rgba(0, 0, 0, 0.5);
}

.username-card,
.form-card {
  background: rgba(30, 30, 50, 0.8);
  border-radius: 16px;
  padding: 1.6rem;
  margin: 0 auto 1.6rem auto;
  max-width: 520px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.5),
              inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(138, 43, 226, 0.3);
  backdrop-filter: blur(10px);
  text-align: left;
}

.username-card label,
.form-card label {
  display: block;
  color: #e0e0f0;
  font-weight: 700;
  margin-bottom: 0.6rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.username-card input,
.form-card input {
  width: 100%;
  padding: 0.9rem 1rem;
  border-radius: 10px;
  border: 2px solid rgba(138, 43, 226, 0.3);
  font-size: 1rem;
  box-sizing: border-box;
  background: rgba(20, 20, 35, 0.8);
  color: #e0e0f0;
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
}

.username-card input:focus,
.form-card input:focus {
  outline: none;
  border-color: rgba(138, 43, 226, 0.6);
  background: rgba(30, 30, 50, 0.9);
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.3),
              0 0 0 3px rgba(138, 43, 226, 0.3);
}

.username-card input::placeholder,
.form-card input::placeholder {
  color: rgba(184, 184, 209, 0.5);
}

.username-hint,
.note {
  color: rgba(184, 184, 209, 0.7);
  font-size: 0.9rem;
  margin-top: 0.8rem;
}

.actions {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
  align-items: center;
}

.btn {
  padding: 0.9rem 1.2rem;
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

.players-list {
  background: rgba(20, 20, 35, 0.6);
  border: 2px solid rgba(138, 43, 226, 0.2);
  border-radius: 12px;
  padding: 1.2rem;
  margin: 1.5rem 0;
  min-height: 60px;
  text-align: left;
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

.start-controls {
  background: rgba(138, 43, 226, 0.15);
  border: 2px solid rgba(138, 43, 226, 0.4);
  border-radius: 12px;
  padding: 1rem;
  margin: 1.5rem 0;
}

@keyframes floatIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
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

@media (max-width: 768px) {
  .page-title {
    font-size: 2rem;
  }
  .content-wrapper {
    padding: 0 1rem;
  }
  .back-button {
    position: static;
    margin-bottom: 1rem;
  }
}
</style>