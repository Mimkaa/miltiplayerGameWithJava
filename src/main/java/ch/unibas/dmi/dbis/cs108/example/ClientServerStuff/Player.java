package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;

public class Player extends GameObject {

    // 1) Fields that match the constructor parameters exactly:
    private float x;       // matches "float x" in the constructor
    private float y;       // matches "float y"
    private final float radius;  // matches "float radius"

    // 2) Additional fields that are NOT constructor params:
    private float oldX;
    private float oldY;
    private float speed = 5.0f;  
    private float inputX = 0;    // for keyboard movement
    private float inputY = 0;

    /**
     * Main constructor for Player, with param names matching the fields (for reflection).
     *
     * @param name        The player's name (sent up to GameObject)
     * @param x           Starting X coordinate
     * @param y           Starting Y coordinate
     * @param radius      Radius for drawing
     * @param myGameName  The name (or ID) of the current game/session (sent up to GameObject)
     */
    public Player(String name, float x, float y, float radius, String myGameName) {
        super(name, myGameName); // Call the parent constructor, sets parent fields: this.name, this.myGameName
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.oldX = x;  // track old position initially as the same as starting pos
        this.oldY = y;
    }

    /**
     * Local update logic: update the player's position based on input.
     * If the position changes, send a MOVE message.
     */
    @Override
    protected void myUpdateLocal() {
        // Save the old coordinates.
        oldX = x;
        oldY = y;

        // Update position based on input (WASD).
        x += inputX * speed;
        y += inputY * speed;

        // If the position changed, send a MOVE message to the server / others.
        if (x != oldX || y != oldY) {
            Message moveMsg = new Message("MOVE", new Object[] { x, y }, null);
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
            System.out.println("MOVE message parameters: " + Arrays.toString(params));
            if (params.length >= 2) {
                float newX = (params[0] instanceof Number)
                             ? ((Number) params[0]).floatValue()
                             : Float.parseFloat(params[0].toString());
                float newY = (params[1] instanceof Number)
                             ? ((Number) params[1]).floatValue()
                             : Float.parseFloat(params[1].toString());
                synchronized (this) {
                    this.x = newX;
                    this.y = newY;
                }
                System.out.println("Processed MOVE for " + getName()
                        + " in game " + extractGameName(msg)
                        + ": new position x=" + newX + ", y=" + newY);
            }
        }
        // Add handling for other message types if needed.
    }

    /**
     * Draws the player as a circle and its name label.
     */
    @Override
    public void draw(Graphics g) {
        // Draw the player's oval.
        int drawX = (int) (x - radius);
        int drawY = (int) (y - radius);
        int size = (int) (radius * 2);
        g.fillOval(drawX, drawY, size, size);

        // Draw the player's name above the oval.
        String nameToDraw = getName(); // the parent's "name" field, inherited
        int nameWidth = g.getFontMetrics().stringWidth(nameToDraw);
        int textX = (int) x - (nameWidth / 2);
        int textY = (int) (y - radius - 5);
        g.drawString(nameToDraw, textX, textY);
    }

    /**
     * An override that sets a new name in the parent field.
     */
    public void setName(String newName) {
        System.out.println(getName() + " is now " + newName);
        super.name = newName; // Directly modify the 'name' field inherited from GameObject
    }

    /**
     * Provides a KeyAdapter that updates the player's input state for WASD movement.
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

    public Object[] getConstructorParamValues() {
        return new Object[] {
            getName(),   
            x,           
            y,           
            radius,      
            getGameName()
        };
    }

    
    public float getX() { return x; }
    public float getY() { return y; }
    public float getRadius() { return radius; }
    public float getOldX() { return oldX; }
    public float getOldY() { return oldY; }
    public float getSpeed() { return speed; }
}
