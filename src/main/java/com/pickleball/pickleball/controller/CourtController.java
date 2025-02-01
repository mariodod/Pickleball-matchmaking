package com.pickleball.pickleball.controller;

import com.pickleball.pickleball.model.Court;
import com.pickleball.pickleball.model.Pair;
import com.pickleball.pickleball.service.CourtService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/courts")
@RequiredArgsConstructor
public class CourtController {

    private final CourtService courtService;

    @GetMapping
    public Collection<Court> getAllCourts() {
        return courtService.getAllCourts();
    }

    @PostMapping("/add")
    public void addCourt(@RequestBody Map<String, Object> payload) {
        Pair team1 = new Pair((Map<String, String>) payload.get("team1"));
        Pair team2 = new Pair((Map<String, String>) payload.get("team2"));
        Queue<String> reserves = new LinkedList<>((List<String>) payload.get("reserves"));
        courtService.addCourt(team1, team2, reserves);
    }

    @PostMapping("/delete")
    public void deleteCourt() {
        courtService.deleteCourt();
    }

    @PutMapping("/{id}/winner")
    public void updateWinner(@PathVariable int id, @RequestBody Map<String, String> request) {
        String winner = request.get("winner");
        //courtService.updateWinner(id, winner);
    }

    @PostMapping("/calculate")
    public void calculate() {
        courtService.nextMatches();
    }

}