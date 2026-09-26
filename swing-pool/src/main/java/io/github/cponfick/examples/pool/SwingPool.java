package io.github.cponfick.examples.pool;

import io.github.cponfick.kompgeom.euclidean.twod.Seg2;
import io.github.cponfick.kompgeom.euclidean.twod.Vec2;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** A small Swing pool game using Komp-Geom vectors and segments. */
public final class SwingPool {
    private static final int WIDTH = 980;
    private static final int HEIGHT = 640;
    private static final double LEFT = 70, RIGHT = 910, TOP = 100, BOTTOM = 540;
    private static final double BALL_RADIUS = 13;
    private static final double FRICTION = .985;
    private static final double STOP_SPEED = 2.0;

    private final List<Ball> balls = new ArrayList<>();
    private final List<Seg2> rails = List.of(
            new Seg2(new Vec2(LEFT, TOP), new Vec2(RIGHT, TOP)),
            new Seg2(new Vec2(RIGHT, TOP), new Vec2(RIGHT, BOTTOM)),
            new Seg2(new Vec2(RIGHT, BOTTOM), new Vec2(LEFT, BOTTOM)),
            new Seg2(new Vec2(LEFT, BOTTOM), new Vec2(LEFT, TOP)));
    private final TablePanel panel = new TablePanel();
    private double aim = 0;
    private double shotPower = .1;
    private long chargeStarted;
    private boolean charging;
    private int score;
    private int shots;
    private int currentPlayer = 1;
    private final int[] playerGroup = new int[3]; // 0=open, 1=solids, 2=stripes
    private boolean breakShot = true;
    private boolean ballInHand;
    private boolean shotInProgress;
    private boolean gameOver;
    private boolean cueScratch;
    private Ball firstHit;
    private final List<Integer> pocketedThisShot = new ArrayList<>();
    private String message = "Break the rack!";

