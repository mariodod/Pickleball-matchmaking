package com.pickleball.pickleball.model;

/**
 * A doubles team. The "player1" slot is the priority slot: during a rotation the
 * player1 of every incoming team is queued ahead of the player2 slots.
 */
public record Pair(String player1, String player2) {
}
