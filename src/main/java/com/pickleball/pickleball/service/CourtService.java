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
import java.util.List;
import java.util.Queue;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;
    private final SessionStateRepository sessionStateRepository;

    @Transactional(readOnly = true)
    public int getCurrentRound() {
        return sessionStateRepository.findById(1L)
                .map(SessionState::getRoundNumber)
                .orElse(1);
    }

    @Transactional
    public void resetSession() {
        courtRepository.deleteAll();
        sessionStateRepository.save(new SessionState(1L, 1));
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
    public void deleteCourt() {
        List<Court> courts = courtRepository.findAllByOrderByIdAsc();
        if (courts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No courts to delete");
        }
        courtRepository.delete(courts.getLast());
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
            courtRepository.save(court);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @Transactional
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

        List<Queue<String>> queues = new ArrayList<>(courts.size());
        for (Court court : courts) {
            queues.add(court.getReservesQueue());
        }

        if (courts.size() == 1) {
            Court court = courts.getFirst();
            Pair win = winners.getFirst();
            Pair lose = losers.getFirst();
            Queue<String> queue = queues.getFirst();

            queue.add(win.player1());
            queue.add(lose.player1());
            queue.add(win.player2());
            queue.add(lose.player2());

            assignFromQueue(court, queue);
            courtRepository.save(court);
            incrementRound();
            return;
        }

        for (int i = 0; i < courts.size(); i++) {
            Queue<String> currentQueue = queues.get(i);
            if (i == 0) {
                Pair currWin = winners.get(i);
                Pair nextWin = winners.get(i + 1);
                currentQueue.add(currWin.player1());
                currentQueue.add(nextWin.player1());
                currentQueue.add(currWin.player2());
                currentQueue.add(nextWin.player2());
            } else if (i == courts.size() - 1) {
                Pair currLose = losers.get(i);
                Pair prevLose = losers.get(i - 1);
                currentQueue.add(currLose.player1());
                currentQueue.add(prevLose.player1());
                currentQueue.add(currLose.player2());
                currentQueue.add(prevLose.player2());
            } else {
                Pair nextWin = winners.get(i + 1);
                Pair prevLose = losers.get(i - 1);
                currentQueue.add(nextWin.player1());
                currentQueue.add(prevLose.player1());
                currentQueue.add(nextWin.player2());
                currentQueue.add(prevLose.player2());
            }
        }

        for (int i = 0; i < courts.size(); i++) {
            assignFromQueue(courts.get(i), queues.get(i));
        }

        courtRepository.saveAll(courts);
        incrementRound();
    }

    private void incrementRound() {
        SessionState state = sessionStateRepository.findById(1L)
                .orElseGet(() -> new SessionState(1L, 1));
        state.setRoundNumber(state.getRoundNumber() + 1);
        sessionStateRepository.save(state);
    }

    private static void assignFromQueue(Court court, Queue<String> queue) {
        court.setTeam1(new Pair(queue.poll(), queue.poll()));
        court.setTeam2(new Pair(queue.poll(), queue.poll()));
        court.setReservesFromQueue(queue);
        court.resetResult();
    }
}
