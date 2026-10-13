package com.pickleball.pickleball.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "session_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionState {

    @Id
    private Long id = 1L;

    private int roundNumber = 1;
}
