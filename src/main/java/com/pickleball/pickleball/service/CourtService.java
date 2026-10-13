package com.pickleball.pickleball.service;

import com.pickleball.pickleball.model.Court;
import com.pickleball.pickleball.model.Pair;
import com.pickleball.pickleball.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;

    public List<Court> getAllCourts() {
        return courtRepository.getAll();
    }

    public void addCourt(Pair team1, Pair team2, Queue<String> reserves) {
        courtRepository.add(team1, team2, reserves);
    }

    public void deleteCourt() {
        try {
            courtRepository.delete();
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    public void updateWinner(int id, String winner) {
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Court not found"));
        try {
            court.setWinner(winner);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    public void nextMatches() {
        List<Court> courts = getAllCourts();
        if (courts.isEmpty()) {
            return;
        }
        if (courts.stream().anyMatch(court -> !court.hasResult())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Every court needs a winner before rotating");
        }

        List<Pair> winners = new ArrayList<>(courts.size());
        List<Pair> losers = new ArrayList<>(courts.size());
        for (Court court : courts) {
            winners.add(court.getWinners());
            losers.add(court.getLosers());
        }

        if (courts.size() == 1) {
            assignIncoming(courts.getFirst(), winners.getFirst(), losers.getFirst());
            return;
        }

        for (int i = 0; i < courts.size(); i++) {
            if (i == 0) {
                assignIncoming(courts.get(i), winners.get(i), winners.get(i + 1));
            } else if (i == courts.size() - 1) {
                assignIncoming(courts.get(i), losers.get(i), losers.get(i - 1));
            } else {
                assignIncoming(courts.get(i), winners.get(i + 1), losers.get(i - 1));
            }
        }
    }

    private static void assignIncoming(Court court, Pair stay, Pair incoming) {
        court.setTeam1(new Pair(stay.player1(), incoming.player1()));
        court.setTeam2(new Pair(stay.player2(), incoming.player2()));
        court.resetResult();
    }
}
