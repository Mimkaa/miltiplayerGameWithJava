package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;


import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.Arrays;

public class BandageGuy extends GameObject {

    // Position and movement fields.
    private double posX;
    private double posY;
    private double previousX;
    private double previousY;
    private double inputX = 0;
    private double inputY = 0;
    private double speed = 5.0;

    // Image fields.
    private Image image;
    private String imagePath; // Save the image path for later retrieval via getConstructorParamValues().

    /**
     * Constructs a BandageGuy game object.
     *
     * @param name      The object's name.
     * @param myGameId  The unique game session ID.
     * @param x         The starting X coordinate.
     * @param y         The starting Y coordinate.
     * @param imagePath The file system path to the image.
     */
    public BandageGuy(String name, String myGameId, double x, double y, String imagePath) {
        super(name, myGameId);
        this.posX = x;
        this.posY = y;
        this.previousX = x;
        this.previousY = y;
        this.imagePath = imagePath;
        try {
            // In JavaFX, to load an image from a file, prepend "file:" to the file path.
            image = new Image("file:" + imagePath);
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
            image = null;
        }
    }

    /**
     * Returns the constructor parameter values in the order:
     * (String name, String myGameId, double x, double y, String imagePath).
     */
    @Override
    public Object[] getConstructorParamValues() {
        return new Object[] { getName(), getGameId(), posX, posY, imagePath };
    }

    /**
     * Draws the BandageGuy on the given JavaFX GraphicsContext.
     *
     * @param gc The GraphicsContext used for drawing.
     */
    @Override
    public void draw(GraphicsContext gc) {
        if (image != null) {
            gc.drawImage(image, posX, posY);
        } else {
            gc.setFill(Color.BLACK);
            gc.fillText("No image loaded", posX, posY);
        }
    }

    /**
     * Local update logic: update position based on input and send a "MOVE" message if changed.
     */
    @Override
    protected void myUpdateLocal() {
        posX += inputX * speed;
        posY += inputY * speed;
        if (previousX != posX || previousY != posY) {
            Message moveMsg = new Message("MOVE", new Object[] { posX, posY }, null);
            sendMessage(moveMsg);
        }
        previousX = posX;
        previousY = posY;
    }

    /**
     * Global update logic: process an incoming "MOVE" message to update position.
     *
     * @param msg The incoming message.
     */
    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            System.out.println("MOVE message parameters: " + Arrays.toString(params));
            if (params.length >= 2) {
                double newX = (params[0] instanceof Number)
                              ? ((Number) params[0]).doubleValue()
                              : Double.parseDouble(params[0].toString());
                double newY = (params[1] instanceof Number)
                              ? ((Number) params[1]).doubleValue()
                              : Double.parseDouble(params[1].toString());
                synchronized (this) {
                    this.posX = newX;
                    this.posY = newY;
                }
                System.out.println("Processed MOVE for " + getName() +
                                   " in game " + extractGameId(msg) +
                                   ": new position x=" + newX + ", y=" + newY);
            }
        }
    }
    
    // Note: Key event handling is typically done by setting event handlers on the Scene or Node in JavaFX,
    // so we remove the AWT KeyAdapter implementation.
}
