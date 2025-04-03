package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
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
    
    // New fields for keyboard-controlled movement:
    private double speed = 5.0;
    // Optionally, you could track old positions if needed
    // but here we use local temporary variables in myUpdateLocal()

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
     * Local update logic: processes keyboard input for movement and rotation.
     * If the Ricardo object is selected, it reads WASD keys to update its position.
     * If the rotation has changed, a ROTATE message is sent.
     */
    @Override
    public void myUpdateLocal() {
        // Save current position
        double oldX = x;
        double oldY = y;

        // Process keyboard input for movement if this object is selected.
        if (this.getId().equals(GameContext.getSelectedGameObjectId())) {
            if (KeyboardState.isKeyPressed(KeyCode.W)) {
                y -= speed;
            }
            if (KeyboardState.isKeyPressed(KeyCode.S)) {
                y += speed;
            }
            if (KeyboardState.isKeyPressed(KeyCode.A)) {
                x -= speed;
            }
            if (KeyboardState.isKeyPressed(KeyCode.D)) {
                x += speed;
            }
        }

        // If the position changed, send a MOVE message so others get updated.
        if (x != oldX || y != oldY) {
            Message moveMsg = new Message("MOVE", new Object[]{x, y}, null);
            sendMessage(moveMsg);
        }

        // Process rotation changes (as before).
        if (rotation != oldRotation) {
            Message rotateMsg = new Message("ROTATE", new Object[]{rotation}, null);
            sendMessage(rotateMsg);
            oldRotation = rotation;
        }
    }

    /**
     * Global update logic: processes incoming ROTATE and MOVE messages.
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
        } else if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            System.out.println("MOVE message parameters: " + Arrays.toString(params));
            if (params.length >= 2) {
                double newX = (params[0] instanceof Number)
                        ? ((Number) params[0]).doubleValue()
                        : x;
                double newY = (params[1] instanceof Number)
                        ? ((Number) params[1]).doubleValue()
                        : y;
                synchronized (this) {
                    this.x = newX;
                    this.y = newY;
                }
                System.out.println("Processed MOVE for " + getName() + ": new position x=" + newX + ", y=" + newY);
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
        return new Object[]{getName(), getGameId(), x, y, imagePath};
    }
}
