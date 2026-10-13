package com.pickleball.pickleball.repository;

import com.pickleball.pickleball.model.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {

    List<Court> findAllByOrderByIdAsc();
}
