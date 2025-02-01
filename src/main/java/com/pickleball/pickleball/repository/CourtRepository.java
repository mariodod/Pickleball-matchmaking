package com.pickleball.pickleball.repository;

import com.pickleball.pickleball.model.Court;
import com.pickleball.pickleball.model.Pair;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@Repository
public class CourtRepository {

    private final List<Court> courts = new ArrayList<>();

    public List<Court> getAll() {
        return courts;
    }

    public void add(Pair team1, Pair team2, Queue<String> reserves) {
        courts.addLast(new Court(courts.size(), team1, team2, reserves));
    }

    public void delete(){
        courts.removeLast();
    }

}