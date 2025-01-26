package com.pickleball.pickleball.model;

import ch.qos.logback.core.joran.sanity.Pair;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class Court {

    private final int id;
    private Pair<Player, Player> team1;
    private Pair<Player, Player> team2;
    private List<Player> reserves;
    private boolean team1Win;
    private boolean team2Win;
}
