package io.github.cponfick.examples.pool;

import io.github.cponfick.kompgeom.euclidean.twod.Seg2;
import io.github.cponfick.kompgeom.euclidean.twod.Vec2;

import java.util.ArrayList;
import java.util.List;

/** Game state and the deliberately small solo eight-ball rule set. */
final class PoolGame {
    static final double LEFT = 70, RIGHT = 910, TOP = 100, BOTTOM = 540;
    static final double BALL_RADIUS = 13;
    static final double STOP_SPEED = 2.0;

    final List<Ball> balls = new ArrayList<>();
    final List<Seg2> rails = List.of(
            new Seg2(new Vec2(LEFT, TOP), new Vec2(RIGHT, TOP)),
            new Seg2(new Vec2(RIGHT, TOP), new Vec2(RIGHT, BOTTOM)),
            new Seg2(new Vec2(RIGHT, BOTTOM), new Vec2(LEFT, BOTTOM)),
            new Seg2(new Vec2(LEFT, BOTTOM), new Vec2(LEFT, TOP)));
    double aim;
    double shotPower = .1;
    long chargeStarted;
    boolean charging;
    int score;
    int shots;
    boolean shotInProgress;
    boolean gameOver;
    boolean cueScratch;
    Ball firstHit;
    final List<Integer> pocketedThisShot = new ArrayList<>();
    String message = "Break the rack!";

    PoolGame() { reset(); }

    void reset() {
        balls.clear();
        balls.addAll(Rack.create());
        aim = 0;
        shotPower = .1;
        charging = false;
        score = 0;
        shots = 0;
        shotInProgress = false;
        gameOver = false;
        message = "Break the rack!";
    }

    boolean stopped() {
        for (Ball ball : balls) if (ball.active && ball.velocity.norm() > STOP_SPEED) return false;
        return true;
    }

    void startCharging() {
        if (stopped() && !gameOver && !charging) {
            charging = true;
            chargeStarted = System.nanoTime();
            shotPower = .1;
        }
    }

    void releaseShot() {
        if (!charging) return;
        shotPower = Math.min(1, Math.max(.1, (System.nanoTime() - chargeStarted) / 1_500_000_000.0));
        charging = false;
        shoot();
    }

    private void shoot() {
        if (!stopped() || gameOver) return;
        Ball cue = balls.get(0);
        cue.active = true;
        cue.velocity = new Vec2(Math.cos(aim), Math.sin(aim)).times(250 + shotPower * 900);
        shots++;
        shotInProgress = true;
        cueScratch = false;
        firstHit = null;
        pocketedThisShot.clear();
    }

    void update(double dt) {
        if (charging) shotPower = Math.min(1, Math.max(.1,
                (System.nanoTime() - chargeStarted) / 1_500_000_000.0));
        PoolPhysics.update(this, dt);
        if (shotInProgress && stopped()) resolveShot();
    }

    void pocket(Ball ball) {
        if (!ball.active) return;
        ball.active = false;
        ball.velocity = new Vec2(0, 0);
        if (ball.cue) {
            cueScratch = true;
        } else {
            pocketedThisShot.add(ball.number);
            score += 100;
        }
    }

    private void resolveShot() {
        shotInProgress = false;
        Ball cue = balls.get(0);
        if (pocketedThisShot.contains(8)) {
            gameOver = true;
            message = remainingObjectBalls() == 0
                    ? "You win! The 8-ball was last. Press R to play again."
                    : "The 8-ball was pocketed too early. Press R to try again.";
        } else {
            message = cueScratch ? "Scratch! The cue ball has been reset." : "Take your next shot.";
        }
        if (cueScratch) {
            cue.active = true;
            cue.position = new Vec2(205, 320);
            cue.velocity = new Vec2(0, 0);
        }
        pocketedThisShot.clear();
    }

    private int remainingObjectBalls() {
        int count = 0;
        for (Ball ball : balls) if (ball.active && !ball.cue && ball.number != 8) count++;
        return count;
    }

    double[][] pockets() {
        return new double[][]{{LEFT, TOP}, {RIGHT, TOP}, {LEFT, BOTTOM}, {RIGHT, BOTTOM},
                {(LEFT + RIGHT) / 2, TOP}, {(LEFT + RIGHT) / 2, BOTTOM}};
    }
}
