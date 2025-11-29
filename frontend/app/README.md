# XO Game - Frontend 🎮

Beautiful, modern Vue.js frontend for the XO (Tic-Tac-Toe) game with stunning UI/UX.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Recommended Browser Setup

- Chromium-based browsers (Chrome, Edge, Brave, etc.):
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd) 
  - [Turn on Custom Object Formatter in Chrome DevTools](http://bit.ly/object-formatters)
- Firefox:
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [Turn on Custom Object Formatter in Firefox DevTools](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript language service aware of `.vue` types.

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

The app will be available at `http://localhost:5173`

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

## 🎨 Features

- **Stunning UI/UX**: Gradient backgrounds, smooth animations, and modern design
- **Responsive Design**: Works perfectly on desktop, tablet, and mobile
- **Complete Navigation Flow**:
  - Home page with game mode selection
  - Online play menu (Create/Join room)
  - Bot mode placeholder
  - Beautiful game board component

## 📁 Project Structure

```
src/
├── components/
│   └── GameBoard.vue       # Reusable game board component
├── views/
│   ├── HomeView.vue        # Landing page with mode selection
│   ├── OnlineMenuView.vue  # Create/Join room menu
│   ├── BotView.vue         # Bot mode placeholder
│   ├── CreateRoomView.vue  # Create room page
│   └── JoinRoomView.vue    # Join room page
├── router/
│   └── index.ts            # Vue Router configuration
├── App.vue                 # Main app component
└── main.ts                 # App entry point
```

## 🎮 Navigation Flow

```
Home (/)
├── Play Online (/online)
│   ├── Create Room (/create-room)
│   └── Join Room (/join-room)
└── Play Against Bot (/bot)
```

## 🔧 GameBoard Component

The `GameBoard` component is a fully styled, ready-to-use game board with:
- 3x3 grid layout with click handlers
- Current turn indicator
- Winner/draw display
- Reset functionality
- Responsive design (120px → 90px → 70px cells)

**The click handler in `GameBoard.vue` is ready for your WebSocket logic:**
```typescript
const handleCellClick = (index: number) => {
  // TODO: Add your WebSocket logic here
  console.log(`Cell ${index} clicked`)
}
```

## 🎨 Color Scheme

- **X Player**: Red (#ff6b6b)
- **O Player**: Teal (#4ecdc4)
- **Primary**: Purple-Pink gradient (#667eea → #764ba2)
- **Create Room**: Green-Cyan gradient (#43e97b → #38f9d7)
- **Join Room**: Pink-Yellow gradient (#fa709a → #fee140)
- **Bot Mode**: Pink-Red gradient (#f093fb → #f5576c)

## 🎯 Next Steps

The UI framework is complete! You can now add:
1. **WebSocket Connection** in `GameBoard.vue`
2. **Room Logic** in Create/Join room views
3. **Game State Management** with Pinia
4. **Bot Implementation** in `BotView.vue`

Enjoy building your game! ✨
