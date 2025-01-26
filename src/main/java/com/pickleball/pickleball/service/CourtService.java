package com.pickleball.pickleball.service;

import com.pickleball.pickleball.model.Court;
import com.pickleball.pickleball.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;

    public Collection<Court> getAllCourts() {
        return courtRepository.getAll();
    }

    public Court getCourtById(int id) {
        return courtRepository.getOne(id);
    }

    public Court addCourt(Court court) {
        return courtRepository.add(court);
    }

    public Court deleteCourt(int id) {
        return courtRepository.delete(id);
    }
}
