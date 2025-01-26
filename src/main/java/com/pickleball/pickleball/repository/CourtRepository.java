package com.pickleball.pickleball.repository;

import com.pickleball.pickleball.model.Court;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Repository
public class CourtRepository {

    private final Map<Integer, Court> courts = new HashMap<>();

    public Collection<Court> getAll() {
        return courts.values();
    }

    public Court getOne(int id) {
        return courts.get(id);
    }

    public Court add(Court court) {
        return courts.putIfAbsent(court.getId(), court);
    }

    public Court delete(int id){
        return courts.remove(id);
    }

}