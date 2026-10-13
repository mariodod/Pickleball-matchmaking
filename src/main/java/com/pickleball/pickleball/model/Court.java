package com.pickleball.pickleball.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Entity
@Table(name = "courts")
@Getter
@Setter
@NoArgsConstructor
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    private String team1Player1;

    @JsonIgnore
    private String team1Player2;

    @JsonIgnore
    private String team2Player1;

    @JsonIgnore
    private String team2Player2;

    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn(name = "reserve_order")
    @CollectionTable(name = "court_reserves", joinColumns = @JoinColumn(name = "court_id"))
    @Column(name = "player_name")
    private List<String> reserves = new ArrayList<>();

    private boolean team1Win;
    private boolean team2Win;

    public Court(Pair team1, Pair team2, Queue<String> reserves) {
        setTeam1(team1);
        setTeam2(team2);
        this.reserves = reserves != null ? new ArrayList<>(reserves) : new ArrayList<>();
    }

    public Court(Long id, Pair team1, Pair team2, Queue<String> reserves) {
        this(team1, team2, reserves);
        this.id = id;
    }

    @JsonProperty("team1")
    public Pair getTeam1() {
        return new Pair(team1Player1, team1Player2);
    }

    public void setTeam1(Pair pair) {
        this.team1Player1 = pair != null ? pair.player1() : null;
        this.team1Player2 = pair != null ? pair.player2() : null;
    }

    @JsonProperty("team2")
    public Pair getTeam2() {
        return new Pair(team2Player1, team2Player2);
    }

    public void setTeam2(Pair pair) {
        this.team2Player1 = pair != null ? pair.player1() : null;
        this.team2Player2 = pair != null ? pair.player2() : null;
    }

    @JsonIgnore
    public Queue<String> getReservesQueue() {
        return new LinkedList<>(reserves);
    }

    public void setReservesFromQueue(Queue<String> queue) {
        this.reserves = queue != null ? new ArrayList<>(queue) : new ArrayList<>();
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

    @JsonIgnore
    public Pair getWinners() {
        if (!hasResult()) {
            return null;
        }
        return team1Win ? getTeam1() : getTeam2();
    }

    @JsonIgnore
    public Pair getLosers() {
        if (!hasResult()) {
            return null;
        }
        return team1Win ? getTeam2() : getTeam1();
    }

    public void resetResult() {
        team1Win = false;
        team2Win = false;
    }
}
