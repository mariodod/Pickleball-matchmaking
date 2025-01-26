package com.pickleball.pickleball.controller;

import com.pickleball.pickleball.model.Court;
import com.pickleball.pickleball.service.CourtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/courts")
@RequiredArgsConstructor
public class CourtController {

    private final CourtService courtService;

    @GetMapping
    public Collection<Court> getAllCourts() {
        return courtService.getAllCourts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Court> getCourtById(@PathVariable int id) {
        Court court = courtService.getCourtById(id);
        if (court != null) {
            return ResponseEntity.ok(court);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Court> addCourt(@RequestBody Court court) {
        Court addedCourt = courtService.addCourt(court);
        if (addedCourt != null) {
            return ResponseEntity.ok(addedCourt);
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Court> deleteCourt(@PathVariable int id) {
        var court = courtService.deleteCourt(id);
        if (court != null) {
            return ResponseEntity.ok(court);
        }
        return ResponseEntity.notFound().build();
    }

}