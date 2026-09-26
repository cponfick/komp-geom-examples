package io.github.cponfick.examples.pool;

import io.github.cponfick.kompgeom.euclidean.twod.Vec2;

import java.awt.Color;

/** Mutable state for one pool ball. */
final class Ball {
    Vec2 position;
    Vec2 velocity = new Vec2(0, 0);
    final int number;
    final Color color;
    final boolean cue;
    boolean active = true;

    Ball(Vec2 position, int number, Color color, boolean cue) {
        this.position = position;
        this.number = number;
        this.color = color;
        this.cue = cue;
    }
}
