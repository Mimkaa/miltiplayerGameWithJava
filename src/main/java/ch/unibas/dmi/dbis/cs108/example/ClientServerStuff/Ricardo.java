package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;

public class Ricardo extends GameObject {
    
    private float x;
    private float y;
    private Image image;

    // Rotation in degrees.
    private float rotation = 0.0f;
    // To track changes in rotation for messaging.
    private float oldRotation = 0.0f;

    /**
     * Constructs a new Ricardo instance.
     *
     * @param name       The object's name.
     * @param myGameName The game or session name.
     * @param x          Initial x position.
     * @param y          Initial y position.
     * @param imagePath  Path to the image file to load.
     */
    public Ricardo(String name, String myGameName, float x, float y, String imagePath) {
        super(name, myGameName);
        this.x = x;
        this.y = y;
        try {
            image = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            System.err.println("Error loading image: " + e.getMessage());
            image = null;
        }
        oldRotation = rotation;
    }
    
    // Getter for rotation.
    public float getRotation() {
        return rotation;
    }

    /**
     * Local update logic: if the rotation has changed, send a ROTATE message.
     */
    @Override
    protected void myUpdateLocal() {
        // Check if rotation changed.
        if (rotation != oldRotation && messageQueue != null) {
            Message rotateMsg = new Message(
                "ROTATE",
                new Object[]{ rotation },
                null
            );
            sendMessage(rotateMsg);
            oldRotation = rotation;
        }
    }
    
    /**
     * Global update logic: process incoming ROTATE messages.
     */
    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("ROTATE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params.length >= 1) {
                float newRotation = (params[0] instanceof Number)
                                    ? ((Number) params[0]).floatValue() : rotation;
                synchronized (this) {
                    this.rotation = newRotation;
                }
                System.out.println("Processed ROTATE for " + getName() +
                                   ": new rotation = " + newRotation);
            }
        }
    }
    
    /**
     * Returns a KeyAdapter that handles rotation.
     * Pressing the space bar increases the rotation by 15°.
     */
    @Override
    public KeyAdapter getKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    // Increase rotation by 15 degrees.
                    rotation += 15;
                }
            }
        };
    }
    
    /**
     * Draws the image at the current (x, y) position applying the rotation.
     */
    @Override
    public void draw(Graphics g) {
        if (image != null) {
            Graphics2D g2 = (Graphics2D) g;
            // Save the current transform.
            AffineTransform oldTransform = g2.getTransform();
            int imgWidth = image.getWidth(null);
            int imgHeight = image.getHeight(null);
            // Translate to the image center.
            g2.translate(x + imgWidth / 2, y + imgHeight / 2);
            // Rotate by the current rotation (converted to radians).
            g2.rotate(Math.toRadians(rotation));
            // Draw the image centered.
            g2.drawImage(image, -imgWidth / 2, -imgHeight / 2, null);
            // Restore the original transform.
            g2.setTransform(oldTransform);
        } else {
            g.drawString("No image loaded", (int) x, (int) y);
        }
    }
}
