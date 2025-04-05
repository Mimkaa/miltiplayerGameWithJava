package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

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
     * Constructs a MovingPlatform game object that oscillates in both x and y.
     *
     * @param name    The platform's name.
     * @param startX  The left-most x position.
     * @param endX    The right-most x position.
     * @param startY  The top-most y position.
     * @param endY    The bottom-most y position.
     * @param width   The width of the platform.
     * @param height  The height of the platform.
     * @param periodX The period (in seconds) for a full horizontal oscillation cycle.
     * @param periodY The period (in seconds) for a full vertical oscillation cycle.
     * @param gameId  The game session ID.
     */
    public MovingPlatform(String name, float startX, float endX, float startY, float endY, float width, float height, float periodX, float periodY, String gameId) {
        super(name, gameId);
        this.startX = startX;
        this.endX = endX;
        this.startY = startY;
        this.endY = endY;
        this.width = width;
        this.height = height;
        this.periodX = periodX;
        this.periodY = periodY;
        // Initialize current position to the starting positions.
        this.x = startX;
        this.y = startY;
        // Initialize time tracking.
        this.lastUpdateNano = System.nanoTime();
    }


    private float velocityX = 100f; // units per second
    private float velocityY = 100f;

    private int directionX = 1; // +1 or -1
    private int directionY = 1;

    /**
     * Implements the movement behavior.
     * Uses deltaTime to update the accumulated elapsed time and then
     * computes new positions using cosine interpolation.
     *
     * @param deltaTime the time in seconds since the last update.
     */
    @Override
    public void move(float deltaTime) {
        // Berechne neue Position
        float newX = x + velocityX * directionX * deltaTime;
        float newY = y + velocityY * directionY * deltaTime;

        // Kollision mit Begrenzung (links/rechts)
        if (newX < startX) {
            newX = startX;
            directionX = 1;
        } else if (newX > endX) {
            newX = endX;
            directionX = -1;
        }

        // Kollision mit Begrenzung (oben/unten)
        if (newY < startY) {
            newY = startY;
            directionY = 1;
        } else if (newY > endY) {
            newY = endY;
            directionY = -1;
        }

        setX(newX);
        setY(newY);

        // Nur auf dem Server senden
        Message moveMsg = new Message("MOVE", new Object[]{getX(), getY()}, null);
        sendMessage(moveMsg);
    }

    private boolean isServerInstance() {
        // Trick: Auf dem Server läuft kein JavaFX, daher kein FX Application Thread.
        return !Thread.currentThread().getName().contains("JavaFX");
    }

    /**
     * Local update method that computes the deltaTime and then calls move.
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

    @Override
    public void myUpdateLocal(float deltaTime) {

    }

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
        // Return parameters in the same order as the constructor.
        return new Object[]{ getName(), startX, endX, startY, endY, getWidth(), getHeight(), periodX, periodY, getGameId() };
    }
}
