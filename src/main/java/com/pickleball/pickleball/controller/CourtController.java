package com.pickleball.pickleball.controller;

import com.pickleball.pickleball.model.Court;
import com.pickleball.pickleball.model.Pair;
import com.pickleball.pickleball.service.CourtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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
        Pair team1 = teamFrom(payload.get("team1"));
        Pair team2 = teamFrom(payload.get("team2"));
        Queue<String> reserves = reservesFrom(payload.get("reserves"));
        courtService.addCourt(team1, team2, reserves);
    }

    @PostMapping("/delete")
    public void deleteCourt() {
        courtService.deleteCourt();
    }

    @DeleteMapping("/{id}")
    public void deleteCourtById(@PathVariable long id) {
        courtService.deleteCourtById(id);
    }

    @PutMapping("/{id}/winner")
    public void updateWinner(@PathVariable long id, @RequestBody Map<String, String> request) {
        courtService.updateWinner(id, request == null ? null : request.get("winner"));
    }

    @GetMapping("/round")
    public Map<String, Integer> getRound() {
        return Map.of("round", courtService.getCurrentRound());
    }

    @PostMapping("/reset")
    public void resetSession() {
        courtService.resetSession();
    }

    @PostMapping("/calculate")
    public void calculate() {
        courtService.nextMatches();
    }

    @SuppressWarnings("unchecked")
    private static Pair teamFrom(Object raw) {
        if (!(raw instanceof Map<?, ?> team) || !team.containsKey("player1") || !team.containsKey("player2")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each team needs player1 and player2");
        }
        return new Pair((Map<String, String>) team);
    }

    @SuppressWarnings("unchecked")
    private static Queue<String> reservesFrom(Object raw) {
        if (raw == null) {
            return new LinkedList<>();
        }
        if (!(raw instanceof List<?> names)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reserves must be a list of names");
        }
        return new LinkedList<>((List<String>) names);
    }
}
