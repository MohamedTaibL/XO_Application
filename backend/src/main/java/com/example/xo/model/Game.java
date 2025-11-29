// ...existing code...
package com.example.xo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.*;

/**
 * Model skeleton for a Game with minimal local board logic.
 * Server will call addPlayer(...) and applyMove(...) and use the returned result to broadcast.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    private String id;
    private Player playerX;
    private Player playerO;
    private List<Move> moves = new ArrayList<>();
    private GameState state;

    // internal board representation (0..8)
    private final char[] board = new char[9];

    // id of player whose turn it is
    private String currentTurnPlayerId;

    // convenience map for lookup
    private final Map<String, Player> players = new HashMap<>();

    public Game(String id) {
        this.id = id;
        Arrays.fill(board, ' ');
    }

    /**
     * Add a player to the game. Assigns mark "X" or "O" and sets first player's turn.
     */
    public synchronized void addPlayer(Player p) {
        if (p == null || p.getId() == null) throw new IllegalArgumentException("player required");
        if (players.containsKey(p.getId())) return; // already present

        // assign mark
        if (playerX == null) {
            p.setMark("X");
            playerX = p;
        } else if (playerO == null) {
            p.setMark("O");
            playerO = p;
        } else {
            // allow spectator but don't assign a mark
            p.setMark(null);
        }

        players.put(p.getId(), p);

        if (currentTurnPlayerId == null && p.getMark() != null) {
            currentTurnPlayerId = p.getId();
        }

        // update game state: waiting until two players have marks, then in progress
        if (playerX != null && playerO != null) {
            this.state = GameState.IN_PROGRESS;
        } else {
            this.state = GameState.WAITING;
        }
    }

    /**
     * Remove a player from the game. Adjust marks, current turn and state accordingly.
     */
    public synchronized void removePlayer(String playerId) {
        if (playerId == null) return;
        Player removed = players.remove(playerId);
        if (removed == null) return;

        // clear references if they were the X or O player
        if (playerX != null && playerX.getId().equals(playerId)) {
            playerX = null;
        }
        if (playerO != null && playerO.getId().equals(playerId)) {
            playerO = null;
        }

        // if the removed player was the current turn, pick the other marked player if present
        if (currentTurnPlayerId != null && currentTurnPlayerId.equals(playerId)) {
            if (playerX != null && playerX.getMark() != null) {
                currentTurnPlayerId = playerX.getId();
            } else if (playerO != null && playerO.getMark() != null) {
                currentTurnPlayerId = playerO.getId();
            } else {
                currentTurnPlayerId = null;
            }
        }

        // update game state
        if (playerX != null && playerO != null) {
            this.state = GameState.IN_PROGRESS;
        } else if (playerX == null && playerO == null) {
            this.state = GameState.WAITING;
        } else {
            this.state = GameState.WAITING;
        }
    }

    /**
     * Apply move to the board. Validates bounds, turn and occupancy.
     * Returns a MoveResult describing winner/draw/nextTurn.
     * Throws IllegalArgumentException on invalid move.
     */
    public synchronized MoveResult applyMove(Move m) {
        if (m == null) throw new IllegalArgumentException("move required");
        Player p = players.get(m.getPlayerId());
        if (p == null || p.getMark() == null) {
            throw new IllegalArgumentException("player not part of game or has no mark");
        }
        if (currentTurnPlayerId == null || !currentTurnPlayerId.equals(m.getPlayerId())) {
            throw new IllegalArgumentException("not player's turn");
        }
        if (m.getX() < 0 || m.getX() > 2 || m.getY() < 0 || m.getY() > 2) {
            throw new IllegalArgumentException("coordinates out of bounds");
        }
        int idx = m.getX() + m.getY() * 3;
        if (board[idx] != ' ') {
            throw new IllegalArgumentException("cell occupied");
        }

        // apply
        board[idx] = p.getMark().charAt(0);
        moves.add(m);

        // check winner / draw
    Character winner = checkWinner();
    boolean draw = winner == null && isBoardFull();

        String nextTurn = null;
        if (winner == null && !draw) {
            // toggle to other marked player if exists
            if (playerX != null && playerO != null) {
                nextTurn = playerX.getId().equals(m.getPlayerId()) ? playerO.getId() : playerX.getId();
            } else {
                nextTurn = m.getPlayerId(); // no other player yet
            }
            currentTurnPlayerId = nextTurn;
            // ensure state reflects ongoing play
            this.state = GameState.IN_PROGRESS;
        } else {
            // game finished
            currentTurnPlayerId = null;
            if (winner != null) {
                // set specific win state
                if (winner == 'X') this.state = GameState.X_WON;
                else if (winner == 'O') this.state = GameState.O_WON;
                else this.state = GameState.FINISHED;
            } else if (draw) {
                this.state = GameState.DRAW;
            } else {
                this.state = GameState.FINISHED;
            }
        }

        return new MoveResult(winner, draw, nextTurn);
    }

    /**
     * Reset the board and moves for a new match while keeping players assigned.
     */
    public synchronized void resetForNewMatch() {
        this.moves.clear();
        Arrays.fill(board, ' ');
        // clear any transient match-specific state
        // currentTurnPlayerId will be set by the server when the match starts
        this.currentTurnPlayerId = null;
        this.state = GameState.WAITING;
    }

    public synchronized String[] getBoardAsArray() {
        String[] arr = new String[9];
        for (int i = 0; i < 9; i++) arr[i] = String.valueOf(board[i]);
        return arr;
    }

    private Character checkWinner() {
        int[][] lines = {
            {0,1,2},{3,4,5},{6,7,8},
            {0,3,6},{1,4,7},{2,5,8},
            {0,4,8},{2,4,6}
        };
        for (int[] L : lines) {
            char a = board[L[0]], b = board[L[1]], c = board[L[2]];
            if (a != ' ' && a == b && a == c) return a;
        }
        return null;
    }

    private boolean isBoardFull() {
        for (char c : board) if (c == ' ') return false;
        return true;
    }

    public static class MoveResult {
        public final Character winner; // 'X' or 'O' or null
        public final boolean draw;
        public final String nextPlayerId;

        public MoveResult(Character winner, boolean draw, String nextPlayerId) {
            this.winner = winner;
            this.draw = draw;
            this.nextPlayerId = nextPlayerId;
        }
    }
}