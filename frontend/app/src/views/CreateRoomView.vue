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

function generateRoomId(len = 6) {
  const chars = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789'
  let out = ''
  for (let i = 0; i < len; i++) out += chars[Math.floor(Math.random() * chars.length)]
  return out
}

function createRoom() {
  if (!username.value || username.value.trim() === '') {
    const el = document.getElementById('username') as HTMLInputElement | null
    el?.focus()
    return
  }
  persistName(username.value.trim())

  // generate a room id and open websocket
  roomId.value = generateRoomId()
  isCreator.value = true
  waiting.value = true
  players.value = [{ id: 'you', name: username.value }]

  connect()

  offHandle = onMessage((msg: any) => {
    console.debug('[CreateRoom] incoming', msg)
    handleIncoming(msg)
  })

  // once connected, send join message
  const trySend = () => {
    if (connected.value) {
      // creator: request server to create the room and register the creator
      send({ type: 'create', gameId: roomId.value, playerId: username.value, name: username.value })
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
      break
    }
    case 'joined':
    case 'created': {
      // confirmation for our own create/join
      if (msg.playerId === username.value) {
        if (msg.players) {
          players.value = Object.values(msg.players).map((p: any) => ({ id: p.id, name: p.name }))
        }
        // if created, we're already registered; keep waiting for others
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
  display:flex;
  align-items:center;
  justify-content:center;
  padding:2rem;
  background: linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%);
}

.content-wrapper {
  width:100%;
  max-width:760px;
  text-align:center;
  position:relative;
  animation: floatIn 0.6s ease;
}

.back-button {
  position:absolute;
  top:-3rem;
  left:0;
  background: rgba(255,255,255,0.12);
  border: 1px solid rgba(255,255,255,0.14);
  color: white;
  padding:0.7rem 1.2rem;
  border-radius:40px;
  cursor:pointer;
  backdrop-filter: blur(8px);
}

.page-title {
  color: #fff;
  font-size:3rem;
  margin-bottom:2rem;
  font-weight:800;
  text-shadow: 0 8px 24px rgba(0,0,0,0.45);
}

.username-card,
.form-card {
  background: linear-gradient(135deg, rgba(255,255,255,0.06), rgba(255,255,255,0.03));
  border-radius:16px;
  padding:1.6rem;
  margin:0 auto 1.6rem auto;
  max-width:520px;
  box-shadow: 0 12px 30px rgba(0,0,0,0.45);
  border: 1px solid rgba(255,255,255,0.06);
  backdrop-filter: blur(8px);
  text-align:left;
}

.username-card label,
.form-card label {
  display:block;
  color: rgba(255,255,255,0.9);
  font-weight:700;
  margin-bottom:0.6rem;
}

.username-card input,
.form-card input {
  width:100%;
  padding:0.9rem 1rem;
  border-radius:10px;
  border:none;
  font-size:1rem;
  box-sizing:border-box;
  background: rgba(255,255,255,0.95);
  color:#111;
  box-shadow: inset 0 2px 6px rgba(0,0,0,0.08);
}

.username-card input:focus,
.form-card input:focus {
  outline:none;
  box-shadow: 0 0 0 6px rgba(120,75,162,0.15);
}

.username-hint,
.note {
  color: rgba(255,255,255,0.75);
  font-size:0.9rem;
  margin-top:0.8rem;
}

.actions {
  display:flex;
  gap:1rem;
  margin-top:1rem;
  align-items:center;
}

.btn {
  padding:0.9rem 1.2rem;
  border-radius:12px;
  border:none;
  cursor:pointer;
  font-weight:700;
  font-size:1rem;
}

.btn.primary {
  background: linear-gradient(90deg,#7b61ff 0%, #ff6bd6 100%);
  color:white;
  box-shadow: 0 8px 20px rgba(123,97,255,0.25);
}

.btn.primary:disabled {
  opacity:0.5;
  cursor:not-allowed;
}

.btn.ghost {
  background: transparent;
  color: #fff;
  border: 1px solid rgba(255,255,255,0.12);
}

@keyframes floatIn {
  from { opacity:0; transform: translateY(12px); }
  to { opacity:1; transform: translateY(0); }
}

@media (max-width:768px){
  .page-title { font-size:2rem; }
  .content-wrapper { padding:0 1rem; }
}
</style>