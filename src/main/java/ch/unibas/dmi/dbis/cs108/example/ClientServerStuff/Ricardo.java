package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;


import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.Arrays;

public class Ricardo extends GameObject {

    private double x;
    private double y;
    private Image image;
    private String imagePath;

    // Rotation in degrees.
    private double rotation = 0.0;
    private double oldRotation = 0.0;

    /**
     * Constructs a new Ricardo instance.
     *
     * @param name       The object's name.
     * @param myGameId   The unique game session ID.
     * @param x          Initial x position.
     * @param y          Initial y position.
     * @param imagePath  The path to the image file to load.
     */
    public Ricardo(String name, String myGameId, double x, double y, String imagePath) {
        super(name, myGameId);
        this.x = x;
        this.y = y;
        this.imagePath = imagePath;
        try {
            // Load the image using JavaFX; prepend "file:" to load from a file.
            image = new Image("file:" + imagePath);
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
            image = null;
        }
        oldRotation = rotation;
    }

    /**
     * Local update logic: if the rotation has changed, send a ROTATE message.
     */
    @Override
    protected void myUpdateLocal() {
        if (rotation != oldRotation) {
            Message rotateMsg = new Message("ROTATE", new Object[]{rotation}, null);
            sendMessage(rotateMsg);
            oldRotation = rotation;
        }
    }

    /**
     * Global update logic: process incoming ROTATE messages.
     *
     * @param msg The message to process.
     */
    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("ROTATE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            System.out.println("ROTATE message parameters: " + Arrays.toString(params));
            if (params.length >= 1) {
                double newRotation = (params[0] instanceof Number)
                                     ? ((Number) params[0]).doubleValue()
                                     : rotation;
                synchronized (this) {
                    this.rotation = newRotation;
                }
                System.out.println("Processed ROTATE for " + getName() + ": new rotation = " + newRotation);
            }
        }
    }

    /**
     * Draws the Ricardo object using the provided JavaFX GraphicsContext.
     *
     * @param gc The GraphicsContext used for drawing.
     */
    @Override
    public void draw(GraphicsContext gc) {
        if (image != null) {
            // Save the current state.
            gc.save();
            double imgWidth = image.getWidth();
            double imgHeight = image.getHeight();
            // Translate so that (x, y) is the top-left corner.
            // We translate to the center of the image.
            gc.translate(x + imgWidth / 2, y + imgHeight / 2);
            // Rotate around the center.
            gc.rotate(rotation); // rotation is in degrees
            // Draw the image centered at (0,0).
            gc.drawImage(image, -imgWidth / 2, -imgHeight / 2);
            // Restore the original state.
            gc.restore();
        } else {
            gc.setFill(Color.BLACK);
            gc.fillText("No image loaded", x, y);
        }
    }

    /**
     * Returns the constructor parameters in the order: (name, myGameId, x, y, imagePath).
     */
    @Override
    public Object[] getConstructorParamValues() {
        return new Object[] { getName(), getGameId(), x, y, imagePath };
    }
}
