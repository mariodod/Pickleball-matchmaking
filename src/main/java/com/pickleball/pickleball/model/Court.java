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

    public Pair getWinners() {
        return team1Win ? team1 : team2;
    }

    public Pair getLosers() {
        return !team1Win ? team1 : team2;
    }

    public void resetResult() {
        team1Win = false;
        team2Win = false;
    }
}