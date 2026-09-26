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
public final class TerminalPool {
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
    private int score;
    private int shots;

    private TerminalPool() {
        reset();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TerminalPool game = new TerminalPool();
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
        balls.add(new Ball(new Vec2(205, 320), Color.WHITE, true));
        Color[] colors = {Color.YELLOW, Color.RED, Color.BLUE, Color.ORANGE, Color.MAGENTA, Color.CYAN};
        int index = 0;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column <= row; column++) {
                balls.add(new Ball(new Vec2(690 + row * 27, 307 + column * 27 - row * 13.5), colors[index++], false));
            }
        }
        aim = 0;
        score = 0;
        shots = 0;
    }

    private boolean stopped() {
        for (Ball ball : balls) if (ball.active && ball.velocity.norm() > STOP_SPEED) return false;
        return true;
    }

    private void shoot() {
        if (!stopped()) return;
        balls.get(0).velocity = new Vec2(Math.cos(aim), Math.sin(aim)).times(650);
        shots++;
    }

    private void update(double dt) {
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
        ball.active = false;
        ball.velocity = new Vec2(0, 0);
        if (ball.cue) {
            ball.active = true;
            ball.position = new Vec2(205, 320);
            score = Math.max(0, score - 25);
        } else {
            score += 100;
        }
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
                    if (stopped()) aim = Math.atan2(event.getY() - balls.get(0).position.getY(),
                            event.getX() - balls.get(0).position.getX());
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent event) { shoot(); requestFocusInWindow(); }
            });
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent event) {
                    if (event.getKeyCode() == KeyEvent.VK_SPACE || event.getKeyCode() == KeyEvent.VK_ENTER) shoot();
                    else if (event.getKeyCode() == KeyEvent.VK_R) reset();
                    else if (event.getKeyCode() == KeyEvent.VK_LEFT || event.getKeyCode() == KeyEvent.VK_A) aim -= .08;
                    else if (event.getKeyCode() == KeyEvent.VK_RIGHT || event.getKeyCode() == KeyEvent.VK_D) aim += .08;
                    else if (event.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
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
            g.drawString("Score: " + score + "    Shots: " + shots + "    Move mouse to aim • Click or Space to shoot • R to reset", 70, 70);

            g.setColor(new Color(112, 65, 31));
            g.fillRoundRect((int) LEFT - 24, (int) TOP - 24, (int) (RIGHT - LEFT) + 48, (int) (BOTTOM - TOP) + 48, 30, 30);
            g.setColor(new Color(31, 126, 73));
            g.fillRect((int) LEFT, (int) TOP, (int) (RIGHT - LEFT), (int) (BOTTOM - TOP));
            for (double[] pocket : pockets()) {
                g.setColor(Color.BLACK);
                g.fillOval((int) pocket[0] - 18, (int) pocket[1] - 18, 36, 36);
            }
            if (stopped()) drawAim(g);
            for (Ball ball : balls) drawBall(g, ball);
            g.dispose();
        }

        private void drawAim(Graphics2D g) {
            Ball cue = balls.get(0);
            g.setColor(new Color(255, 255, 255, 150));
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine((int) cue.position.getX(), (int) cue.position.getY(),
                    (int) (cue.position.getX() + Math.cos(aim) * 250),
                    (int) (cue.position.getY() + Math.sin(aim) * 250));
        }

        private void drawBall(Graphics2D g, Ball ball) {
            if (!ball.active) return;
            int x = (int) Math.round(ball.position.getX() - BALL_RADIUS);
            int y = (int) Math.round(ball.position.getY() - BALL_RADIUS);
            g.setColor(new Color(0, 0, 0, 80));
            g.fillOval(x + 3, y + 4, (int) BALL_RADIUS * 2, (int) BALL_RADIUS * 2);
            g.setColor(ball.color);
            g.fillOval(x, y, (int) BALL_RADIUS * 2, (int) BALL_RADIUS * 2);
            g.setColor(Color.WHITE);
            g.drawOval(x, y, (int) BALL_RADIUS * 2, (int) BALL_RADIUS * 2);
        }
    }

    private static final class Ball {
        Vec2 position;
        Vec2 velocity = new Vec2(0, 0);
        final Color color;
        final boolean cue;
        boolean active = true;

        Ball(Vec2 position, Color color, boolean cue) {
            this.position = position;
            this.color = color;
            this.cue = cue;
        }
    }
}
