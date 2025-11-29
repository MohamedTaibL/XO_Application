<template>
  <div class="bot-container">
    <div class="content-wrapper">
      <button class="back-button" @click="$router.push('/')">
        ← Back
      </button>
      
      <h1 class="page-title">🤖 Bot Mode</h1>

      <div v-if="!playerMark" class="setup-card">
        <h2 class="setup-title">Choose Your Side</h2>
        <p class="setup-subtitle">Select your mark to begin. 'X' always goes first.</p>
        <div class="choice-buttons">
          <button class="choice-btn x-btn" @click="selectMark('X')">
            Play as X
            <span class="btn-subtext">(Go First)</span>
          </button>
          <button class="choice-btn o-btn" @click="selectMark('O')">
            Play as O
            <span class="btn-subtext">(Go Second)</span>
          </button>
        </div>
      </div>
      
      <div v-else class="game-card">
        <GameBoard ref="gameBoard" :is-bot-game="true" :player-mark="playerMark" />
        <button class="reset-button-ingame" @click="restartAndSwitch">
          Change Sides & Restart
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import GameBoard from '@/components/GameBoard.vue'

type PlayerMark = 'X' | 'O';

const playerMark = ref<PlayerMark | null>(null);
const gameBoard = ref<InstanceType<typeof GameBoard> | null>(null)

const selectMark = (mark: PlayerMark) => {
  playerMark.value = mark;
}

const restartAndSwitch = () => {
  playerMark.value = null;
}
</script>

<style scoped>
.bot-container {
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

.back-button:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateX(-5px);
}

.page-title {
  color: white;
  font-size: 3.5rem;
  font-weight: 800;
  margin-bottom: 1rem;
  text-shadow: 3px 3px 6px rgba(0, 0, 0, 0.3);
}

.game-card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border-radius: 30px;
  padding: 2rem;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.3);
  border: 2px solid rgba(255, 255, 255, 0.2);
  animation: cardFloat 3s ease-in-out infinite;
  position: relative;
}

.reset-button-ingame {
  margin-top: 2rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 0.8rem 2rem;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
}

.reset-button-ingame:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.6);
}

.setup-card {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(15px);
  border-radius: 30px;
  padding: 3rem 4rem;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.18);
  animation: fadeIn 0.5s ease-out;
  max-width: 600px;
  margin: 0 auto;
}

.setup-title {
  color: white;
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
}

.setup-subtitle {
  color: rgba(255, 255, 255, 0.85);
  font-size: 1.1rem;
  margin-bottom: 2.5rem;
}

.choice-buttons {
  display: flex;
  gap: 2rem;
  justify-content: center;
}

.choice-btn {
  border: none;
  border-radius: 15px;
  padding: 1.5rem 2.5rem;
  font-size: 1.5rem;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 10px 25px rgba(0,0,0,0.2);
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.choice-btn:hover {
  transform: translateY(-8px) scale(1.05);
  box-shadow: 0 20px 40px rgba(0,0,0,0.3);
}

.x-btn {
  background: linear-gradient(135deg, #ff6b6b 0%, #f98a8a 100%);
}

.o-btn {
  background: linear-gradient(135deg, #4ecdc4 0%, #6ce0d9 100%);
}

.btn-subtext {
  font-size: 0.9rem;
  font-weight: 500;
  opacity: 0.9;
  margin-top: 0.25rem;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes cardFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
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
