package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class BandageGuy extends GameObject {
    private float posX;
    private float posY;
    private Image image;
    private float previousX;
    private float previousY;
    private float inputX;
    private float inputY;
    private float speed = 5;

    public BandageGuy(String name, String myGameName, float x, float y, String imagePath) {
        super(name, myGameName);
        this.posX = x;
        this.posY = y;
        try {
            image = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            System.err.println("Error loading image: " + e.getMessage());
            image = null;
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (image != null) {
            g2d.drawImage(image, (int) posX, (int) posY, null);
        } else {
            g.drawString("No image loaded", (int) posX, (int) posY);
        }
    }

    @Override
    protected void myUpdateLocal() {
        // Lokale Updates hier implementieren
        posX += inputX*speed;
        posY += inputY*speed;
        if(previousX != posX || previousY != posY) {
            Message moveMsg = new Message(
                    "MOVE",
                    new Object[]{ posX, posY },
                    null
            );
            sendMessage(moveMsg);
        }
        previousX = posX;
        previousY = posY;
    }

    @Override
    protected void myUpdateGlobal(Message msg) {
        // Globale Updates hier implementieren
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params.length >= 2) {
                float newX = (params[0] instanceof Number) ? ((Number) params[0]).floatValue() : posX;
                float newY = (params[1] instanceof Number) ? ((Number) params[1]).floatValue() : posY;
                synchronized (this) {
                    this.posX = newX;
                    this.posY = newY;
                }
                System.out.println("Processed MOVE for " + getName() +
                        " in game " + extractGameName(msg) +
                        ": new position x=" + newX + ", y=" + newY);
            }
        }
    }

    @Override
    public KeyAdapter getKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W: inputY = -1; break;
                    case KeyEvent.VK_S: inputY = 1; break;
                    case KeyEvent.VK_A: inputX = -1; break;
                    case KeyEvent.VK_D: inputX = 1; break;
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W:
                    case KeyEvent.VK_S: inputY = 0; break;
                    case KeyEvent.VK_A:
                    case KeyEvent.VK_D: inputX = 0; break;
                }
            }
        };
    }
}
