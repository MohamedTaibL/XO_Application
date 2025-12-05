import { ref } from 'vue'

type MessageHandler = (data: any) => void

// simple singleton composable to manage a websocket connection across views
const socket = ref<WebSocket | null>(null)
const connected = ref(false)
const lastMessage = ref<any>(null)

let messageHandlers: MessageHandler[] = []

const DEFAULT_WS = (import.meta.env.VITE_WS_URL as string) || 'ws://localhost:8081'

function connect(url?: string) {
  const wsUrl = url || DEFAULT_WS
  if (socket.value && socket.value.readyState === WebSocket.OPEN) return

  // close any existing socket first
  if (socket.value) {
    try { socket.value.close() } catch (e) {}
    socket.value = null
  }

  socket.value = new WebSocket(wsUrl)

  socket.value.onopen = () => {
    connected.value = true
  }

  socket.value.onmessage = (ev) => {
    try {
      const parsed = JSON.parse(ev.data)
      lastMessage.value = parsed
      for (const h of messageHandlers) {
        try { h(parsed) } catch (e) { console.error('ws handler error', e) }
      }
    } catch (e) {
      lastMessage.value = ev.data
      for (const h of messageHandlers) { try { h(ev.data) } catch (e) {} }
    }
  }

  socket.value.onclose = () => {
    // notify handlers that the socket has closed so views can react (e.g., room was closed by owner)
    connected.value = false
  // capture lastmessage snapshot for handlers
    const lm = lastMessage.value
    socket.value = null
    for (const h of messageHandlers) {
      try { h({ type: 'socket_closed', lastMessage: lm }) } catch (e) { console.error('ws handler error', e) }
    }
  }

  socket.value.onerror = (err) => {
    console.error('WebSocket error', err)
  }
}

function send(obj: any) {
  if (!socket.value || socket.value.readyState !== WebSocket.OPEN) {
    console.warn('WebSocket not open; cannot send', obj)
    return false
  }
  try {
    socket.value.send(JSON.stringify(obj))
    return true
  } catch (e) {
    console.error('Failed to send ws message', e)
    return false
  }
}

function close() {
  if (socket.value) {
    try { socket.value.close() } catch (e) {}
    socket.value = null
  }
  connected.value = false
}

function onMessage(handler: MessageHandler) {
  messageHandlers.push(handler)
  return () => {
    messageHandlers = messageHandlers.filter(h => h !== handler)
  }
}

export function useWebSocket() {
  return {
    socket,
    connected,
    lastMessage,
    connect,
    send,
    close,
    onMessage
  }
}

export default useWebSocket
