import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import OnlineMenuView from '../views/OnlineMenuView.vue'
import BotView from '../views/BotView.vue'
import CreateRoomView from '../views/CreateRoomView.vue'
import JoinRoomView from '../views/JoinRoomView.vue'
import GameView from '../views/GameView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/online',
      name: 'online',
      component: OnlineMenuView,
    },
    {
      path: '/bot',
      name: 'bot',
      component: BotView,
    },
    {
      path: '/create-room',
      name: 'create-room',
      component: CreateRoomView,
    },
    {
      path: '/join-room',
      name: 'join-room',
      component: JoinRoomView,
    },
    {
      path: '/game',
      name: 'game',
      component: GameView,
    },
  ],
})

export default router
