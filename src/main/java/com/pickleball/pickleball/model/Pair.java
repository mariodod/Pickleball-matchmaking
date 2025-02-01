package com.pickleball.pickleball.model;

import java.util.Map;

//Only relevant thing here is being player 1 gives you priority in reserves queue
public record Pair(String player1, String player2) {
    public Pair(Map<String, String> team) {
        this(team.get("player1"), team.get("player2"));
    }
}