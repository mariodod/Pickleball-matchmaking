package com.pickleball.pickleball.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickleball.pickleball.model.Court;
import com.pickleball.pickleball.model.Pair;
import com.pickleball.pickleball.repository.CourtRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CourtServiceTest {

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private CourtService courtService;

    @BeforeEach
    void setUp() {
        courtService.resetSession();
    }

    @Test
    void courtCanBeSerializedToJsonWithoutWinnerSet() throws Exception {
        Court court = new Court(1L, new Pair("A1", "A2"), new Pair("B1", "B2"), new LinkedList<>());
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(court);
        assertNotNull(json);
        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"player1\":\"A1\""));
    }

    @Test
    void updateWinnerSetsExclusiveResult() {
        addCourt("A1", "A2", "B1", "B2");
        long id = courtRepository.findAllByOrderByIdAsc().getFirst().getId();

        courtService.updateWinner(id, "team1");

        Court court = courtRepository.findById(id).orElseThrow();
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
    void nextMatchesSwapsPartnersOnSingleCourtWithoutReserves() {
        addCourt("A1", "A2", "B1", "B2");
        long id = courtRepository.findAllByOrderByIdAsc().getFirst().getId();
        courtService.updateWinner(id, "team1");

        courtService.nextMatches();

        Court court = courtRepository.findAllByOrderByIdAsc().getFirst();
        assertEquals(new Pair("A1", "B1"), court.getTeam1());
        assertEquals(new Pair("A2", "B2"), court.getTeam2());
        assertTrue(court.getReserves().isEmpty());
        assertFalse(court.hasResult());
    }

    @Test
    void nextMatchesRotatesReservesIntoMatchOnSingleCourt() {
        addCourt("A1", "A2", "B1", "B2", "R1");
        long id = courtRepository.findAllByOrderByIdAsc().getFirst().getId();
        courtService.updateWinner(id, "team1");

        courtService.nextMatches();

        Court court = courtRepository.findAllByOrderByIdAsc().getFirst();
        assertEquals(new Pair("R1", "A1"), court.getTeam1());
        assertEquals(new Pair("B1", "A2"), court.getTeam2());
        assertEquals(List.of("B2"), List.copyOf(court.getReserves()));
        assertFalse(court.hasResult());
    }

    @Test
    void nextMatchesMovesWinnersUpAndLosersDownWithoutReserves() {
        addCourt("A1", "A2", "B1", "B2");
        addCourt("C1", "C2", "D1", "D2");
        addCourt("E1", "E2", "F1", "F2");

        List<Court> initialCourts = courtRepository.findAllByOrderByIdAsc();
        courtService.updateWinner(initialCourts.get(0).getId(), "team1");
        courtService.updateWinner(initialCourts.get(1).getId(), "team2");
        courtService.updateWinner(initialCourts.get(2).getId(), "team1");

        courtService.nextMatches();

        List<Court> courts = courtRepository.findAllByOrderByIdAsc();
        assertEquals(new Pair("A1", "D1"), courts.get(0).getTeam1());
        assertEquals(new Pair("A2", "D2"), courts.get(0).getTeam2());

        assertEquals(new Pair("E1", "B1"), courts.get(1).getTeam1());
        assertEquals(new Pair("E2", "B2"), courts.get(1).getTeam2());

        assertEquals(new Pair("F1", "C1"), courts.get(2).getTeam1());
        assertEquals(new Pair("F2", "C2"), courts.get(2).getTeam2());
    }

    @Test
    void nextMatchesRotatesReservesOnMultiCourts() {
        addCourt("A1", "A2", "B1", "B2", "R0");
        addCourt("C1", "C2", "D1", "D2");
        addCourt("E1", "E2", "F1", "F2");

        List<Court> initialCourts = courtRepository.findAllByOrderByIdAsc();
        courtService.updateWinner(initialCourts.get(0).getId(), "team1");
        courtService.updateWinner(initialCourts.get(1).getId(), "team2");
        courtService.updateWinner(initialCourts.get(2).getId(), "team1");

        courtService.nextMatches();

        List<Court> courts = courtRepository.findAllByOrderByIdAsc();
        assertEquals(new Pair("R0", "A1"), courts.get(0).getTeam1());
        assertEquals(new Pair("D1", "A2"), courts.get(0).getTeam2());
        assertEquals(List.of("D2"), List.copyOf(courts.get(0).getReserves()));
    }

    @Test
    void nextMatchesIncrementsRoundCounter() {
        assertEquals(1, courtService.getCurrentRound());
        addCourt("A1", "A2", "B1", "B2");
        long id = courtRepository.findAllByOrderByIdAsc().getFirst().getId();
        courtService.updateWinner(id, "team1");

        courtService.nextMatches();

        assertEquals(2, courtService.getCurrentRound());
    }

    @Test
    void resetSessionClearsCourtsAndResetsRoundCounter() {
        addCourt("A1", "A2", "B1", "B2");
        long id = courtRepository.findAllByOrderByIdAsc().getFirst().getId();
        courtService.updateWinner(id, "team1");
        courtService.nextMatches();
        assertEquals(2, courtService.getCurrentRound());
        assertFalse(courtRepository.findAllByOrderByIdAsc().isEmpty());

        courtService.resetSession();

        assertEquals(1, courtService.getCurrentRound());
        assertTrue(courtRepository.findAllByOrderByIdAsc().isEmpty());
    }

    private void addCourt(String t1p1, String t1p2, String t2p1, String t2p2, String... reserves) {
        courtService.addCourt(new Pair(t1p1, t1p2), new Pair(t2p1, t2p2), new LinkedList<>(List.of(reserves)));
    }
}
