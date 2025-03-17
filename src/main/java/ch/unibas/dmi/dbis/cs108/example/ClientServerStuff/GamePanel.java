package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private GameObject[] gameObjects;

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

    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw each game object.
        for (GameObject obj : gameObjects) {
            obj.draw(g);
        }
    }
}
