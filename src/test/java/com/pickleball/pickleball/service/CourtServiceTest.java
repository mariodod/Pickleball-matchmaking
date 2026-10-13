package com.pickleball.pickleball.service;

import com.pickleball.pickleball.model.Court;
import com.pickleball.pickleball.model.Pair;
import com.pickleball.pickleball.repository.CourtRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourtServiceTest {

    private CourtRepository courtRepository;
    private CourtService courtService;

    @BeforeEach
    void setUp() {
        courtRepository = new CourtRepository();
        courtService = new CourtService(courtRepository);
    }

    @Test
    void updateWinnerSetsExclusiveResult() {
        addCourt("A1", "A2", "B1", "B2");

        courtService.updateWinner(0, "team1");

        Court court = courtRepository.getAll().getFirst();
        assertTrue(court.isTeam1Win());
        assertFalse(court.isTeam2Win());
        assertEquals("A1", court.getWinners().player1());
    }

    @Test
    void nextMatchesRequiresAWinnerOnEveryCourt() {
        addCourt("A1", "A2", "B1", "B2");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, courtService::nextMatches);
        assertEquals("Every court needs a winner before rotating", ex.getReason());
    }

    @Test
    void nextMatchesKeepsReservesSittingOnASingleCourt() {
        addCourt("A1", "A2", "B1", "B2", "R1");
        courtService.updateWinner(0, "team1");

        courtService.nextMatches();

        Court court = courtRepository.getAll().getFirst();
        assertEquals(new Pair("A1", "B1"), court.getTeam1());
        assertEquals(new Pair("A2", "B2"), court.getTeam2());
        assertEquals(List.of("R1"), List.copyOf(court.getReserves()));
        assertFalse(court.hasResult());
    }

    @Test
    void nextMatchesMovesWinnersUpAndLosersDown() {
        addCourt("A1", "A2", "B1", "B2", "R0");
        addCourt("C1", "C2", "D1", "D2", "R1");
        addCourt("E1", "E2", "F1", "F2", "R2");
        courtService.updateWinner(0, "team1");
        courtService.updateWinner(1, "team2");
        courtService.updateWinner(2, "team1");

        courtService.nextMatches();

        List<Court> courts = courtRepository.getAll();
        assertEquals(new Pair("A1", "D1"), courts.get(0).getTeam1());
        assertEquals(new Pair("A2", "D2"), courts.get(0).getTeam2());
        assertEquals(List.of("R0"), List.copyOf(courts.get(0).getReserves()));

        assertEquals(new Pair("E1", "B1"), courts.get(1).getTeam1());
        assertEquals(new Pair("E2", "B2"), courts.get(1).getTeam2());

        assertEquals(new Pair("F1", "C1"), courts.get(2).getTeam1());
        assertEquals(new Pair("F2", "C2"), courts.get(2).getTeam2());
    }

    private void addCourt(String t1p1, String t1p2, String t2p1, String t2p2, String... reserves) {
        courtService.addCourt(new Pair(t1p1, t1p2), new Pair(t2p1, t2p2), new LinkedList<>(List.of(reserves)));
    }
}
