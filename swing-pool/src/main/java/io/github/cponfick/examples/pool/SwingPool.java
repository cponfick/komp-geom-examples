package io.github.cponfick.examples.pool;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Dimension;

/** Application entry point for the Komp-Geom Swing pool example. */
public final class SwingPool {
    private static final int WIDTH = 980;
    private static final int HEIGHT = 640;

    private SwingPool() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PoolGame game = new PoolGame();
            PoolPanel panel = new PoolPanel(game);
            JFrame frame = new JFrame("Komp-Geom Pool");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(panel);
            frame.setSize(WIDTH, HEIGHT);
            frame.setMinimumSize(new Dimension(WIDTH, HEIGHT));
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.requestFocusInWindow();
            new Timer(16, event -> {
                game.update(1.0 / 60.0);
                panel.repaint();
            }).start();
        });
    }
}
