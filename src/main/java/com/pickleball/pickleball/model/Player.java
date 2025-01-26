package com.pickleball.pickleball.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Player {

    private final int id;
    private final String name;
}
