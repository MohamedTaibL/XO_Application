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
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  padding: 2rem;
  position: relative;
  overflow: hidden;
}

.online-menu-container::before {
  content: '';
  position: absolute;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 20% 50%, rgba(138, 43, 226, 0.15) 0%, transparent 50%),
              radial-gradient(circle at 80% 80%, rgba(0, 229, 255, 0.1) 0%, transparent 50%);
  animation: pulse-bg 6s ease-in-out infinite;
}

.content-wrapper {
  text-align: center;
  animation: slideIn 0.6s ease-out;
  position: relative;
  z-index: 1;
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
  font-size: 3.5rem;
  font-weight: 800;
  margin-bottom: 3rem;
  text-shadow: 0 0 20px rgba(138, 43, 226, 0.6),
               3px 3px 6px rgba(0, 0, 0, 0.5);
}

.username-card {
  background: rgba(30, 30, 50, 0.8);
  border-radius: 20px;
  padding: 2rem;
  margin-bottom: 3rem;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5),
              inset 0 1px 0 rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(138, 43, 226, 0.3);
  text-align: left;
}

.username-card label {
  display: block;
  color: #e0e0f0;
  font-size: 1.2rem;
  font-weight: 600;
  margin-bottom: 1rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.username-card input {
  width: 100%;
  padding: 1rem;
  border: 2px solid rgba(138, 43, 226, 0.3);
  border-radius: 10px;
  background: rgba(20, 20, 35, 0.8);
  font-size: 1.1rem;
  color: #e0e0f0;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.username-card input:focus {
  outline: none;
  background: rgba(30, 30, 50, 0.9);
  border-color: rgba(138, 43, 226, 0.6);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.3),
              0 0 0 3px rgba(138, 43, 226, 0.3);
}

.username-card input::placeholder {
  color: rgba(184, 184, 209, 0.5);
}

.username-hint {
  color: rgba(184, 184, 209, 0.7);
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
  background: rgba(30, 30, 50, 0.8);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(138, 43, 226, 0.3);
  border-radius: 20px;
  padding: 3rem 2rem;
  text-decoration: none;
  color: #e0e0f0;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5),
              inset 0 1px 0 rgba(255, 255, 255, 0.1);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.menu-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(138, 43, 226, 0.3), transparent);
  transition: left 0.5s ease;
}

.menu-card:hover::before {
  left: 100%;
}

.menu-card:hover {
  transform: translateY(-10px) scale(1.02);
  box-shadow: 0 20px 50px rgba(138, 43, 226, 0.4),
              0 0 30px rgba(138, 43, 226, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.2);
  border-color: rgba(138, 43, 226, 0.6);
}

.create-card:hover {
  background: linear-gradient(135deg, rgba(67, 233, 123, 0.3) 0%, rgba(56, 249, 215, 0.3) 100%);
  border-color: rgba(67, 233, 123, 0.6);
}

.join-card:hover {
  background: linear-gradient(135deg, rgba(255, 51, 102, 0.3) 0%, rgba(254, 225, 64, 0.3) 100%);
  border-color: rgba(255, 51, 102, 0.6);
}

.card-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
  animation: pulse 2s ease-in-out infinite;
  filter: drop-shadow(0 0 10px rgba(138, 43, 226, 0.5));
}

.menu-card h2 {
  font-size: 1.8rem;
  margin: 0.5rem 0;
  font-weight: 700;
  color: #ffffff;
}

.menu-card p {
  font-size: 1rem;
  opacity: 0.7;
  margin: 0;
  color: #b8b8d1;
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

@keyframes pulse-bg {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.8;
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
