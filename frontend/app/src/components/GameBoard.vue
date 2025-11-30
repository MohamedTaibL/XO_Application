<template>
  <div class="game-board-wrapper">
    <div class="game-info">
      <div class="player-indicator">
        <span class="current-turn">Current Turn:</span>
        <span class="turn-mark" :class="{ 'x-turn': currentTurn === 'X', 'o-turn': currentTurn === 'O' }">
          {{ currentTurn }}
        </span>
      </div>
    </div>

    <div class="board">
      <div
        v-for="(cell, index) in board"
        :key="index"
        class="cell"
        :class="{ 
          'cell-x': cell === 'X', 
          'cell-o': cell === 'O',
          'cell-empty': cell === '' || cell === ' '
        }"
        @click="handleCellClick(index)"
      >
        <span class="cell-content" v-if="cell && cell !== ' '">{{ cell }}</span>
      </div>
    </div>

    <div class="game-status">
      <p class="status-text" v-if="winner">
        <span class="winner-icon">🎉</span>
        Player <span :class="winner === 'X' ? 'x-letter' : 'o-letter'">{{ winner }}</span> Wins!
      </p>
      <p class="status-text" v-else-if="isDraw">
        <span class="draw-icon">🤝</span>
        It's a Draw!
      </p>
    </div>

    <button v-if="!isOnline" class="reset-button" @click="resetGame">
      New Game
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'

const props = defineProps({
  isBotGame: {
    type: Boolean,
    default: false
  },
  isOnline: {
    type: Boolean,
    default: false
  },
  playerMark: {
    type: String as () => 'X' | 'O',
    default: 'X'
  }
})

const emit = defineEmits(['cell-click'])

// Game state
const board = ref<string[]>(Array(9).fill(' '))
const currentTurn = ref<'X' | 'O'>('X')
const winner = ref<string | null>(null)
const isDraw = ref(false)
const gameActive = ref(true)
const winningLine = ref<number[] | null>(null)

const botMark = computed(() => (props.playerMark === 'X' ? 'O' : 'X'))

// --- Game Logic ---

const makeMove = (index: number, player: 'X' | 'O') => {
  if (gameActive.value && board.value[index] === ' ') {
    board.value[index] = player
    const winningCombination = checkWinner(player)
    if (winningCombination) {
      winner.value = player
      winningLine.value = winningCombination
      gameActive.value = false
    } else if (board.value.every(cell => cell !== ' ')) {
      isDraw.value = true
      gameActive.value = false
    } else {
      currentTurn.value = player === 'X' ? 'O' : 'X'
    }
    return true
  }
  return false
}

const handleCellClick = (index: number) => {
  if (!gameActive.value) return

  if (props.isBotGame) {
    if (currentTurn.value !== props.playerMark) return
    makeMove(index, currentTurn.value)
    return
  }

  // multiplayer mode: emit to parent to allow server-authoritative move
  emit('cell-click', index)
}

// define an emit function for TS-aware invocation
// (no-op) emit helper removed in favor of defineEmits call above

watch(currentTurn, (newTurn) => {
  if (props.isBotGame && newTurn === botMark.value && gameActive.value) {
    setTimeout(() => {
      botMove()
    }, 600) // A slight delay for a more natural feel
  }
})

const checkWinner = (player: 'X' | 'O'): number[] | null => {
  const winningCombos = [
    [0, 1, 2], [3, 4, 5], [6, 7, 8], // rows
    [0, 3, 6], [1, 4, 7], [2, 5, 8], // columns
    [0, 4, 8], [2, 4, 6]             // diagonals
  ]
  for (const combo of winningCombos) {
    if (combo.every(index => board.value[index] === player)) {
      return combo
    }
  }
  return null
}

const resetGame = () => {
  board.value = Array(9).fill(' ')
  winner.value = null
  isDraw.value = false
  gameActive.value = true
  winningLine.value = null
  currentTurn.value = 'X' // X always starts
  
  // If bot is X (player chose O), bot should move first
  if (props.isBotGame && botMark.value === 'X') {
    setTimeout(() => {
      botMove()
    }, 600)
  }
}

onMounted(() => {
  resetGame()
})

// --- Bot AI (Minimax Algorithm) ---

const botMove = () => {
  const bestMove = findBestMove(board.value)
  if (bestMove !== -1) {
    makeMove(bestMove, botMark.value)
  }
}

const findBestMove = (currentBoard: string[]): number => {
  let bestVal = -Infinity
  let move = -1

  for (let i = 0; i < 9; i++) {
    if (currentBoard[i] === ' ') {
      currentBoard[i] = botMark.value
      const moveVal = minimax(currentBoard, 0, false)
      currentBoard[i] = ' ' // backtrack

      if (moveVal > bestVal) {
        move = i
        bestVal = moveVal
      }
    }
  }
  return move
}

const minimax = (currentBoard: string[], depth: number, isMax: boolean): number => {
  const score = evaluate(currentBoard)

  if (score === 10) return score - depth
  if (score === -10) return score + depth
  if (currentBoard.every(cell => cell !== ' ')) return 0

  if (isMax) {
    let best = -Infinity
    for (let i = 0; i < 9; i++) {
      if (currentBoard[i] === ' ') {
        currentBoard[i] = botMark.value
        best = Math.max(best, minimax(currentBoard, depth + 1, !isMax))
        currentBoard[i] = ' '
      }
    }
    return best
  } else {
    let best = Infinity
    for (let i = 0; i < 9; i++) {
      if (currentBoard[i] === ' ') {
        currentBoard[i] = props.playerMark
        best = Math.min(best, minimax(currentBoard, depth + 1, !isMax))
        currentBoard[i] = ' '
      }
    }
    return best
  }
}

