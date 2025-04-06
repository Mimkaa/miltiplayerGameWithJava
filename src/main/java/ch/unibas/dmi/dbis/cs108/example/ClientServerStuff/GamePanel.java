package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;

import javax.swing.*;
import java.awt.*;

/**
 * The {@code GamePanel} class is a custom Swing {@link JPanel} that renders an array of
 * {@link GameObject}s and displays a ping indicator in the top-left corner.
 * It provides methods to update the displayed game objects and the current ping value
 * (e.g., network latency or any custom measurement).
 */
public class GamePanel extends JPanel {

    /**
     * An array of {@link GameObject} instances to be drawn on this panel.
     * It is updated via {@link #updateGameObjects(GameObject[])}.
     */
    private GameObject[] gameObjects;

    /**
     * A simple integer value representing a ping indicator, which is displayed
     * in the top-left corner of the panel. It is initialized to a random integer
     * between 0 and 99.
     */
    private int pingIndicator = (int) (Math.random() * 100);

    /**
     * Constructs a {@code GamePanel} with the specified initial array of game objects.
     *
     * @param gameObjects An array of {@link GameObject} instances to be rendered.
     */
    public GamePanel(GameObject[] gameObjects) {
        this.gameObjects = gameObjects;
        setBackground(Color.WHITE);
    }

    /**
     * Updates the array of game objects to be drawn. This method is synchronized to
     * ensure thread safety in case multiple threads update the game objects concurrently.
     * After updating, the panel is repainted to reflect the changes.
     *
     * @param newGameObjects The updated array of game objects to display.
     */
    public synchronized void updateGameObjects(GameObject[] newGameObjects) {
        this.gameObjects = newGameObjects;
        repaint();
    }

    /**
     * Sets a new value for the ping indicator and triggers a repaint to
     * show the updated value. The method is synchronized to maintain consistency
     * during concurrent updates.
     *
     * @param value The new ping indicator value.
     */
    public synchronized void setPingIndicator(int value) {
        this.pingIndicator = value;
        repaint();
    }

    /**
     * Overrides the {@link JPanel#paintComponent(Graphics)} method to draw the updated
     * array of game objects and the current ping indicator. The method is synchronized to
     * ensure consistent drawing if updates occur from multiple threads.
     *
     * @param g The {@link Graphics} context used to draw the panel's content.
     */
    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);

       

        Font originalFont = g.getFont();
        Font bigFont = originalFont.deriveFont(Font.BOLD, originalFont.getSize() * 2.0f);
        g.setFont(bigFont);

        String indicatorText = "Ping: " + pingIndicator;
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        int x = 10;
        int y = metrics.getAscent() + 10;

        g.setColor(Color.BLACK);
        g.drawString(indicatorText, x, y);

        g.setFont(originalFont);
    }
}
