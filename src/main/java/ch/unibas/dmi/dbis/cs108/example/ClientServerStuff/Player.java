package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;
import java.util.Arrays;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class Player extends GameObject {

    // Fields corresponding to constructor parameters.
    private float x;       
    private float y;       
    private final float radius;  

    // Additional fields.
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
     * Local update logic: update the player's position based on input.
     * If the position changes, sends a MOVE message.
     */
    @Override
    protected void myUpdateLocal() {
        // Save the old coordinates.
        oldX = x;
        oldY = y;

        // Update using inputX and inputY (if you update these externally).
        x += inputX * speed;
        y += inputY * speed;

        // Also update based on the global KeyboardState.
        updateFromKeyboard();

        // Now, if the position has changed, send a MOVE message.
        if (x != oldX || y != oldY) {
            Message moveMsg = new Message("MOVE", new Object[]{x, y}, null);
            sendMessage(moveMsg);
        }
    }

    /**
     * Checks the global keyboard state and updates the player's position.
     */
    private void updateFromKeyboard() {
        // Only process keyboard input if this player's ID matches the selected one.
        if (!this.getId().equals(GameContext.getSelectedGameObjectId())) {
            return;
        }
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


    /**
     * Global update logic: process an incoming MOVE message to update the player's state.
     */
    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            System.out.println("MOVE message parameters: " + Arrays.toString(params));
            if (params.length >= 2) {
                float newX = (params[0] instanceof Number)
                        ? ((Number) params[0]).floatValue()
                        : Float.parseFloat(params[0].toString());
                float newY = (params[1] instanceof Number)
                        ? ((Number) params[1]).floatValue()
                        : Float.parseFloat(params[1].toString());
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
     * Draws the player on a JavaFX GraphicsContext.
     * The player is drawn as a filled oval with the player's name centered above it.
     *
     * @param gc the JavaFX GraphicsContext used for drawing.
     */
    @Override
    public void draw(GraphicsContext gc) {
        // Set a specific color for the player.
        gc.setFill(Color.BLUE);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        // Set a different color for the text.
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        text.setFont(gc.getFont());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), x - textWidth / 2, y - radius - 5);
    }

    /**
     * Returns an array of constructor parameter values in the same order as the Player constructor.
     */
    @Override
    public Object[] getConstructorParamValues() {
        return new Object[] { getName(), x, y, radius, getGameId() };
    }
}
