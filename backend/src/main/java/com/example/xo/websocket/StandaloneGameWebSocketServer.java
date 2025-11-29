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

    // gameId -> Game Instance (model owns board/moves/turn)
    private final Map<String, Game> idToGame = new ConcurrentHashMap<>();

    //Just an object Mapper
    private final ObjectMapper mapper = new ObjectMapper();

    //gameId -> setOfConnections
    private final Map<String , Set<WebSocket> > games = new ConcurrentHashMap<>();

    // WebSocket connection -> Game
    public final Map<WebSocket , String> connToGame = new ConcurrentHashMap<>();

    //WebSocket connection -> Player
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
        // TODO: handle connection close

        String gameId = connToGame.remove(conn);
        // capture playerId BEFORE removing it from the map so we can include it in broadcasts
        String playerId = connToPlayer.get(conn);
        // now remove the mapping
        connToPlayer.remove(conn);

        if (gameId != null){
            Set<WebSocket> peers = games.get(gameId);
            if (peers != null){
                peers.remove(conn);

                // include game state if available; remove player from model first so state reflects current players
                Game game = idToGame.get(gameId);
                if (game != null) {
                    // remove the player from the game model
                    game.removePlayer(playerId);
                    broadcastToGame(gameId, buildMessageWithState("player_left", game, Map.of("playerId", playerId)), null);
                } else {
                    broadcastToGame(gameId , Map.of(
                        "type" , "player_left",
                        "gameId" , gameId,
                        "playerId" , playerId
                    ) , null);
                }

                log.debug("After close, game {} has {} peers", gameId, peers.size());
                if (peers.isEmpty()){
                    games.remove(gameId);
                    // remove game model as well to free memory and prevent stale joins
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
                    String gameId = node.path("gameId").asText();
                    String playerId = node.path("playerId").asText();
                    if (gameId.isBlank() || playerId.isBlank()){
                        sendJson(conn, Map.of("type", "error", "message", "missing"));
                        return;
                    }

                    // Do not overwrite existing game
                    if (idToGame.containsKey(gameId)){
                        sendJson(conn, Map.of("type", "error", "message", "game_exists", "gameId", gameId));
                        return;
                    }

                    // create game and register creator
                    Game game = new Game(gameId);
                    idToGame.put(gameId, game);

                    games.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(conn);
                    connToGame.put(conn, gameId);
                    connToPlayer.put(conn, playerId);

                    Player p = new Player(playerId, node.path("name").asText(null), null, null);
                    game.addPlayer(p);

                    sendJson(conn, buildMessageWithState("created", game, Map.of("playerId", playerId)));
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
                        sendJson(conn, Map.of("type", "error", "message", "unknown game", "gameId", gameId));
                        log.warn("Sync request for unknown game {} by {}", gameId, playerId);
                        return;
                    }

                    // Send current game state to the requesting connection
                    sendJson(conn, buildMessageWithState("synced", game, Map.of("playerId", playerId)));
                    log.info("Synced game state for {} to player {}", gameId, playerId);
                    break;
                }
                case "join":{
                    String gameId = node.path("gameId").asText();
                    String playerId = node.path("playerId").asText();
                    if (gameId.isBlank() || playerId.isBlank()){
                        sendJson(conn , Map.of("type" , "error" , "message" , "missing"));
                        return ;
                    }

                    // only allow joining existing games
                    Game game = idToGame.get(gameId);
                    if (game == null) {
                        sendJson(conn, Map.of("type", "error", "message", "unknown game", "gameId", gameId));
                        log.warn("Join attempt for unknown game {} by {}", gameId, playerId);
                        return;
                    }

                    games.computeIfAbsent(gameId , k -> ConcurrentHashMap.newKeySet()).add(conn);
                    connToGame.put(conn , gameId);
                    connToPlayer.put(conn , playerId);

                    // register the player
                    Player p = new Player(playerId, node.path("name").asText(null), null, null);
                    game.addPlayer(p);

                    sendJson(conn, buildMessageWithState("joined", game, Map.of("playerId", playerId)));

                    log.info("Player {} joined game {} — broadcasting to peers (excluding joiner)", playerId, gameId);
                    broadcastToGame(gameId, buildMessageWithState("player_joined", game, Map.of("playerId", playerId, "name", node.path("name").asText(null))), conn);
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

                        // broadcast move with full game state
                        broadcastToGame(gameId, buildMessageWithState("move", game, moveExtras), null);

                        if (result.winner != null || result.draw) {
                            Map<String,Object> overExtras = new java.util.HashMap<>();
                            overExtras.put("winner", result.winner != null ? String.valueOf(result.winner) : (result.draw ? "DRAW" : null));
                            broadcastToGame(gameId, buildMessageWithState("game_over", game, overExtras), null);
                            // keep game model so players remain attached and creator may restart a new match
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

                    // remove connection from peers if present
                    Set<WebSocket> peers = games.get(gameId);
                    if (peers != null) {
                        peers.remove(conn);
                    }

                    // remove mappings for this connection
                    connToGame.remove(conn);
                    connToPlayer.remove(conn);

                    // remove player from model and broadcast updated state
                    game.removePlayer(playerId);
                    broadcastToGame(gameId, buildMessageWithState("player_left", game, Map.of("playerId", playerId)), null);

                    log.info("Player {} left game {} via leave message", playerId, gameId);

                    // if no peers remain, cleanup
                    if (peers == null || peers.isEmpty()){
                        games.remove(gameId);
                        idToGame.remove(gameId);
                        log.info("Removed empty game {} after leave", gameId);
                    }

                    // ack the leaver
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

                    // only allow the player who is in this connection to close the room
                    String ownerPlayer = connToPlayer.get(conn);
                    if (ownerPlayer == null || !ownerPlayer.equals(playerId)) {
                        sendJson(conn, Map.of("type", "error", "message", "not authorized"));
                        log.warn("Unauthorized close attempt for game {} by {} (owner {})", gameId, playerId, ownerPlayer);
                        return;
                    }

                    // broadcast room closed to all peers
                    // broadcast room closed to all peers
                    broadcastToGame(gameId, Map.of("type", "room_closed", "gameId", gameId), null);

                    // cleanup mappings for this game and actively disconnect peers (kick them)
                    Set<WebSocket> peers = games.remove(gameId);
                    idToGame.remove(gameId);

                    if (peers != null) {
                        for (WebSocket peer : peers) {
                            try {
                                // send explicit frame to each peer (some may have already received broadcast)
                                if (peer != conn && peer.isOpen()) {
                                    try { peer.send(mapper.writeValueAsString(Map.of("type", "room_closed", "gameId", gameId))) ; } catch (Exception ignored) {}
                                    try { peer.close(1000, "room closed by owner") ; } catch (Exception ignored) {}
                                }
                            } finally {
                                // remove any mappings for peer
                                connToGame.remove(peer);
                                connToPlayer.remove(peer);
                            }
                        }
                    }

                    log.info("Game {} closed by {} and removed from server", gameId, playerId);

                    // ack the closer
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

                    // only allow the creator (connection owner) to start
                    String ownerPlayer = connToPlayer.get(conn);
                    if (ownerPlayer == null || !ownerPlayer.equals(playerId)) {
                        sendJson(conn, Map.of("type", "error", "message", "not authorized"));
                        log.warn("Unauthorized start attempt for game {} by {} (owner {})", gameId, playerId, ownerPlayer);
                        return;
                    }

                    // determine starting player: if creatorStarts true => playerId starts; else pick the other marked player if present
                    String startPlayerId = playerId;
                    if (!creatorStarts) {
                        for (Map.Entry<String, Player> e : game.getPlayers().entrySet()) {
                            if (!e.getKey().equals(playerId) && e.getValue().getMark() != null) {
                                startPlayerId = e.getKey();
                                break;
                            }
                        }
                    }

                    // reset board/moves for the new match and set starting player
                    game.resetForNewMatch();
                    game.setCurrentTurnPlayerId(startPlayerId);
                    game.setState(com.example.xo.model.GameState.IN_PROGRESS);

                    // broadcast game_started with authoritative state
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
}
