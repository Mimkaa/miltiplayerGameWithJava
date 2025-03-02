package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Square extends GameObject {
    private float x;
    private float y;
    private float radius;
    private float oldRadius; // Track previous radius value.
    private final float inflationRate = 5.0f;  // How much the square inflates per key press.

    /**
     * Constructs a Square game object.
     *
     * @param name       The object's name.
     * @param x          The starting X coordinate.
     * @param y          The starting Y coordinate.
     * @param radius     The initial radius (half the side length).
     * @param myGameName The game (or session) name.
     */
    public Square(String name, float x, float y, float radius, String myGameName) {
        super(name, myGameName);
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.oldRadius = radius;
    }

    /**
     * Returns a KeyAdapter that listens for the space key. When the space key is pressed,
     * the square's radius is increased (inflated). The message sending is handled in myUpdateLocal().
     */
    @Override
    public KeyAdapter getKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    // Inflate the square.
                    radius += inflationRate;
                    
                }
            }
        };
    }

    /**
     * Local update logic: detect changes to the square's radius and send an "INFT" message if it has changed.
     */
    @Override
    protected void myUpdateLocal() {
        if (radius != oldRadius) {
            // Create an "INFT" message with the new radius.
            Message inflateMsg = new Message(
                "INFT",
                new Object[]{ radius },
                null,
                new String[]{ getName(), getGameName() }
            );
            sendMessage(inflateMsg);
            oldRadius = radius;
        }
    }

    /**
     * Global update logic: process an incoming "INFT" message to update the square's radius.
     *
     * @param msg the message to process.
     */
    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("INFT".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params != null && params.length > 0) {
                float newRadius = (params[0] instanceof Number)
                                  ? ((Number) params[0]).floatValue()
                                  : radius;
                this.radius = newRadius;
                System.out.println("Processed INFT for " + getName() +
                                   ", new radius: " + newRadius);
            }
        }
    }

    /**
     * Draws the square on the given Graphics context.
     * The square is drawn in red with its name centered above it.
     *
     * @param g the Graphics context.
     */
    @Override
    public void draw(Graphics g) {
        // Draw the square in red.
        g.setColor(Color.RED);
        int side = (int)(radius * 2);
        g.fillRect((int)(x - radius), (int)(y - radius), side, side);
        
        // Draw the object's name in black above the square.
        g.setColor(Color.BLACK);
        int textWidth = g.getFontMetrics().stringWidth(getName());
        g.drawString(getName(), (int)x - textWidth / 2, (int)(y - radius - 5));
    }
}
