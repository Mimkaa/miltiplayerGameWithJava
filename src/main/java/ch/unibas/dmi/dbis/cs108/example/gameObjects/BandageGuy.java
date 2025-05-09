package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;

/**
 * The {@code BandageGuy} class represents a movable game object with an image,
 * extending {@link GameObject}. It handles local updates (movement) and
 * synchronizes its position via messages across the network.
 * <p>
 * This object responds to MOVE messages and supports rendering via JavaFX.
 * </p>
 */
@Getter
@Setter
public class BandageGuy extends GameObject {

    // Use float for bounding box fields.
    private float x;
    private float y;
    private float width;
    private float height;

    // Fields for movement.
    private float previousX;
    private float previousY;
    private float inputX = 0;
    private float inputY = 0;
    private float speed = 5.0f;

    // Image fields.
    private Image image;
    private String imagePath; // File path to the image.

    /**
     * Constructs a BandageGuy game object with image and initial position.
     *
     * @param name      the display name of the object
     * @param gameId    the ID of the game session this object belongs to
     * @param x         the initial x-coordinate
     * @param y         the initial y-coordinate
     * @param imagePath the path to the image file representing the object
     */
    public BandageGuy(String name, String gameId, float x, float y, String imagePath) {
        super(name, gameId);
        this.x = x;
        this.y = y;
        this.previousX = x;
        this.previousY = y;
        this.imagePath = imagePath;
        try {
            image = new Image("file:" + imagePath);
            if (image.getWidth() > 0 && image.getHeight() > 0) {
                this.width = (float) image.getWidth();
                this.height = (float) image.getHeight();
            } else {
                this.width = 50;
                this.height = 50;
            }
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
            image = null;
            this.width = 50;
            this.height = 50;
        }
    }

    @Override
    public Object[] getConstructorParamValues() {
        return new Object[]{ getName(), getGameId(), x, y, imagePath };
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (image != null) {
            gc.drawImage(image, x, y, width, height);
        } else {
            gc.setFill(Color.BLACK);
            gc.fillText("No image loaded", x, y);
        }
    }

    @Override
    public void myUpdateLocal() {
        x += inputX * speed;
        y += inputY * speed;
        if (previousX != x || previousY != y) {
            Message moveMsg = new Message("MOVE", new Object[]{x, y}, null);
            sendMessage(moveMsg);
        }
        previousX = x;
        previousY = y;
    }

    @Override
    public void myUpdateLocal(float deltaTime) {

    }

    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params.length >= 2) {
                float newX = (params[0] instanceof Number) ? ((Number) params[0]).floatValue() : Float.parseFloat(params[0].toString());
                float newY = (params[1] instanceof Number) ? ((Number) params[1]).floatValue() : Float.parseFloat(params[1].toString());
                synchronized (this) {
                    this.x = newX;
                    this.y = newY;
                }
            }
        }
    }

    // Bounding box methods.
    @Override
    public float getX() { return this.x; }
    @Override
    public float getY() { return this.y; }
    @Override
    public float getWidth() { return this.width; }
    @Override
    public float getHeight() { return this.height; }
    @Override
    public void setX(float x) { this.x = x; }
    @Override
    public void setY(float y) { this.y = y; }
    @Override
    public void setWidth(float width) { this.width = width; }
    @Override
    public void setHeight(float height) { this.height = height; }
    @Override
    public Message createSnapshot() {
        // Pack the position, velocity, and acceleration into an Object array.
        Object[] params = new Object[]{
            x, y,   // Positionvelocity.x, velocity.y,   // Velocity
            
        };
        // Create a new message with type "SNAPSHOT" and an appropriate option (e.g., "UPDATE").
        Message snapshotMsg = new Message("SNAPSHOT", params, "GAME");
        
        // Set the concealed parameters so receivers know the source of the snapshot.
        snapshotMsg.setConcealedParameters(new String[]{ getId(), getGameId() });
        
        return snapshotMsg;
    }
}
