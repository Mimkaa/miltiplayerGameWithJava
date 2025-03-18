package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private GameObject[] gameObjects;
    // New variable: pingIndicator initialized to a random int between 0 and 99.
    private int pingIndicator = (int) (Math.random() * 100);

    public GamePanel(GameObject[] gameObjects) {
        this.gameObjects = gameObjects;
        setBackground(Color.WHITE);
    }
    
    /**
     * Synchronized update of the game objects array.
     * @param newGameObjects the new array of game objects to be displayed.
     */
    public synchronized void updateGameObjects(GameObject[] newGameObjects) {
        this.gameObjects = newGameObjects;
        repaint();
    }

    /**
     * Sets the ping indicator value and repaints the panel.
     * @param value the new value for the ping indicator.
     */
    public synchronized void setPingIndicator(int value) {
        this.pingIndicator = value;
        repaint();
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw each game object.
        for (GameObject obj : gameObjects) {
            obj.draw(g);
        }
        
        // Set a larger bold font for the ping indicator.
        Font originalFont = g.getFont();
        Font bigFont = originalFont.deriveFont(Font.BOLD, originalFont.getSize() * 2.0f);
        g.setFont(bigFont);
        
        // Draw pingIndicator at the top left corner.
        String indicatorText = "Ping: " + pingIndicator;
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        int x = 10; // 10 pixels margin from the left edge.
        int y = metrics.getAscent() + 10; // 10 pixels margin from the top.
        
        g.setColor(Color.BLACK);
        g.drawString(indicatorText, x, y);
        
        // Optionally, reset the font to the original font.
        g.setFont(originalFont);
    }
}
