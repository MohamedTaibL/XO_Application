package com.example.xo.websocket;

import com.example.xo.model.Game;
import com.example.xo.model.Move;
import com.example.xo.model.Player;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skeleton standalone WebSocket server using org.java-websocket.
 *
 * This class intentionally contains no game logic. Implementations should
 * parse incoming messages, manage game sessions and broadcast updates.
 */


public class StandaloneGameWebSocketServer extends WebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(StandaloneGameWebSocketServer.class);

    // maps
    private final Map<String, Game> idToGame = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String , Set<WebSocket> > games = new ConcurrentHashMap<>();
    public final Map<WebSocket , String> connToGame = new ConcurrentHashMap<>();
    public final Map<WebSocket , String> connToPlayer = new ConcurrentHashMap<>();

    public StandaloneGameWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        sendJson(conn, Map.of("type", "welcome", "message", "connected"));
        log.debug("WebSocket opened: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String gameId = connToGame.remove(conn);
    // capture playerid before removing it so broadcasts include it
        String playerId = connToPlayer.get(conn);
    // remove the mapping
        connToPlayer.remove(conn);

        if (gameId != null){
            Set<WebSocket> peers = games.get(gameId);
            if (peers != null){
                peers.remove(conn);

                Game game = idToGame.get(gameId);
                if (game != null) {
                    // keep player in model for reconnection; notify peers
                    broadcastToGame(gameId, Map.of(
                        "type", "player_disconnected",
                        "gameId", gameId,
                        "playerId", playerId
                    ), null);
                    log.info("Player {} disconnected from game {} (can reconnect)", playerId, gameId);
                }

                log.debug("After close, game {} has {} peers", gameId, peers.size());
                // cleanup game only if all peers disconnected and model has no players
                if (peers.isEmpty() && game != null && game.getPlayers().isEmpty()){
                    games.remove(gameId);
                    idToGame.remove(gameId);
                    log.info("Removed empty game {}", gameId);
                }
            }
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {

        try {
            JsonNode node = mapper.readTree(message);
            String type = node.path("type").asText("");

            log.debug("onMessage from {}: type={}", conn.getRemoteSocketAddress(), type);
            switch (type){
                
                case "create": {
                    String playerId = node.path("playerId").asText();
                    if (playerId.isBlank()){
                        sendJson(conn, Map.of("type", "error", "message", "missing playerId"));
                        return;
                    }

                    // check if already in a game
                    String existingGameId = connToGame.get(conn);
                    if (existingGameId != null && idToGame.containsKey(existingGameId)) {
                        // connection already in a game, return error
                        sendJson(conn, Map.of("type", "error", "message", "already_in_game", "gameId", existingGameId));
                        log.warn("Create attempt by {} who is already in game {}", playerId, existingGameId);
                        return;
                    }

                    // generate unique 6-char id
                    String gameId = generateUniqueGameId();

                    // create game and register creator
                    Game game = new Game(gameId);
                    idToGame.put(gameId, game);

                    games.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(conn);
                    connToGame.put(conn, gameId);
                    connToPlayer.put(conn, playerId);

                    Player p = new Player(playerId, node.path("name").asText(null), null, null);
                    game.addPlayer(p);

                    sendJson(conn, buildMessageWithState("created", game, Map.of("playerId", playerId, "gameId", gameId)));
                    log.info("Game {} created by player {}", gameId, playerId);
                    break;
                }
                case "sync": {
                    String gameId = node.path("gameId").asText();
                    String playerId = node.path("playerId").asText();
                    
                    if (gameId.isBlank() || playerId.isBlank()) {
                        sendJson(conn, Map.of("type", "error", "message", "missing"));
                        return;
                    }

                    Game game = idToGame.get(gameId);
                    if (game == null) {
                        sendJson(conn, Map.of("type", "error", "message", "unknown_game", "gameId", gameId));
                        log.warn("Sync request for unknown game {} by {}", gameId, playerId);
                        return;
                    }

                    // send current game state to the requesting connection
                    sendJson(conn, buildMessageWithState("synced", game, Map.of("playerId", playerId)));
                    log.info("Synced game state for {} to player {}", gameId, playerId);
                    break;
                }
                case "reconnect": {
                    String gameId = node.path("gameId").asText();
                    String playerId = node.path("playerId").asText();
                    
                    if (gameId.isBlank() || playerId.isBlank()) {
                        sendJson(conn, Map.of("type", "error", "message", "missing"));
                        return;
                    }

                    Game game = idToGame.get(gameId);
                    if (game == null) {
                        sendJson(conn, Map.of("type", "error", "message", "unknown_game", "gameId", gameId));
                        log.warn("Reconnect request for unknown game {} by {}", gameId, playerId);
                        return;
                    }

                    // check if this player was part of the game
                    Player existingPlayer = game.getPlayers().get(playerId);
                    if (existingPlayer == null) {
                        sendJson(conn, Map.of("type", "error", "message", "not_in_game", "gameId", gameId));
                        log.warn("Reconnect request for game {} by unknown player {}", gameId, playerId);
                        return;
                    }

                    // remove stale connections for this player
                    WebSocket oldConn = null;
                    for (Map.Entry<WebSocket, String> entry : connToPlayer.entrySet()) {
                        if (entry.getValue().equals(playerId) && entry.getKey() != conn) {
                            oldConn = entry.getKey();
                            break;
                        }
                    }
                    if (oldConn != null) {
                        connToGame.remove(oldConn);
                        connToPlayer.remove(oldConn);
                        Set<WebSocket> peers = games.get(gameId);
                        if (peers != null) peers.remove(oldConn);
                        log.info("Removed stale connection for player {} during reconnect", playerId);
                    }

                    // add new connection to the game
                    games.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(conn);
                    connToGame.put(conn, gameId);
                    connToPlayer.put(conn, playerId);

                    // first player is creator
                    String role = "player";
                    int idx = 0;
                    for (String pid : game.getPlayers().keySet()) {
                        if (pid.equals(playerId) && idx == 0) {
                            role = "creator";
                            break;
                        }
                        idx++;
                    }

                    // send current game state with reconnect confirmation
                    Map<String, Object> extras = new java.util.HashMap<>();
                    extras.put("playerId", playerId);
                    extras.put("role", role);
                    extras.put("reconnected", true);
                    sendJson(conn, buildMessageWithState("reconnected", game, extras));
                    
                    log.info("Player {} reconnected to game {} as {}", playerId, gameId, role);
                    break;
                }
                case "join":{
                    String gameId = node.path("gameId").asText();
                    String playerId = node.path("playerId").asText();
                    if (gameId.isBlank() || playerId.isBlank()){
                        sendJson(conn , Map.of("type" , "error" , "message" , "missing playerId or gameId"));
                        return ;
                    }

                    // check if already in a game
                    String existingGameId = connToGame.get(conn);
                    if (existingGameId != null) {
                        if (existingGameId.equals(gameId)) {
                            // trying to join the same game - treat as reconnection/sync
                            Game existingGame = idToGame.get(gameId);
                            if (existingGame != null) {
                                sendJson(conn, buildMessageWithState("joined", existingGame, Map.of("playerId", playerId)));
                                log.info("Player {} re-joined/synced to game {}", playerId, gameId);
                                return;
                            }
                        } else {
                            // trying to join a different game - error
                            sendJson(conn, Map.of("type", "error", "message", "already_in_game", "gameId", existingGameId));
                            log.warn("Player {} tried to join game {} but already in game {}", playerId, gameId, existingGameId);
                            return;
                        }
                    }

                    // validate game exists
                    Game game = idToGame.get(gameId);
                    if (game == null) {
                        sendJson(conn, Map.of("type", "error", "message", "unknown_game", "gameId", gameId));
                        log.warn("Join attempt for unknown game {} by {}", gameId, playerId);
                        return;
                    }

                    // check capacity
                    synchronized (game) {
                        if (game.getPlayers().size() >= 2) {
                            sendJson(conn, Map.of("type", "error", "message", "game_full", "gameId", gameId));
                            log.warn("Player {} tried to join full game {}", playerId, gameId);
                            return;
                        }

                        // add connection
                        games.computeIfAbsent(gameId , k -> ConcurrentHashMap.newKeySet()).add(conn);
                        connToGame.put(conn , gameId);
                        connToPlayer.put(conn , playerId);

                        // Register player.
                        Player p = new Player(playerId, node.path("name").asText(null), null, null);
                        game.addPlayer(p);

                        sendJson(conn, buildMessageWithState("joined", game, Map.of("playerId", playerId)));

                        log.info("Player {} joined game {} — broadcasting to peers (excluding joiner)", playerId, gameId);
                        broadcastToGame(gameId, buildMessageWithState("player_joined", game, Map.of("playerId", playerId, "name", node.path("name").asText(null))), conn);
                    }
                    break;

                }

                case "move":{
                    String gameId = node.path("gameId").asText(connToGame.getOrDefault(conn, ""));
                    String playerId = node.path("playerId").asText(connToPlayer.getOrDefault(conn, ""));
                    int x = node.path("x").asInt(-1);
                    int y = node.path("y").asInt(-1);

                    if (gameId.isBlank() || playerId.isBlank()) {
                        sendJson(conn, Map.of("type", "error", "message", "not joined to a game"));
                        return;
                    }

                    Game game = idToGame.get(gameId);
                    if (game == null) {
                        sendJson(conn, Map.of("type", "error", "message", "unknown game"));
                        return;
                    }

                    Move m = new Move(x, y, playerId, System.currentTimeMillis());
                    try {
                        Game.MoveResult result = game.applyMove(m);

                        // build extras for move
                        Map<String,Object> moveExtras = new java.util.HashMap<>();
                        moveExtras.put("playerId", playerId);
                        moveExtras.put("x", x);
                        moveExtras.put("y", y);
                        moveExtras.put("nextTurn", result.nextPlayerId);

                        // Broadcast move.
                        broadcastToGame(gameId, buildMessageWithState("move", game, moveExtras), null);

                        if (result.winner != null || result.draw) {
                            Map<String,Object> overExtras = new java.util.HashMap<>();
                            overExtras.put("winner", result.winner != null ? String.valueOf(result.winner) : (result.draw ? "DRAW" : null));
                            broadcastToGame(gameId, buildMessageWithState("game_over", game, overExtras), null);
                            // Keep model for restart.
                        }
                    } catch (IllegalArgumentException ex) {
                        sendJson(conn, Map.of("type", "error", "message", ex.getMessage()));
                    }
                    break;
                }
                case "leave": {
                    String gameId = node.path("gameId").asText();
                    String playerId = node.path("playerId").asText();
                    if (gameId.isBlank() || playerId.isBlank()) {
                        sendJson(conn, Map.of("type", "error", "message", "missing"));
                        return;
                    }

                    Game game = idToGame.get(gameId);
                    if (game == null) {
                        sendJson(conn, Map.of("type", "error", "message", "unknown game", "gameId", gameId));
                        log.warn("Leave attempt for unknown game {} by {}", gameId, playerId);
                        return;
                    }

                    // Remove from peers.
                    Set<WebSocket> peers = games.get(gameId);
                    if (peers != null) {
                        peers.remove(conn);
                    }

                    // Remove mappings.
                    connToGame.remove(conn);
                    connToPlayer.remove(conn);

                    // Remove player and broadcast.
                    game.removePlayer(playerId);
                    broadcastToGame(gameId, buildMessageWithState("player_left", game, Map.of("playerId", playerId)), null);

                    log.info("Player {} left game {} via leave message", playerId, gameId);

                    // Cleanup empty game.
                    if (peers == null || peers.isEmpty()){
                        games.remove(gameId);
                        idToGame.remove(gameId);
                        log.info("Removed empty game {} after leave", gameId);
                    }

                    // Ack leave.
                    sendJson(conn, Map.of("type", "left", "gameId", gameId, "playerId", playerId));
                    break;
                }
                case "close": {
                    String gameId = node.path("gameId").asText();
                    String playerId = node.path("playerId").asText();
                    if (gameId.isBlank() || playerId.isBlank()) {
                        sendJson(conn, Map.of("type", "error", "message", "missing"));
                        return;
                    }

                    Game game = idToGame.get(gameId);
                    if (game == null) {
                        sendJson(conn, Map.of("type", "error", "message", "unknown game", "gameId", gameId));
                        log.warn("Close attempt for unknown game {} by {}", gameId, playerId);
                        return;
                    }

                    // Verify owner.
                    String ownerPlayer = connToPlayer.get(conn);
                    if (ownerPlayer == null || !ownerPlayer.equals(playerId)) {
                        sendJson(conn, Map.of("type", "error", "message", "not authorized"));
                        log.warn("Unauthorized close attempt for game {} by {} (owner {})", gameId, playerId, ownerPlayer);
                        return;
                    }

                    // Broadcast close.
                    broadcastToGame(gameId, Map.of("type", "room_closed", "gameId", gameId), null);

                    // Cleanup and kick peers.
                    Set<WebSocket> peers = games.remove(gameId);
                    idToGame.remove(gameId);

                    if (peers != null) {
                        for (WebSocket peer : peers) {
                            try {
                                // Send close frame.
                                if (peer != conn && peer.isOpen()) {
                                    try { peer.send(mapper.writeValueAsString(Map.of("type", "room_closed", "gameId", gameId))) ; } catch (Exception ignored) {}
                                    try { peer.close(1000, "room closed by owner") ; } catch (Exception ignored) {}
                                }
                            } finally {
                                // Remove peer mappings.
                                connToGame.remove(peer);
                                connToPlayer.remove(peer);
                            }
                        }
                    }

                    log.info("Game {} closed by {} and removed from server", gameId, playerId);

                    // Ack close.
                    sendJson(conn, Map.of("type", "closed", "gameId", gameId, "playerId", playerId));
                    break;
                }
                case "start": {
                    String gameId = node.path("gameId").asText();
                    String playerId = node.path("playerId").asText();
                    boolean creatorStarts = node.path("creatorStarts").asBoolean(true);

                    if (gameId.isBlank() || playerId.isBlank()) {
                        sendJson(conn, Map.of("type", "error", "message", "missing"));
                        return;
                    }

                    Game game = idToGame.get(gameId);
                    if (game == null) {
                        sendJson(conn, Map.of("type", "error", "message", "unknown game", "gameId", gameId));
                        log.warn("Start attempt for unknown game {} by {}", gameId, playerId);
                        return;
                    }

                    // Verify owner.
                    String ownerPlayer = connToPlayer.get(conn);
                    if (ownerPlayer == null || !ownerPlayer.equals(playerId)) {
                        sendJson(conn, Map.of("type", "error", "message", "not authorized"));
                        log.warn("Unauthorized start attempt for game {} by {} (owner {})", gameId, playerId, ownerPlayer);
                        return;
                    }

                    // Determine start player.
                    String startPlayerId = playerId;
                    if (!creatorStarts) {
                        for (Map.Entry<String, Player> e : game.getPlayers().entrySet()) {
                            if (!e.getKey().equals(playerId) && e.getValue().getMark() != null) {
                                startPlayerId = e.getKey();
                                break;
                            }
                        }
                    }

                    // Reset game.
                    game.resetForNewMatch();
                    game.setCurrentTurnPlayerId(startPlayerId);
                    game.setState(com.example.xo.model.GameState.IN_PROGRESS);

                    // Broadcast start.
                    broadcastToGame(gameId, buildMessageWithState("game_started", game, Map.of("startedBy", playerId, "startPlayerId", startPlayerId)), null);

                    log.info("Game {} started by {} — startPlayer={}", gameId, playerId, startPlayerId);
                    break;
                }
            }
        } catch (Exception e) {
            sendJson(conn, Map.of("type", "error", "message", "invalid json"));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        if (conn != null && conn.isOpen()) {
            sendJson(conn, Map.of("type", "error", "message", "server error"));
        }
    }

    @Override
    public void onStart() {
        // TODO: server startup tasks
    }


    private void sendJson(WebSocket conn , Object obj){
        try{
            String json = mapper.writeValueAsString(obj);
            conn.send(json);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    
    private Map<String,Object> buildMessageWithState(String type, Game game, Map<String,Object> extras){
        Map<String,Object> m = new java.util.HashMap<>();
        m.put("type", type);
        m.put("gameId", game.getId());
        m.put("state", game.getState() != null ? game.getState().name() : null);
        m.put("board", game.getBoardAsArray());
        m.put("currentTurn", game.getCurrentTurnPlayerId());

        Map<String,Object> players = new java.util.HashMap<>();
        for (Map.Entry<String, Player> e : game.getPlayers().entrySet()){
            Player p = e.getValue();
            Map<String,Object> pd = new java.util.HashMap<>();
            pd.put("id", p.getId());
            pd.put("name", p.getName());
            pd.put("mark", p.getMark());
            players.put(e.getKey(), pd);
        }
        m.put("players", players);

        if (extras != null) m.putAll(extras);
        return m;
    }
    private void broadcastToGame(String gameId , Object obj , WebSocket exclude){
        Set<WebSocket> peers = games.get(gameId);
        if (peers == null || peers.isEmpty()) return;
        String json;
        try {
            json = mapper.writeValueAsString(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        int sent = 0;
        for (WebSocket peer : peers) {
            if (peer != exclude && peer.isOpen()) {
                try {
                    peer.send(json);
                    sent++;
                } catch (Exception ignored) {
                }
            }
        }
        log.info("broadcastToGame gameId={} -> peersSize={} sent={}", gameId, peers.size(), sent);
    }

    // Generate unique 6-char ID.
    private String generateUniqueGameId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int maxAttempts = 10;
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                int index = (int) (Math.random() * chars.length());
                sb.append(chars.charAt(index));
            }
            String gameId = sb.toString();
            
            // Check if this ID is already in use
            if (!idToGame.containsKey(gameId)) {
                return gameId;
            }
            log.warn("Game ID collision detected: {}, retrying... (attempt {}/{})", gameId, attempt + 1, maxAttempts);
        }
        
        // Fallback: append timestamp if all random attempts fail
        String fallbackId = "G" + System.currentTimeMillis() % 100000;
        log.error("Failed to generate unique game ID after {} attempts, using fallback: {}", maxAttempts, fallbackId);
        return fallbackId;
    }
}
