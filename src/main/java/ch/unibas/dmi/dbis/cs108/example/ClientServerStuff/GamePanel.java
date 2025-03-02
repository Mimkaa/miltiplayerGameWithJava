package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private final GameObject[] gameObjects;

    public GamePanel(GameObject[] gameObjects) {
        this.gameObjects = gameObjects;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw each game object.
        for (GameObject obj : gameObjects) {
            obj.draw(g);
        }
    }
}
