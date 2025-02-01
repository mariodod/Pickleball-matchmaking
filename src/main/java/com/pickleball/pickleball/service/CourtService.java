package com.pickleball.pickleball.service;

import com.pickleball.pickleball.model.Court;
import com.pickleball.pickleball.model.Pair;
import com.pickleball.pickleball.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        courtRepository.delete();
    }
    
    public void nextMatches() {
        var courts = getAllCourts();
        // prepare reserves for next matches
        for (int i = 0; i < courts.size(); i++) {
            Court current = courts.get(i);
            Court prev, next;
            if (i == 0) { // player1() court winners stay
                next = courts.get(i+1);
                current.getReserves().add(current.getWinners().player1());
                current.getReserves().add(next.getWinners().player1());
                current.getReserves().add(current.getWinners().player2());
                current.getReserves().add(next.getWinners().player2());
            } else if (i == courts.size() - 1) { // last court losers stay
                prev = courts.get(i-1);
                current.getReserves().add(current.getLosers().player1());
                current.getReserves().add(prev.getLosers().player1());
                current.getReserves().add(current.getLosers().player2());
                current.getReserves().add(prev.getLosers().player2());
            } else {
                prev = courts.get(i-1);
                next = courts.get(i+1);
                current.getReserves().add(next.getWinners().player1());
                current.getReserves().add(prev.getLosers().player1());
                current.getReserves().add(next.getWinners().player2());
                current.getReserves().add(prev.getLosers().player2());
            }
        }
        // set teams
        for (Court c : courts) {
            var reserves = c.getReserves();
            c.setTeam1(new Pair(reserves.poll(), reserves.poll()));
            c.setTeam2(new Pair(reserves.poll(), reserves.poll()));
            c.resetResult();
        }
    }

}