    private SwingPool() {
        reset();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SwingPool game = new SwingPool();
            JFrame frame = new JFrame("Komp-Geom Pool");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(game.panel);
            // Set the size explicitly so the game remains visible on window managers
            // that do not honor a JPanel preferred size during pack().
            frame.setSize(WIDTH, HEIGHT);
            frame.setMinimumSize(new Dimension(WIDTH, HEIGHT));
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
            game.panel.requestFocusInWindow();
            new Timer(16, event -> {
                game.update(1.0 / 60.0);
                game.panel.repaint();
            }).start();
        });
    }

    private void reset() {
        balls.clear();
        balls.add(new Ball(new Vec2(205, 320), 0, Color.WHITE, false));
        // A standard rack: seven solids, the eight ball, and seven stripes.
        // A conventional WPA-style pattern: 1 at the apex, 8 in the
        // middle of the third row, and unlike groups in the rear corners.
        int[][] rack = {
                {1},
                {9, 2},
                {3, 8, 10},
                {11, 4, 12, 5},
                {13, 6, 14, 15, 7}
        };
        for (int row = 0; row < rack.length; row++) {
            for (int column = 0; column < rack[row].length; column++) {
                int number = rack[row][column];
                balls.add(new Ball(new Vec2(690 + row * 27, 320 + column * 27 - row * 13.5),
                        number, ballColor(number), false));
            }
        }
        aim = 0;
        shotPower = .1;
        charging = false;
        score = 0;
        shots = 0;
        currentPlayer = 1;
        playerGroup[1] = playerGroup[2] = 0;
        breakShot = true;
        ballInHand = false;
        shotInProgress = false;
        gameOver = false;
        message = "Break the rack!";
    }

    private Color ballColor(int number) {
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

    private boolean stopped() {
        for (Ball ball : balls) if (ball.active && ball.velocity.norm() > STOP_SPEED) return false;
        return true;
    }

    private void startCharging() {
        if (stopped() && !gameOver && !charging) {
            charging = true;
            chargeStarted = System.nanoTime();
            shotPower = .1;
        }
    }

    private void releaseShot() {
        if (!charging) return;
        shotPower = Math.min(1, Math.max(.1, (System.nanoTime() - chargeStarted) / 1_500_000_000.0));
        charging = false;
        shoot();
    }

    private void shoot() {
        if (!stopped() || gameOver) return;
        Ball cue = balls.get(0);
        cue.active = true;
        double speed = 250 + shotPower * 900;
        cue.velocity = new Vec2(Math.cos(aim), Math.sin(aim)).times(speed);
        shots++;
        shotInProgress = true;
        cueScratch = false;
        firstHit = null;
        pocketedThisShot.clear();
    }

    private void update(double dt) {
        if (charging) {
            shotPower = Math.min(1, Math.max(.1, (System.nanoTime() - chargeStarted) / 1_500_000_000.0));
        }
        for (Ball ball : balls) {
            if (!ball.active) continue;
            ball.position = ball.position.plus(ball.velocity.times(dt));
            ball.velocity = ball.velocity.times(Math.pow(FRICTION, dt * 60));
            if (ball.velocity.norm() < STOP_SPEED) ball.velocity = new Vec2(0, 0);
            bounceOffRails(ball);
            if (inPocket(ball.position)) pocket(ball);
        }
        for (int i = 0; i < balls.size(); i++) {
            for (int j = i + 1; j < balls.size(); j++) collide(balls.get(i), balls.get(j));
        }
        if (shotInProgress && stopped()) resolveShot();
    }

    private void bounceOffRails(Ball ball) {
        for (Seg2 rail : rails) {
            Vec2 start = rail.getStart();
            Vec2 end = rail.getEnd();
            if (start.getY() == end.getY()) {
                if (ball.position.getY() < TOP + BALL_RADIUS) {
                    ball.position = new Vec2(ball.position.getX(), TOP + BALL_RADIUS);
                    ball.velocity = new Vec2(ball.velocity.getX(), Math.abs(ball.velocity.getY()));
                } else if (ball.position.getY() > BOTTOM - BALL_RADIUS) {
                    ball.position = new Vec2(ball.position.getX(), BOTTOM - BALL_RADIUS);
                    ball.velocity = new Vec2(ball.velocity.getX(), -Math.abs(ball.velocity.getY()));
                }
            } else if (ball.position.getX() < LEFT + BALL_RADIUS) {
                ball.position = new Vec2(LEFT + BALL_RADIUS, ball.position.getY());
                ball.velocity = new Vec2(Math.abs(ball.velocity.getX()), ball.velocity.getY());
            } else if (ball.position.getX() > RIGHT - BALL_RADIUS) {
                ball.position = new Vec2(RIGHT - BALL_RADIUS, ball.position.getY());
                ball.velocity = new Vec2(-Math.abs(ball.velocity.getX()), ball.velocity.getY());
            }
        }
    }

    private void collide(Ball a, Ball b) {
        if (!a.active || !b.active) return;
        Vec2 delta = b.position.minus(a.position);
        double distance = delta.norm();
        if (distance == 0 || distance >= BALL_RADIUS * 2) return;
        Vec2 normal = delta.times(1 / distance);
        if (shotInProgress && firstHit == null) {
            if (a.cue && !b.cue) firstHit = b;
            else if (b.cue && !a.cue) firstHit = a;
        }
        double relativeSpeed = b.velocity.minus(a.velocity).dot(normal);
        if (relativeSpeed >= 0) return;
        Vec2 impulse = normal.times(relativeSpeed);
        a.velocity = a.velocity.plus(impulse);
        b.velocity = b.velocity.minus(impulse);
        double correction = (BALL_RADIUS * 2 - distance) / 2;
        a.position = a.position.minus(normal.times(correction));
        b.position = b.position.plus(normal.times(correction));
    }

    private boolean inPocket(Vec2 position) {
        for (double[] pocket : pockets()) {
            if (position.distance(new Vec2(pocket[0], pocket[1])) < 28) return true;
        }
        return false;
    }

    private void pocket(Ball ball) {
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
        if (!shotInProgress) return;
        shotInProgress = false;
        Ball cue = balls.get(0);
        boolean eightPocketed = pocketedThisShot.contains(8);
        if (eightPocketed) {
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

    private int otherPlayer() { return currentPlayer == 1 ? 2 : 1; }

    private int groupOf(int number) { return number >= 9 ? 2 : 1; }

    private void assignGroups() {
        for (int number : pocketedThisShot) {
            if (number == 8) continue;
            int group = groupOf(number);
            playerGroup[currentPlayer] = group;
            playerGroup[otherPlayer()] = 3 - group;
            message = "Player " + currentPlayer + " has " + (group == 1 ? "solids" : "stripes") + ".";
            return;
        }
    }

    private int remainingGroupBalls(int group) {
        int count = 0;
        for (Ball ball : balls) if (ball.active && !ball.cue && groupOf(ball.number) == group) count++;
        return count;
    }

    private double[][] pockets() {
        return new double[][]{{LEFT, TOP}, {RIGHT, TOP}, {LEFT, BOTTOM}, {RIGHT, BOTTOM},
                {(LEFT + RIGHT) / 2, TOP}, {(LEFT + RIGHT) / 2, BOTTOM}};
    }

    private final class TablePanel extends JPanel {
        TablePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setBackground(new Color(24, 27, 34));
            setFocusable(true);
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent event) {
                    Ball cue = balls.get(0);
                    if (stopped() && ballInHand) {
                        cue.position = new Vec2(Math.max(LEFT + BALL_RADIUS, Math.min(RIGHT - BALL_RADIUS, event.getX())),
                                Math.max(TOP + BALL_RADIUS, Math.min(BOTTOM - BALL_RADIUS, event.getY())));
                    }
                    if (stopped()) aim = Math.atan2(event.getY() - cue.position.getY(),
                            event.getX() - cue.position.getX());
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent event) {
                    requestFocusInWindow();
                    startCharging();
                }
                @Override public void mouseReleased(MouseEvent event) { releaseShot(); }
            });
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent event) {
                    if (event.getKeyCode() == KeyEvent.VK_SPACE || event.getKeyCode() == KeyEvent.VK_ENTER) startCharging();
                    else if (event.getKeyCode() == KeyEvent.VK_R) reset();
                    else if (event.getKeyCode() == KeyEvent.VK_LEFT || event.getKeyCode() == KeyEvent.VK_A) aim -= .08;
                    else if (event.getKeyCode() == KeyEvent.VK_RIGHT || event.getKeyCode() == KeyEvent.VK_D) aim += .08;
                    else if (event.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
                }
                @Override public void keyReleased(KeyEvent event) {
                    if (event.getKeyCode() == KeyEvent.VK_SPACE || event.getKeyCode() == KeyEvent.VK_ENTER) releaseShot();
                }
            });
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            g.drawString("KOMP-GEOM POOL", 70, 42);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            g.drawString("Solo game    Score: " + score + "    Shots: " + shots, 70, 70);
            g.setColor(new Color(225, 235, 220));
            g.drawString(message + (ballInHand ? "  Place the cue ball with the mouse." : ""), 70, 88);
            g.setColor(new Color(112, 65, 31));
            g.fillRoundRect((int) LEFT - 24, (int) TOP - 24, (int) (RIGHT - LEFT) + 48, (int) (BOTTOM - TOP) + 48, 30, 30);
            g.setColor(new Color(31, 126, 73));
            g.fillRect((int) LEFT, (int) TOP, (int) (RIGHT - LEFT), (int) (BOTTOM - TOP));
            for (double[] pocket : pockets()) {
                g.setColor(Color.BLACK);
                g.fillOval((int) pocket[0] - 18, (int) pocket[1] - 18, 36, 36);
            }
            if (stopped() && !gameOver) drawAim(g);
            for (Ball ball : balls) drawBall(g, ball);
            g.dispose();
        }

        private void drawAim(Graphics2D g) {
            Ball cue = balls.get(0);
            g.setColor(new Color(255, 255, 255, 150));
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double guideLength = 100 + shotPower * 300;
            g.drawLine((int) cue.position.getX(), (int) cue.position.getY(),
                    (int) (cue.position.getX() + Math.cos(aim) * guideLength),
                    (int) (cue.position.getY() + Math.sin(aim) * guideLength));
        }

        private void drawBall(Graphics2D g, Ball ball) {
            if (!ball.active) return;
            int x = (int) Math.round(ball.position.getX() - BALL_RADIUS);
            int y = (int) Math.round(ball.position.getY() - BALL_RADIUS);
            g.setColor(new Color(0, 0, 0, 80));
            g.fillOval(x + 3, y + 4, (int) BALL_RADIUS * 2, (int) BALL_RADIUS * 2);
            if (ball.number >= 9) {
                g.setColor(Color.WHITE);
                g.fillOval(x, y, (int) BALL_RADIUS * 2, (int) BALL_RADIUS * 2);
                g.setColor(ball.color);
                g.fillRect(x, y + 7, (int) BALL_RADIUS * 2, 12);
            } else {
                g.setColor(ball.color);
                g.fillOval(x, y, (int) BALL_RADIUS * 2, (int) BALL_RADIUS * 2);
            }
            g.setColor(Color.WHITE);
            g.drawOval(x, y, (int) BALL_RADIUS * 2, (int) BALL_RADIUS * 2);
            if (ball.number > 0) {
                g.setColor(ball.number == 8 ? Color.WHITE : Color.BLACK);
                g.fillOval(x + 6, y + 6, 14, 14);
                g.setColor(ball.number == 8 ? Color.BLACK : Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                String label = Integer.toString(ball.number);
                int labelWidth = g.getFontMetrics().stringWidth(label);
                g.drawString(label, (int) ball.position.getX() - labelWidth / 2,
                        (int) ball.position.getY() + 4); 
            }
        }
    }

    private static final class Ball {
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
}