const evaluate = (b: string[]): number => {
  const winningCombos = [
    [0, 1, 2], [3, 4, 5], [6, 7, 8],
    [0, 3, 6], [1, 4, 7], [2, 5, 8],
    [0, 4, 8], [2, 4, 6]
  ]
  for (const combo of winningCombos) {
    if (combo.every(index => b[index] === botMark.value)) return 10
    if (combo.every(index => b[index] === props.playerMark)) return -10
  }
  return 0
}

// Expose for parent component usage
defineExpose({ resetGame, setState })

// parent can call this to set the board/currentTurn/winner/isDraw from server state
function setState(state: { board?: string[]; currentTurn?: string | null; winner?: string | null; draw?: boolean }) {
  if (state.board) board.value = state.board.map(s => s === " " ? " " : s)
  if (state.currentTurn) currentTurn.value = state.currentTurn as 'X' | 'O'
  winner.value = state.winner || null
  isDraw.value = !!state.draw
  gameActive.value = !(winner.value || isDraw.value)
}
</script>

<style scoped>
.game-board-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2rem;
  padding: 2rem;
}

.game-info {
  background: rgba(30, 30, 50, 0.8);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(138, 43, 226, 0.3);
  padding: 1.5rem 3rem;
  border-radius: 15px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.5),
              inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.player-indicator {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 1.5rem;
}

.current-turn {
  color: #b8b8d1;
  font-weight: 500;
}

.turn-mark {
  font-weight: 900;
  font-size: 2rem;
  transition: all 0.3s ease;
}

.x-turn {
  color: #ff3366;
  text-shadow: 0 0 10px rgba(255, 51, 102, 0.8),
               2px 2px 4px rgba(255, 51, 102, 0.4);
}

.o-turn {
  color: #00e5ff;
  text-shadow: 0 0 10px rgba(0, 229, 255, 0.8),
               2px 2px 4px rgba(0, 229, 255, 0.4);
}

.board {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
  background: linear-gradient(135deg, rgba(138, 43, 226, 0.3) 0%, rgba(102, 126, 234, 0.3) 100%);
  padding: 20px;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5),
              inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(138, 43, 226, 0.4);
}

.cell {
  width: 120px;
  height: 120px;
  background: rgba(20, 20, 35, 0.9);
  border: 2px solid rgba(138, 43, 226, 0.4);
  border-radius: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.5),
              inset 0 1px 0 rgba(255, 255, 255, 0.05);
  position: relative;
  overflow: hidden;
}

.cell::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, transparent 0%, rgba(138, 43, 226, 0.3) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.cell:hover::before {
  opacity: 1;
}

.cell-empty:hover {
  transform: scale(1.05);
  background: rgba(30, 30, 50, 0.9);
  border-color: rgba(138, 43, 226, 0.6);
  box-shadow: 0 10px 30px rgba(138, 43, 226, 0.4),
              0 0 20px rgba(138, 43, 226, 0.2);
}

.cell-content {
  font-size: 4rem;
  font-weight: 900;
  animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.cell-x .cell-content {
  color: #ff3366;
  text-shadow: 0 0 15px rgba(255, 51, 102, 1),
               3px 3px 6px rgba(255, 51, 102, 0.5);
  filter: drop-shadow(0 0 10px rgba(255, 51, 102, 0.8));
}

.cell-o .cell-content {
  color: #00e5ff;
  text-shadow: 0 0 15px rgba(0, 229, 255, 1),
               3px 3px 6px rgba(0, 229, 255, 0.5);
  filter: drop-shadow(0 0 10px rgba(0, 229, 255, 0.8));
}

.game-status {
  min-height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-text {
  font-size: 2rem;
  font-weight: 700;
  margin: 0;
  animation: bounceIn 0.6s ease-out;
  color: #ffffff;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
}

.winner-icon,
.draw-icon {
  font-size: 2.5rem;
  margin-right: 0.5rem;
}

.x-letter {
  color: #ff3366;
  text-shadow: 0 0 10px rgba(255, 51, 102, 0.8);
}

.o-letter {
  color: #00e5ff;
  text-shadow: 0 0 10px rgba(0, 229, 255, 0.8);
}

.reset-button {
  background: linear-gradient(135deg, #ff3366 0%, #8a2be2 100%);
  color: white;
  border: 2px solid rgba(138, 43, 226, 0.5);
  padding: 1rem 3rem;
  border-radius: 50px;
  font-size: 1.2rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 10px 30px rgba(138, 43, 226, 0.4);
}

.reset-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 15px 40px rgba(138, 43, 226, 0.6);
}

.reset-button:active {
  transform: translateY(-1px);
}

@keyframes popIn {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes bounceIn {
  0% {
    transform: scale(0.3);
    opacity: 0;
  }
  50% {
    transform: scale(1.05);
  }
  70% {
    transform: scale(0.9);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .cell {
    width: 90px;
    height: 90px;
  }

  .cell-content {
    font-size: 3rem;
  }

  .board {
    gap: 10px;
    padding: 15px;
  }

  .player-indicator {
    font-size: 1.2rem;
  }

  .turn-mark {
    font-size: 1.5rem;
  }

  .status-text {
    font-size: 1.5rem;
  }
}

@media (max-width: 480px) {
  .cell {
    width: 70px;
    height: 70px;
  }

  .cell-content {
    font-size: 2.5rem;
  }
}
</style>
