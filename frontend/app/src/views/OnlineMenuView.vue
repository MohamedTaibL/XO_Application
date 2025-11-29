<template>
  <div class="online-menu-container">
    <div class="content-wrapper">
      <button class="back-button" @click="$router.push('/')">
        ← Back
      </button>

      <h1 class="page-title">Online Play</h1>

      <div class="username-card">
        <label for="username">Enter your username</label>
        <input id="username" v-model="username" placeholder="e.g. Alice" />
        <p class="username-hint">This name will be used when creating or joining a room.</p>
      </div>

      <div class="menu-cards">
        <button class="menu-card create-card" @click="goCreate">
          <div class="card-icon">🎮</div>
          <h2>Create Room</h2>
          <p>Start a new game and invite friends</p>
        </button>

        <button class="menu-card join-card" @click="goJoin">
          <div class="card-icon">🚪</div>
          <h2>Join Room</h2>
          <p>Enter a room code to join</p>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref(localStorage.getItem('xo_username') || '')

function persistName(name: string) {
  localStorage.setItem('xo_username', name)
}

function goCreate() {
  if (!username.value || username.value.trim() === '') {
    // simple visual feedback: focus the input
    const el = document.getElementById('username') as HTMLInputElement | null
    el?.focus()
    return
  }
  persistName(username.value.trim())
  router.push({ name: 'create-room', query: { username: username.value.trim() } })
}

function goJoin() {
  if (!username.value || username.value.trim() === '') {
    const el = document.getElementById('username') as HTMLInputElement | null
    el?.focus()
    return
  }
  persistName(username.value.trim())
  router.push({ name: 'join-room', query: { username: username.value.trim() } })
}
</script>

<style scoped>
.online-menu-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2rem;
}

.content-wrapper {
  text-align: center;
  animation: slideIn 0.6s ease-out;
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

.back-button:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateX(-5px);
}

.page-title {
  color: white;
  font-size: 3.5rem;
  font-weight: 800;
  margin-bottom: 3rem;
  text-shadow: 3px 3px 6px rgba(0, 0, 0, 0.3);
}

.username-card {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  padding: 2rem;
  margin-bottom: 3rem;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
  box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  text-align: left;
}

.username-card label {
  display: block;
  color: white;
  font-size: 1.2rem;
  font-weight: 600;
  margin-bottom: 1rem;
  text-shadow: 1px 1px 3px rgba(0,0,0,0.2);
}

.username-card input {
  width: 100%;
  padding: 1rem;
  border: none;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.7);
  font-size: 1.1rem;
  color: #333;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.1);
  transition: all 0.3s ease;
  box-sizing: border-box; /* Added for better padding behavior */
}

.username-card input:focus {
  outline: none;
  background: white;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.1), 0 0 0 4px rgba(120, 75, 162, 0.5);
}

.username-card input::placeholder {
  color: #888;
}

.username-hint {
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.9rem;
  margin-top: 1rem;
  text-align: center;
}

.menu-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
  max-width: 700px;
  margin: 0 auto;
}

.menu-card {
  background: white;
  border-radius: 20px;
  padding: 3rem 2rem;
  text-decoration: none;
  color: #333;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.menu-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, transparent 0%, rgba(255, 255, 255, 0.1) 100%);
  opacity: 0;
  transition: opacity 0.4s ease;
}

.menu-card:hover {
  transform: translateY(-10px) scale(1.02);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.menu-card:hover::before {
  opacity: 1;
}

.create-card:hover {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  color: white;
}

.join-card:hover {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: white;
}

.card-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
  animation: pulse 2s ease-in-out infinite;
}

.menu-card h2 {
  font-size: 1.8rem;
  margin: 0.5rem 0;
  font-weight: 700;
}

.menu-card p {
  font-size: 1rem;
  opacity: 0.8;
  margin: 0;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

@media (max-width: 768px) {
  .page-title {
    font-size: 2.5rem;
  }
  
  .menu-cards {
    grid-template-columns: 1fr;
    max-width: 350px;
  }
  
  .back-button {
    position: static;
    margin-bottom: 2rem;
  }
}
</style>
