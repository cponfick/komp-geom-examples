package io.github.cponfick.examples.pool;

import io.github.cponfick.kompgeom.euclidean.twod.Vec2;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Builds the conventional fifteen-ball eight-ball rack. */
final class Rack {
    private Rack() { }

    static List<Ball> create() {
        List<Ball> balls = new ArrayList<>();
        balls.add(new Ball(new Vec2(205, 320), 0, Color.WHITE, true));
        int[][] numbers = {{1}, {9, 2}, {3, 8, 10}, {11, 4, 12, 5}, {13, 6, 14, 15, 7}};
        for (int row = 0; row < numbers.length; row++) {
            for (int column = 0; column < numbers[row].length; column++) {
                int number = numbers[row][column];
                balls.add(new Ball(new Vec2(690 + row * 27, 320 + column * 27 - row * 13.5),
                        number, colorFor(number), false));
            }
        }
        return balls;
    }

    private static Color colorFor(int number) {
        return switch ((number - 1) % 8) {
            case 0 -> Color.YELLOW;
            case 1 -> Color.BLUE;
            case 2 -> Color.RED;
            case 3 -> Color.MAGENTA;
            case 4 -> Color.ORANGE;
            case 5 -> Color.GREEN;
            case 6 -> Color.RED.darker();
            default -> Color.BLACK;
        };
    }
}
