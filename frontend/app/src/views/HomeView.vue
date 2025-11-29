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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2rem;
}

.content-wrapper {
  text-align: center;
  animation: fadeIn 0.8s ease-out;
}

.game-title {
  font-size: 8rem;
  font-weight: 900;
  margin: 0;
  letter-spacing: 1rem;
  text-shadow: 4px 4px 8px rgba(0, 0, 0, 0.3);
}

.x-letter {
  color: #ff6b6b;
  animation: bounce 2s ease-in-out infinite;
}

.o-letter {
  color: #4ecdc4;
  animation: bounce 2s ease-in-out 0.5s infinite;
}

.subtitle {
  color: white;
  font-size: 1.5rem;
  margin-top: 0.5rem;
  margin-bottom: 3rem;
  font-weight: 300;
  letter-spacing: 0.3rem;
  text-transform: uppercase;
  opacity: 0.95;
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

.online-card:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.bot-card:hover {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.card-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
  animation: float 3s ease-in-out infinite;
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
