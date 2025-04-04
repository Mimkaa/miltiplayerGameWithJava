package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;
import java.util.Arrays;

@Getter
@Setter
public class Player extends GameObject {

    // Fields used for the bounding box (collision detection) – declared as float.
    private float x;
    private float y;
    private float width;
    private float height;

    // Additional fields for movement.
    private float speed = 5.0f;
    private float vx = 2.0f;
    private float vy = 1.0f;

    // For visual feedback: if a collision occurs, this flag is set.
    private boolean collisionDetected = false;

    /**
     * Overloaded constructor that accepts five parameters.
     * This assumes the provided side length is used for both width and height.
     *
     * @param name   The player's name.
     * @param x      Starting x coordinate (as double).
     * @param y      Starting y coordinate (as double).
     * @param side   The side length for both width and height (as double).
     * @param gameId The game session ID.
     */
    public Player(String name, double x, double y, double side, String gameId) {
        // Delegate to the main constructor with width and height equal to side.
        this(name, (float)x, (float)y, (float)side, (float)side, gameId);
    }

    /**
     * Main constructor that accepts six parameters (all numeric values as float).
     *
     * @param name   The player's name.
     * @param x      Starting x coordinate (as float).
     * @param y      Starting y coordinate (as float).
     * @param width  Rectangle width.
     * @param height Rectangle height.
     * @param gameId The game session ID.
     */
    public Player(String name, float x, float y, float width, float height, String gameId) {
        super(name, gameId);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void myUpdateLocal() {
        // Save old position
        float oldX = getX();
        float oldY = getY();

        // Only update position based on keyboard input
        if (this.getId().equals(GameContext.getSelectedGameObjectId())) {
            if (KeyboardState.isKeyPressed(KeyCode.W)) { setY(getY() - speed); }
            if (KeyboardState.isKeyPressed(KeyCode.S)) { setY(getY() + speed); }
            if (KeyboardState.isKeyPressed(KeyCode.A)) { setX(getX() - speed); }
            if (KeyboardState.isKeyPressed(KeyCode.D)) { setX(getX() + speed); }
        }

        // If the position has changed, send a MOVE message.
        if (getX() != oldX || getY() != oldY) {
            Message moveMsg = new Message("MOVE", new Object[]{getX(), getY()}, null);
            sendMessage(moveMsg);
        }
    }

    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            System.out.println("Player MOVE message parameters: " + Arrays.toString(params));
            if (params.length >= 2) {
                float newX = (params[0] instanceof Number)
                        ? ((Number) params[0]).floatValue()
                        : Float.parseFloat(params[0].toString());
                float newY = (params[1] instanceof Number)
                        ? ((Number) params[1]).floatValue()
                        : Float.parseFloat(params[1].toString());
                synchronized (this) {
                    setX(newX);
                    setY(newY);
                }
                System.out.println("Processed MOVE for " + getName() +
                        " in game " + extractGameId(msg) +
                        ": new position x=" + newX + ", y=" + newY);
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Draw a rectangle (with collision feedback).
        if (isCollisionDetected()) {
            gc.setFill(Color.GREEN);
        } else {
            gc.setFill(Color.BLUE);
        }
        gc.fillRect(getX(), getY(), getWidth(), getHeight());

        // Draw the object's name above the rectangle.
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        text.setFont(gc.getFont());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), getX() + getWidth() / 2 - textWidth / 2, getY() - 5);
    }

    @Override
    public Object[] getConstructorParamValues() {
        return new Object[]{ getName(), getX(), getY(), getWidth(), getHeight(), getGameId() };
    }
}
