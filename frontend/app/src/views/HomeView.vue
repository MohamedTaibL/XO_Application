<template>
  <div class="home-container">
    <div class="content-wrapper">
      <h1 class="game-title">
        <span class="x-letter">X</span>
        <span class="o-letter">O</span>
      </h1>
      <p class="subtitle">Tic Tac Toe</p>
      
      <div class="menu-cards">
        <router-link to="/online" class="menu-card online-card">
          <div class="card-icon">🌐</div>
          <h2>Play Online</h2>
          <p>Challenge players worldwide</p>
        </router-link>
        
        <router-link to="/bot" class="menu-card bot-card">
          <div class="card-icon">🤖</div>
          <h2>Play Against Bot</h2>
          <p>Practice with AI opponent</p>
        </router-link>
      </div>    
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  padding: 2rem;
  position: relative;
  overflow: hidden;
}

.home-container::before {
  content: '';
  position: absolute;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(138, 43, 226, 0.1) 0%, transparent 70%);
  animation: rotate 20s linear infinite;
}

.content-wrapper {
  text-align: center;
  animation: fadeIn 0.8s ease-out;
  position: relative;
  z-index: 1;
}

.game-title {
  font-size: 8rem;
  font-weight: 900;
  margin: 0;
  letter-spacing: 1rem;
  text-shadow: 0 0 20px rgba(138, 43, 226, 0.5),
               0 0 40px rgba(138, 43, 226, 0.3),
               4px 4px 8px rgba(0, 0, 0, 0.5);
  filter: drop-shadow(0 0 30px rgba(138, 43, 226, 0.4));
}

.x-letter {
  color: #ff3366;
  animation: bounce 2s ease-in-out infinite, glow-x 2s ease-in-out infinite;
}

.o-letter {
  color: #00e5ff;
  animation: bounce 2s ease-in-out 0.5s infinite, glow-o 2s ease-in-out infinite;
}

.subtitle {
  color: #b8b8d1;
  font-size: 1.5rem;
  margin-top: 0.5rem;
  margin-bottom: 3rem;
  font-weight: 300;
  letter-spacing: 0.5rem;
  text-transform: uppercase;
  opacity: 0.9;
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

.online-card:hover {
  background: linear-gradient(135deg, rgba(138, 43, 226, 0.3) 0%, rgba(102, 126, 234, 0.3) 100%);
}

.bot-card:hover {
  background: linear-gradient(135deg, rgba(255, 51, 102, 0.3) 0%, rgba(245, 87, 108, 0.3) 100%);
}

.card-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
  animation: float 3s ease-in-out infinite;
  filter: drop-shadow(0 0 10px rgba(138, 43, 226, 0.5));
}

.menu-card h2 {
  font-size: 1.8rem;
  margin: 0.5rem 0;
  font-weight: 700;
  color: #ffffff;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
}

.menu-card p {
  font-size: 1rem;
  opacity: 0.7;
  margin: 0;
  color: #b8b8d1;
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

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20px);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes glow-x {
  0%, 100% {
    filter: drop-shadow(0 0 10px rgba(255, 51, 102, 0.8));
  }
  50% {
    filter: drop-shadow(0 0 20px rgba(255, 51, 102, 1));
  }
}

@keyframes glow-o {
  0%, 100% {
    filter: drop-shadow(0 0 10px rgba(0, 229, 255, 0.8));
  }
  50% {
    filter: drop-shadow(0 0 20px rgba(0, 229, 255, 1));
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .game-title {
    font-size: 5rem;
  }
  
  .menu-cards {
    grid-template-columns: 1fr;
    max-width: 350px;
  }
}
</style>
