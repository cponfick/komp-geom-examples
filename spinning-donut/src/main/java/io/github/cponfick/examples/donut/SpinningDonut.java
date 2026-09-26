package io.github.cponfick.examples.donut;

import io.github.cponfick.kompgeom.core.AngleUnit;
import io.github.cponfick.kompgeom.euclidean.threed.AffineTransformationMatrix3;
import io.github.cponfick.kompgeom.euclidean.threed.Vec3;

/**
 * A small terminal renderer for a rotating torus.
 *
 * <p>The torus points and normals are Komp-Geom {@link Vec3}s. Each frame uses
 * {@link AffineTransformationMatrix3} to rotate them before perspective projection.
 */
public final class SpinningDonut {
    private static final int WIDTH = 80;
    private static final int HEIGHT = 22;
    private static final String SHADES = ".,-~:;=!*#$@";

    private SpinningDonut() {}

    public static void main(String[] args) throws InterruptedException {
        double angleX = 0.0;
        double angleZ = 0.0;
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> System.out.print("\033[?25h\033[0m")));

        // Clear the screen and hide the terminal cursor while frames replace one another.
        System.out.print("\033[2J\033[H\033[?25l");
        try {
            while (true) {
                System.out.print("\033[H");
                System.out.print(render(angleX, angleZ));
                System.out.flush();
                angleX += 0.04;
                angleZ += 0.02;
                Thread.sleep(15);
            }
        } finally {
            System.out.print("\033[?25h");
        }
    }

    private static String render(double angleX, double angleZ) {
        char[][] pixels = new char[HEIGHT][WIDTH];
        double[][] depth = new double[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                pixels[y][x] = ' ';
                depth[y][x] = 0.0;
            }
        }

        // Compose rotations with Komp-Geom instead of rotating coordinates by hand.
        AffineTransformationMatrix3 transform = AffineTransformationMatrix3.Companion
                .createRotationX(angleX, AngleUnit.RADIANS)
                .rotateZ(angleZ, AngleUnit.RADIANS);
        // The reference uses the (intentionally unnormalized) light direction (0, 1, -1).
        Vec3 light = new Vec3(0.0, 1.0, -1.0);

        // These match the reference implementation: theta steps by .07 and phi by .02.
        for (int tube = 0; tube < 90; tube++) {
            double v = 2.0 * Math.PI * tube / 90.0;
            for (int side = 0; side < 315; side++) {
                double u = 2.0 * Math.PI * side / 315.0;
                double cosU = Math.cos(u);
                double sinU = Math.sin(u);
                double cosV = Math.cos(v);
                double sinV = Math.sin(v);

                // The reference torus uses R1=1 (tube radius) and R2=2 (major radius).
                Vec3 point = new Vec3((2.0 + cosV) * cosU,
                        sinV,
                        (2.0 + cosV) * sinU);
                Vec3 normal = new Vec3(cosV * cosU, sinV, cosV * sinU);
                Vec3 rotated = transform.apply(point);
                Vec3 rotatedNormal = transform.apply(normal);

                double cameraZ = rotated.getZ() + 5.0;
                if (cameraZ <= 0.0) {
                    continue;
                }
                double inverseDepth = 1.0 / cameraZ;
                int x = (int) (WIDTH / 2.0 + rotated.getX() * 30.0 * inverseDepth);
                int y = (int) (HEIGHT / 2.0 - rotated.getY() * 15.0 * inverseDepth);
                if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT || inverseDepth <= depth[y][x]) {
                    continue;
                }

                double brightness = rotatedNormal.normalize().dot(light);
                if (brightness <= 0.0) {
                    continue;
                }
                int shade = Math.min(SHADES.length() - 1, (int) (brightness * 8.0));
                depth[y][x] = inverseDepth;
                pixels[y][x] = SHADES.charAt(shade);
            }
        }

        StringBuilder output = new StringBuilder(WIDTH * (HEIGHT + 1));
        for (char[] row : pixels) {
            output.append(row).append('\n');
        }
        return output.toString();
    }
}
