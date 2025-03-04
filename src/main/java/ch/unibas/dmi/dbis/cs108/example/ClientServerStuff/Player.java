package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;


public class Player extends GameObject {
    //private final String name;
    private final float radius;
    private float x;
    private float y;
    private float speed = 5.0f;

    // The "game name" or session ID this player belongs to.
    //private final String myGameName;
    
    // Internal fields for movement input.
    private float inputX = 0;
    private float inputY = 0;
    
    // For tracking position changes.
    private float oldX;
    private float oldY;

    /**
     * @param name        The player's name
     * @param x           Starting X coordinate
     * @param y           Starting Y coordinate
     * @param radius      Radius for drawing
     * @param myGameName  The name (or ID) of the current game/session
     */
    public Player(String name, float x, float y, float radius, String myGameName) {
        super(name, myGameName); // Call GameObject constructor to initialize name and myGameName.
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.oldX = x;
        this.oldY = y;
    }

   
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }

    /**
     * Local update logic: update the player's position based on input.
     * If the position changes, a MOVE message is sent using sendMessage().
     */
    @Override
    protected void myUpdateLocal() {
        // Save the old coordinates.
        oldX = x;
        oldY = y;
        // Update position based on input.
        x += inputX * speed;
        y += inputY * speed;
        
        // If the position has changed, send a MOVE message.
        if ((x != oldX || y != oldY) && messageQueue != null) {
            Message moveMsg = new Message(
                "MOVE",
                new Object[]{ x, y },
                null
            );
            sendMessage(moveMsg);
        }
    }

    /**
     * Global update logic: process an incoming MOVE message to update the player's state.
     */
    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params.length >= 2) {
                float newX = (params[0] instanceof Number) ? ((Number) params[0]).floatValue() : x;
                float newY = (params[1] instanceof Number) ? ((Number) params[1]).floatValue() : y;
                synchronized (this) {
                    this.x = newX;
                    this.y = newY;
                }
                System.out.println("Processed MOVE for " + getName() +
                                   " in game " + extractGameName(msg) +
                                   ": new position x=" + newX + ", y=" + newY);
            }
        }
        // Additional message types can be processed here.
    }

    /**
     * Provides a KeyAdapter that updates the player's input state.
     */
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
    
    /**
     * Draws the player on the given Graphics context.
     */
    @Override
    public void draw(Graphics g) {
        // Draw the player's oval.
        g.fillOval((int)(x - radius), (int)(y - radius), (int)(radius * 2), (int)(radius * 2));
        // Draw the player's name above the oval.
        g.drawString(getName(), (int)x - (g.getFontMetrics().stringWidth(getName()) / 2), (int)(y - radius - 5));
    }
}
