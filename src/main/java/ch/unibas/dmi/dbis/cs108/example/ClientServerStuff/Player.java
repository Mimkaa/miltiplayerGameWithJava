package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;
import java.util.Arrays;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

/**
 * The {@code Player} class demonstrates a simple moveable game object that
 * sends position updates ("MOVE" messages) when it moves, and receives
 * position updates from other instances via {@link #myUpdateGlobal(Message)}.
 */
public class Player extends GameObject {

    // Fields corresponding to constructor parameters:
    private float x;
    private float y;
    private final float radius;

    // Additional fields:
    private float oldX;
    private float oldY;
    private float speed = 5.0f;
    private float inputX = 0;
    private float inputY = 0;

    /**
     * Main constructor for Player.
     *
     * @param name       The player's name.
     * @param x          Starting X coordinate.
     * @param y          Starting Y coordinate.
     * @param radius     Radius for drawing.
     * @param myGameId   The unique ID of the game session.
     */
    public Player(String name, float x, float y, float radius, String myGameId) {
        super(name, myGameId);
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.oldX = x;
        this.oldY = y;
    }

    /**
     * Local update logic: checks input (keyboard + any local inputX/inputY),
     * updates position, and if position changed, sends a MOVE message.
     */
    @Override
    public void myUpdateLocal() {
        // Save old coordinates:
        oldX = x;
        oldY = y;

        // Movement from "inputX"/"inputY" if you’re using them for local control:
        x += inputX * speed;
        y += inputY * speed;

        // Also handle direct keyboard input (WASD), but only if this is the selected object:
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

        // If the position changed, send a MOVE message so others get updated:
        if (x != oldX || y != oldY) {
            Message moveMsg = new Message("MOVE", new Object[]{x, y}, null);
            sendMessage(moveMsg);
        }
    }

    /**
     * Global update logic: processes incoming MOVE messages to sync this player's position.
     */
    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            System.out.println("MOVE message parameters: " + Arrays.toString(params));
            if (params.length >= 2) {
                float newX = asFloat(params[0]);
                float newY = asFloat(params[1]);
                synchronized (this) {
                    this.x = newX;
                    this.y = newY;
                }
                System.out.println("Processed MOVE for " + getName()
                        + " in game " + extractGameId(msg)
                        + ": new position x=" + newX + ", y=" + newY);
            }
        }
    }

    /**
     * Utility to safely convert an Object param to float.
     */
    private float asFloat(Object param) {
        if (param instanceof Number) {
            return ((Number)param).floatValue();
        } else {
            return Float.parseFloat(param.toString());
        }
    }

    /**
     * Draws the player as a blue circle, and labels with the player's name.
     */
    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.BLUE);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        text.setFont(gc.getFont());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), x - textWidth / 2, y - radius - 5);
    }

    /**
     * Returns constructor params in the same order as your Player(...) constructor.
     */
    @Override
    public Object[] getConstructorParamValues() {
        return new Object[] { getName(), x, y, radius, getGameId() };
    }
}

