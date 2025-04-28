package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

/**
 * The {@code MovingPlatform} class represents a platform that oscillates
 * between specified start and end positions along both the X and Y axes.
 * <p>
 * Movement is computed based on velocity and direction, and position updates
 * are sent as "MOVE" messages when running in a server context.
 * Rendering is done via JavaFX by drawing a gray rectangle and optionally
 * displaying the platform's name.
 * </p>
 */
@Getter
@Setter
public class MovingPlatform extends GameObject implements IMovable {

    // Bounding box fields for collision detection.
    private float x;
    private float y;
    private float width;
    private float height;

    // Oscillation parameters for the horizontal movement.
    private float startX;
    private float endX;
    private float periodX; // period in seconds for x oscillation

    // Oscillation parameters for the vertical movement.
    private float startY;
    private float endY;
    private float periodY; // period in seconds for y oscillation

    // Time tracking for movement.
    private long lastUpdateNano;
    private float elapsedTime = 0;

    /**
     * Constructs a MovingPlatform that oscillates between given bounds.
     *
     * @param name    the platform's display name
     * @param startX  the minimum x-coordinate
     * @param endX    the maximum x-coordinate
     * @param startY  the minimum y-coordinate
     * @param endY    the maximum y-coordinate
     * @param width   the platform's width
     * @param height  the platform's height
     * @param periodX the horizontal oscillation period in seconds
     * @param periodY the vertical oscillation period in seconds
     * @param gameId  the ID of the game session this platform belongs to
     */
    public MovingPlatform(String name,
                          float startX, float endX,
                          float startY, float endY,
                          float width, float height,
                          float periodX, float periodY,
                          String gameId) {
        super(name, gameId);
        this.startX = startX;
        this.endX = endX;
        this.startY = startY;
        this.endY = endY;
        this.width = width;
        this.height = height;
        this.periodX = periodX;
        this.periodY = periodY;
        this.x = startX;
        this.y = startY;
        this.lastUpdateNano = System.nanoTime();
    }

    private float velocityX = 100f; // units per second
    private float velocityY = 100f;
    private int directionX = 1;     // +1 or -1
    private int directionY = 1;

    /**
     * Moves the platform according to its velocity and direction,
     * reversing direction when reaching bounds, and sends a MOVE message
     * if running on the server.
     *
     * @param deltaTime time elapsed since the last update, in seconds
     */
    @Override
    public void move(float deltaTime) {
        float newX = x + velocityX * directionX * deltaTime;
        float newY = y + velocityY * directionY * deltaTime;

        if (newX < startX) {
            newX = startX;
            directionX = 1;
        } else if (newX > endX) {
            newX = endX;
            directionX = -1;
        }

        if (newY < startY) {
            newY = startY;
            directionY = 1;
        } else if (newY > endY) {
            newY = endY;
            directionY = -1;
        }

        setX(newX);
        setY(newY);

        if (isServerInstance()) {
            Message moveMsg = new Message("MOVE", new Object[]{getX(), getY()}, null);
            sendMessage(moveMsg);
        }
    }

    /**
     * Determines if this instance is running on the server (no JavaFX thread).
     *
     * @return true if server-side, false if client-side
     */
    private boolean isServerInstance() {
        return !Thread.currentThread().getName().contains("JavaFX");
    }

    /**
     * Local update called each frame; computes deltaTime and moves platform
     * when running on the server.
     */
    @Override
    public void myUpdateLocal() {
        if (isServerInstance()) {
            long now = System.nanoTime();
            float deltaTime = (now - lastUpdateNano) / 1_000_000_000f;
            lastUpdateNano = now;
            move(deltaTime);
        }
    }

    /**
     * Stub for time-based updates; not implemented.
     *
     * @param deltaTime time since last update, in seconds
     */
    @Override
    public void myUpdateLocal(float deltaTime) {
    }

    /**
     * Applies a global update based on a received Message.
     * Handles MOVE messages to set the platform's position.
     *
     * @param msg the received Message with type "MOVE" and parameters [ newX, newY ]
     */
    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            System.out.println("MovingPlatform MOVE message parameters: " + Arrays.toString(params));
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
                System.out.println("Processed MOVE for platform " + getName() +
                        ": new position x=" + newX + ", y=" + newY);
            }
        }
    }

    /**
     * Renders the platform as a gray rectangle and its name above it.
     *
     * @param gc the JavaFX GraphicsContext to draw on
     */
    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.GRAY);
        gc.fillRect(getX(), getY(), getWidth(), getHeight());
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        text.setFont(gc.getFont());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), getX() + getWidth() / 2 - textWidth / 2, getY() - 5);
    }

    /**
     * Creates a "SNAPSHOT" Message containing the current position.
     * concealedParameters are set to this object's ID and gameId.
     *
     * @return a Message of type "SNAPSHOT" with parameters [ x, y ]
     */
    @Override
    public Message createSnapshot() {
        Object[] params = new Object[]{ x, y };
        Message snapshotMsg = new Message("SNAPSHOT", params, "GAME");
        snapshotMsg.setConcealedParameters(new String[]{ getId(), getGameId() });
        return snapshotMsg;
    }

    /**
     * Returns the constructor parameters in the same order as the signature.
     *
     * @return an Object array of constructor arguments
     */
    @Override
    public Object[] getConstructorParamValues() {
        return new Object[]{ getName(), startX, endX, startY, endY, getWidth(), getHeight(), periodX, periodY, getGameId() };
    }
}
