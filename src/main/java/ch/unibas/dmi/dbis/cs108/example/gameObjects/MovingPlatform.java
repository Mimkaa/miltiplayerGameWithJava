package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.GameObject;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovingPlatform extends GameObject {

    // Bounding box fields (using float for collision detection)
    private float x;
    private float y;
    private float width;
    private float height;

    // Store the starting x position to oscillate around it.
    private float initialX;
    // Amplitude of oscillation (max displacement from the initial x position)
    private float amplitude;
    // Horizontal velocity (units per second) – positive moves right, negative moves left.
    private float vx;

    // Field to track the last update time for delta time calculations.
    private long lastUpdateTime;

    /**
     * Constructs a MovingPlatform game object.
     *
     * @param name      The platform's name.
     * @param x         Starting x coordinate.
     * @param y         Starting y coordinate.
     * @param width     Width of the platform.
     * @param height    Height of the platform.
     * @param amplitude Maximum displacement from the initial x position.
     * @param speed     The speed of movement (units per second).
     * @param gameId    The game session ID.
     */
    public MovingPlatform(String name, float x, float y, float width, float height, float amplitude, float speed, String gameId) {
        super(name, gameId);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.initialX = x;
        this.amplitude = amplitude;
        // Set initial horizontal velocity to speed (moving right).
        this.vx = speed;
        // Initialize the last update time.
        this.lastUpdateTime = System.nanoTime();
    }

    @Override
    public void myUpdateLocal() {
        // Calculate delta time in seconds.
        long now = System.nanoTime();
        float dt = (now - lastUpdateTime) / 1_000_000_000f;
        lastUpdateTime = now;

        // Update horizontal position using delta time.
        setX(getX() + vx * dt);

        // Reverse direction if we exceed the oscillation bounds.
        if (getX() > initialX + amplitude) {
            setX(initialX + amplitude);
            vx = -vx;
        } else if (getX() < initialX - amplitude) {
            setX(initialX - amplitude);
            vx = -vx;
        }

        // Send a MOVE message so other clients are updated.
        Message moveMsg = new Message("MOVE", new Object[]{getX(), getY()}, null);
        sendMessage(moveMsg);
    }

    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
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
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Draw the platform as a gray rectangle.
        gc.setFill(Color.GRAY);
        gc.fillRect(getX(), getY(), getWidth(), getHeight());

        // Optionally, draw the platform's name above it.
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        text.setFont(gc.getFont());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), getX() + getWidth() / 2 - textWidth / 2, getY() - 5);
    }

    @Override
    public Object[] getConstructorParamValues() {
        // For re-creation via messages, we pass name, x, y, width, height, amplitude, speed, gameId.
        return new Object[]{ getName(), getX(), getY(), getWidth(), getHeight(), amplitude, Math.abs(vx), getGameId() };
    }
}
