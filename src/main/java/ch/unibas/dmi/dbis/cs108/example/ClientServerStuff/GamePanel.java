package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private Player[] players;

    public GamePanel(Player[] players) {
        this.players = players;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        // Draw each player as a circle with the player's name above.
        for (Player p : players) {
            int x = (int) p.getX();
            int y = (int) p.getY();
            int r = (int) p.getRadius();
            g2.setColor(Color.BLUE);
            // Draw circle centered at (x,y)
            g2.drawOval(x - r, y - r, 2 * r, 2 * r);
            // Draw the player's name above the circle
            g2.drawString(p.getName(), x - r, y - r - 5);
        }
    }
}
