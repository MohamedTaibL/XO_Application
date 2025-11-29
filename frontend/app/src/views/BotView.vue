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
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  padding: 2rem;
  position: relative;
  overflow: hidden;
}

.bot-view-container::before {
  content: '';
  position: absolute;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 20% 80%, rgba(255, 51, 102, 0.15) 0%, transparent 50%),
              radial-gradient(circle at 80% 20%, rgba(138, 43, 226, 0.15) 0%, transparent 50%);
  animation: pulse-glow 8s ease-in-out infinite;
}

.content-wrapper {
  text-align: center;
  animation: fadeIn 0.8s ease-out;
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
  margin-bottom: 1rem;
  text-shadow: 0 0 20px rgba(138, 43, 226, 0.6),
               3px 3px 6px rgba(0, 0, 0, 0.5);
}

.game-card {
  background: rgba(30, 30, 50, 0.6);
  backdrop-filter: blur(20px);
  border-radius: 30px;
  padding: 2rem;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.5),
              inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(138, 43, 226, 0.3);
  animation: cardFloat 3s ease-in-out infinite;
  position: relative;
}

.reset-button-ingame {
  margin-top: 2rem;
  background: linear-gradient(135deg, #ff3366 0%, #8a2be2 100%);
  color: white;
  border: 2px solid rgba(138, 43, 226, 0.5);
  padding: 0.8rem 2rem;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 5px 20px rgba(138, 43, 226, 0.4);
}

.reset-button-ingame:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(138, 43, 226, 0.6);
}

.setup-card {
  background: rgba(30, 30, 50, 0.8);
  backdrop-filter: blur(15px);
  border-radius: 30px;
  padding: 3rem 4rem;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5),
              inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(138, 43, 226, 0.3);
  animation: fadeIn 0.5s ease-out;
  max-width: 600px;
  margin: 0 auto;
}

.setup-title {
  color: #ffffff;
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  text-shadow: 0 0 15px rgba(138, 43, 226, 0.5),
               2px 2px 4px rgba(0, 0, 0, 0.3);
}

.setup-subtitle {
  color: rgba(184, 184, 209, 0.9);
  font-size: 1.1rem;
  margin-bottom: 2.5rem;
}

.choice-buttons {
  display: flex;
  gap: 2rem;
  justify-content: center;
}

.choice-btn {
  border: 2px solid;
  border-radius: 15px;
  padding: 1.5rem 2.5rem;
  font-size: 1.5rem;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.5);
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.choice-btn:hover {
  transform: translateY(-8px) scale(1.05);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.6);
}

.x-btn {
  background: linear-gradient(135deg, rgba(255, 51, 102, 0.3) 0%, rgba(249, 138, 138, 0.3) 100%);
  border-color: rgba(255, 51, 102, 0.6);
}

.x-btn:hover {
  background: linear-gradient(135deg, rgba(255, 51, 102, 0.5) 0%, rgba(249, 138, 138, 0.5) 100%);
  box-shadow: 0 20px 40px rgba(255, 51, 102, 0.4);
}

.o-btn {
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.3) 0%, rgba(108, 224, 217, 0.3) 100%);
  border-color: rgba(0, 229, 255, 0.6);
}

.o-btn:hover {
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.5) 0%, rgba(108, 224, 217, 0.5) 100%);
  box-shadow: 0 20px 40px rgba(0, 229, 255, 0.4);
}

.btn-subtext {
  font-size: 0.9rem;
  font-weight: 500;
  opacity: 0.9;
  margin-top: 0.25rem;
}

@keyframes pulse-glow {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
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
