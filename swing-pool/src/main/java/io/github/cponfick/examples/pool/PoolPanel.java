package io.github.cponfick.examples.pool;

import io.github.cponfick.kompgeom.euclidean.twod.Vec2;

import javax.swing.JPanel;
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

/** Swing view and input adapter for the pool game. */
final class PoolPanel extends JPanel {
    private static final int WIDTH = 980;
    private static final int HEIGHT = 640;
    private final PoolGame game;

    PoolPanel(PoolGame game) {
        this.game = game;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(24, 27, 34));
        setFocusable(true);
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent event) {
                if (game.stopped()) game.aim = Math.atan2(event.getY() - cue().position.getY(),
                        event.getX() - cue().position.getX());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                game.startCharging();
            }
            @Override public void mouseReleased(MouseEvent event) { game.releaseShot(); }
        });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_SPACE || event.getKeyCode() == KeyEvent.VK_ENTER) game.startCharging();
                else if (event.getKeyCode() == KeyEvent.VK_R) game.reset();
                else if (event.getKeyCode() == KeyEvent.VK_LEFT || event.getKeyCode() == KeyEvent.VK_A) game.aim -= .08;
                else if (event.getKeyCode() == KeyEvent.VK_RIGHT || event.getKeyCode() == KeyEvent.VK_D) game.aim += .08;
                else if (event.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
            }
            @Override public void keyReleased(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_SPACE || event.getKeyCode() == KeyEvent.VK_ENTER) game.releaseShot();
            }
        });
    }

    private Ball cue() { return game.balls.get(0); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        g.drawString("KOMP-GEOM POOL", 70, 42);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        g.drawString("Solo game    Score: " + game.score + "    Shots: " + game.shots, 70, 70);
        g.setColor(new Color(225, 235, 220));
        g.drawString(game.message, 70, 88);
        g.setColor(new Color(112, 65, 31));
        g.fillRoundRect((int) PoolGame.LEFT - 24, (int) PoolGame.TOP - 24,
                (int) (PoolGame.RIGHT - PoolGame.LEFT) + 48, (int) (PoolGame.BOTTOM - PoolGame.TOP) + 48, 30, 30);
        g.setColor(new Color(31, 126, 73));
        g.fillRect((int) PoolGame.LEFT, (int) PoolGame.TOP,
                (int) (PoolGame.RIGHT - PoolGame.LEFT), (int) (PoolGame.BOTTOM - PoolGame.TOP));
        for (double[] pocket : game.pockets()) {
            g.setColor(Color.BLACK);
            g.fillOval((int) pocket[0] - 18, (int) pocket[1] - 18, 36, 36);
        }
        if (game.stopped() && !game.gameOver) drawAim(g);
        for (Ball ball : game.balls) drawBall(g, ball);
        g.dispose();
    }

    private void drawAim(Graphics2D g) {
        Ball cue = cue();
        g.setColor(new Color(255, 255, 255, 150));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double length = 100 + game.shotPower * 300;
        g.drawLine((int) cue.position.getX(), (int) cue.position.getY(),
                (int) (cue.position.getX() + Math.cos(game.aim) * length),
                (int) (cue.position.getY() + Math.sin(game.aim) * length));
    }

    private void drawBall(Graphics2D g, Ball ball) {
        if (!ball.active) return;
        int diameter = (int) PoolGame.BALL_RADIUS * 2;
        int x = (int) Math.round(ball.position.getX() - PoolGame.BALL_RADIUS);
        int y = (int) Math.round(ball.position.getY() - PoolGame.BALL_RADIUS);
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(x + 3, y + 4, diameter, diameter);
        if (ball.number >= 9) {
            g.setColor(Color.WHITE);
            g.fillOval(x, y, diameter, diameter);
            g.setColor(ball.color);
            g.fillRect(x, y + 7, diameter, 12);
        } else {
            g.setColor(ball.color);
            g.fillOval(x, y, diameter, diameter);
        }
        g.setColor(Color.WHITE);
        g.drawOval(x, y, diameter, diameter);
        if (ball.number > 0) {
            g.setColor(ball.number == 8 ? Color.WHITE : Color.BLACK);
            g.fillOval(x + 6, y + 6, 14, 14);
            g.setColor(ball.number == 8 ? Color.BLACK : Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            String label = Integer.toString(ball.number);
            int labelWidth = g.getFontMetrics().stringWidth(label);
            g.drawString(label, (int) ball.position.getX() - labelWidth / 2, (int) ball.position.getY() + 4);
        }
    }
}
