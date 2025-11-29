package com.example.xo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a single move on the board.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Move {
    private int x; // 0..2
    private int y; // 0..2
    private String playerId;
    private long timestamp;
    

}
