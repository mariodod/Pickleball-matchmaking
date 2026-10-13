package com.pickleball.pickleball.repository;

import com.pickleball.pickleball.model.SessionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionStateRepository extends JpaRepository<SessionState, Long> {
}
