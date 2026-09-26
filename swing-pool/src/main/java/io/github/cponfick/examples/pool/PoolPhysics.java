package io.github.cponfick.examples.pool;

import io.github.cponfick.kompgeom.euclidean.twod.Seg2;
import io.github.cponfick.kompgeom.euclidean.twod.Vec2;

/** Movement and collision calculations, expressed with Komp-Geom vectors. */
final class PoolPhysics {
    private static final double FRICTION = .985;

    private PoolPhysics() { }

    static void update(PoolGame game, double dt) {
        for (Ball ball : game.balls) {
            if (!ball.active) continue;
            ball.position = ball.position.plus(ball.velocity.times(dt));
            ball.velocity = ball.velocity.times(Math.pow(FRICTION, dt * 60));
            if (ball.velocity.norm() < PoolGame.STOP_SPEED) ball.velocity = new Vec2(0, 0);
            bounceOffRails(game, ball);
            if (inPocket(game, ball.position)) game.pocket(ball);
        }
        for (int i = 0; i < game.balls.size(); i++) {
            for (int j = i + 1; j < game.balls.size(); j++) collide(game, game.balls.get(i), game.balls.get(j));
        }
    }

    private static void bounceOffRails(PoolGame game, Ball ball) {
        for (Seg2 rail : game.rails) {
            Vec2 start = rail.getStart();
            Vec2 end = rail.getEnd();
            if (start.getY() == end.getY()) {
                if (ball.position.getY() < PoolGame.TOP + PoolGame.BALL_RADIUS) {
                    ball.position = new Vec2(ball.position.getX(), PoolGame.TOP + PoolGame.BALL_RADIUS);
                    ball.velocity = new Vec2(ball.velocity.getX(), Math.abs(ball.velocity.getY()));
                } else if (ball.position.getY() > PoolGame.BOTTOM - PoolGame.BALL_RADIUS) {
                    ball.position = new Vec2(ball.position.getX(), PoolGame.BOTTOM - PoolGame.BALL_RADIUS);
                    ball.velocity = new Vec2(ball.velocity.getX(), -Math.abs(ball.velocity.getY()));
                }
            } else if (ball.position.getX() < PoolGame.LEFT + PoolGame.BALL_RADIUS) {
                ball.position = new Vec2(PoolGame.LEFT + PoolGame.BALL_RADIUS, ball.position.getY());
                ball.velocity = new Vec2(Math.abs(ball.velocity.getX()), ball.velocity.getY());
            } else if (ball.position.getX() > PoolGame.RIGHT - PoolGame.BALL_RADIUS) {
                ball.position = new Vec2(PoolGame.RIGHT - PoolGame.BALL_RADIUS, ball.position.getY());
                ball.velocity = new Vec2(-Math.abs(ball.velocity.getX()), ball.velocity.getY());
            }
        }
    }

    private static void collide(PoolGame game, Ball a, Ball b) {
        if (!a.active || !b.active) return;
        Vec2 delta = b.position.minus(a.position);
        double distance = delta.norm();
        if (distance == 0 || distance >= PoolGame.BALL_RADIUS * 2) return;
        Vec2 normal = delta.times(1 / distance);
        if (game.shotInProgress && game.firstHit == null) {
            if (a.cue && !b.cue) game.firstHit = b;
            else if (b.cue && !a.cue) game.firstHit = a;
        }
        double relativeSpeed = b.velocity.minus(a.velocity).dot(normal);
        if (relativeSpeed >= 0) return;
        Vec2 impulse = normal.times(relativeSpeed);
        a.velocity = a.velocity.plus(impulse);
        b.velocity = b.velocity.minus(impulse);
        double correction = (PoolGame.BALL_RADIUS * 2 - distance) / 2;
        a.position = a.position.minus(normal.times(correction));
        b.position = b.position.plus(normal.times(correction));
    }

    private static boolean inPocket(PoolGame game, Vec2 position) {
        for (double[] pocket : game.pockets()) {
            if (position.distance(new Vec2(pocket[0], pocket[1])) < 28) return true;
        }
        return false;
    }
}
