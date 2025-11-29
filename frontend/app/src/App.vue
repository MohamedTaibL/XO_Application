<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div id="app">
    <RouterView />
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background: #0f0c29;
  color: #e0e0f0;
  overflow-x: hidden;
}

#app {
  width: 100%;
  min-height: 100vh;
}

/* Custom scrollbar for dark theme */
::-webkit-scrollbar {
  width: 12px;
  height: 12px;
}

::-webkit-scrollbar-track {
  background: rgba(15, 12, 41, 0.5);
}

::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #8a2be2 0%, #ff3366 100%);
  border-radius: 6px;
  border: 2px solid rgba(15, 12, 41, 0.5);
}

::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #9d3ff5 0%, #ff4d7a 100%);
}

/* Selection color */
::selection {
  background: rgba(138, 43, 226, 0.4);
  color: #ffffff;
}

::-moz-selection {
  background: rgba(138, 43, 226, 0.4);
  color: #ffffff;
}
</style>
