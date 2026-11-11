package com.pickleball.pickleball.service;

import com.pickleball.pickleball.model.Court;
import com.pickleball.pickleball.model.Pair;
import com.pickleball.pickleball.model.SessionState;
import com.pickleball.pickleball.repository.CourtRepository;
import com.pickleball.pickleball.repository.SessionStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Service
@RequiredArgsConstructor
public class CourtService {

    private static final long SESSION_ID = 1L;

    private final CourtRepository courtRepository;
    private final SessionStateRepository sessionStateRepository;

    @Transactional(readOnly = true)
    public int getCurrentRound() {
        return sessionStateRepository.findById(SESSION_ID)
                .map(SessionState::getRoundNumber)
                .orElse(1);
    }

    @Transactional
    public void resetSession() {
        courtRepository.deleteAll();
        sessionStateRepository.save(new SessionState(SESSION_ID, 1));
    }

    @Transactional(readOnly = true)
    public List<Court> getAllCourts() {
        return courtRepository.findAllByOrderByIdAsc();
    }

    @Transactional
    public void addCourt(Pair team1, Pair team2, Queue<String> reserves) {
        courtRepository.save(new Court(team1, team2, reserves));
    }

    @Transactional
    public void deleteCourtById(long id) {
        if (!courtRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Court not found");
        }
        courtRepository.deleteById(id);
    }

    @Transactional
    public void updateWinner(long id, String winner) {
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Court not found"));
        try {
            court.setWinner(winner);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        courtRepository.save(court);
    }

    /**
     * Rotates the ladder: winners move up, losers move down, partners are split and
     * reserve players enter the courts in FIFO order. Every court must have a winner.
     */
    @Transactional
    public void nextMatches() {
        List<Court> courts = courtRepository.findAllByOrderByIdAsc();
        if (courts.isEmpty()) {
            return;
        }
        if (courts.stream().anyMatch(court -> !court.hasResult())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Every court needs a winner before rotating");
        }

        List<Pair> winners = new ArrayList<>(courts.size());
        List<Pair> losers = new ArrayList<>(courts.size());
        List<Queue<String>> queues = new ArrayList<>(courts.size());
        for (Court court : courts) {
            winners.add(court.getWinners());
            losers.add(court.getLosers());
            queues.add(new LinkedList<>(court.getReserves()));
        }

        if (courts.size() == 1) {
            Pair win = winners.getFirst();
            Pair lose = losers.getFirst();
            Queue<String> queue = queues.getFirst();

            queue.add(win.player1());
            queue.add(lose.player1());
            queue.add(win.player2());
            queue.add(lose.player2());

            assignFromQueue(courts.getFirst(), queue);
            courtRepository.save(courts.getFirst());
            incrementRound();
            return;
        }

        for (int i = 0; i < courts.size(); i++) {
            Queue<String> queue = queues.get(i);
            if (i == 0) {
                // Top court: keeps its own winners and receives the winners of the court below.
                Pair currentWinner = winners.get(i);
                Pair nextWinner = winners.get(i + 1);
                queue.add(currentWinner.player1());
                queue.add(nextWinner.player1());
                queue.add(currentWinner.player2());
                queue.add(nextWinner.player2());
            } else if (i == courts.size() - 1) {
                // Bottom court: keeps its own losers and receives the losers of the court above.
                Pair currentLoser = losers.get(i);
                Pair previousLoser = losers.get(i - 1);
                queue.add(currentLoser.player1());
                queue.add(previousLoser.player1());
                queue.add(currentLoser.player2());
                queue.add(previousLoser.player2());
            } else {
                // Middle courts: winners move up, losers move down.
                Pair nextWinner = winners.get(i + 1);
                Pair previousLoser = losers.get(i - 1);
                queue.add(nextWinner.player1());
                queue.add(previousLoser.player1());
                queue.add(nextWinner.player2());
                queue.add(previousLoser.player2());
            }
        }

        for (int i = 0; i < courts.size(); i++) {
            assignFromQueue(courts.get(i), queues.get(i));
        }

        courtRepository.saveAll(courts);
        incrementRound();
    }

    private void incrementRound() {
        SessionState state = sessionStateRepository.findById(SESSION_ID)
                .orElseGet(() -> new SessionState(SESSION_ID, 1));
        state.setRoundNumber(state.getRoundNumber() + 1);
        sessionStateRepository.save(state);
    }

    private static void assignFromQueue(Court court, Queue<String> queue) {
        court.setTeam1(new Pair(queue.poll(), queue.poll()));
        court.setTeam2(new Pair(queue.poll(), queue.poll()));
        court.setReserves(new ArrayList<>(queue));
        court.resetResult();
    }
}
