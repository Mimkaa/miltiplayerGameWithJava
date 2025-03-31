package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;


import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import java.util.Arrays;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class Square extends GameObject {
    private float x;
    private float y;
    private float radius;
    private float oldRadius; // Track previous radius value.
    private final float inflationRate = 5.0f;  // How much the square inflates per key press.

    /**
     * Constructs a Square game object.
     *
     * @param name       The object's name.
     * @param x          The starting X coordinate.
     * @param y          The starting Y coordinate.
     * @param radius     The initial radius (half the side length).
     * @param myGameId   The unique identifier of the game session.
     */
    public Square(String name, float x, float y, float radius, String myGameId) {
        super(name, myGameId);
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.oldRadius = radius;
    }

    /**
     * Local update logic: detect changes to the square's radius and send an "INFT" message if it has changed.
     */
    @Override
    public void myUpdateLocal() {
        if (radius != oldRadius) {
            Message inflateMsg = new Message("INFT", new Object[]{ radius }, null,
                    new String[]{ getName(), getGameId() });
            sendMessage(inflateMsg);
            oldRadius = radius;
        }
    }

    /**
     * Global update logic: process an incoming "INFT" message to update the square's radius.
     *
     * @param msg the message to process.
     */
    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("INFT".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            System.out.println("INFT message parameters: " + Arrays.toString(params));
            if (params != null && params.length > 0) {
                float newRadius = (params[0] instanceof Number)
                        ? ((Number) params[0]).floatValue()
                        : Float.parseFloat(params[0].toString());
                this.radius = newRadius;
                System.out.println("Processed INFT for " + getName() + ", new radius: " + newRadius);
            }
        }
    }

    /**
     * Draws the square using JavaFX's GraphicsContext.
     * The square is drawn in red and its name is centered above it in black.
     *
     * @param gc the GraphicsContext used for drawing.
     */
    @Override
    public void draw(GraphicsContext gc) {
        // Set fill color to red and draw the square.
        gc.setFill(Color.RED);
        double side = radius * 2;
        gc.fillRect(x - radius, y - radius, side, side);

        // Draw the object's name in black above the square.
        gc.setFill(Color.BLACK);
        // Use a temporary Text node to measure text width.
        Text text = new Text(getName());
        text.setFont(gc.getFont());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), x - textWidth / 2, y - radius - 5);
    }

    /**
     * Returns an array of constructor parameter values in the order:
     * (name, x, y, radius, myGameId).
     */
    @Override
    public Object[] getConstructorParamValues() {
        return new Object[] { getName(), x, y, radius, getGameId() };
    }
}

