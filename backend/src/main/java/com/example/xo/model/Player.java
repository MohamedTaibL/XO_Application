package com.example.xo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * player model for online play.
 * bot gameplay is handled entirely client-side, so no bot flag needed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private String id;
    private String name;
    private String mark;
    private String sessionId; // optional: for session validation

    public Player(String id, String name, String mark) {
        this.id = id;
        this.name = name;
        this.mark = mark;
    }

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getMark(){
        return mark;
    }
}
