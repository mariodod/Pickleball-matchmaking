package com.pickleball.pickleball.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Queue;

@Getter
@Setter
public class Court {

    private final int id;
    private Pair team1;
    private Pair team2;
    private Queue<String> reserves;
    private boolean team1Win;
    private boolean team2Win;

    public Court(int id, Pair team1, Pair team2, Queue<String> reserves) {
        this.id = id;
        this.team1 = team1;
        this.team2 = team2;
        this.reserves = reserves;
    }

    public boolean hasResult() {
        return team1Win ^ team2Win;
    }

    public void setWinner(String winner) {
        if (winner == null || winner.isBlank()) {
            resetResult();
            return;
        }
        if ("team1".equalsIgnoreCase(winner)) {
            team1Win = true;
            team2Win = false;
            return;
        }
        if ("team2".equalsIgnoreCase(winner)) {
            team1Win = false;
            team2Win = true;
            return;
        }
        throw new IllegalArgumentException("winner must be team1 or team2");
    }

    public Pair getWinners() {
        if (!hasResult()) {
            throw new IllegalStateException("No winner set for court " + id);
        }
        return team1Win ? team1 : team2;
    }

    public Pair getLosers() {
        if (!hasResult()) {
            throw new IllegalStateException("No winner set for court " + id);
        }
        return team1Win ? team2 : team1;
    }

    public void resetResult() {
        team1Win = false;
        team2Win = false;
    }
}
