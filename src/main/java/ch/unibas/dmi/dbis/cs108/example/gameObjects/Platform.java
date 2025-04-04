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
public class Platform extends GameObject {

    // Position and size fields (bounding box)
    private double x;
    private double y;
    private double width;
    private double height;

    /**
     * Constructs a static Platform object.
     *
     * @param name   The platform's name.
     * @param x      The x-coordinate (starting position).
     * @param y      The y-coordinate (starting position).
     * @param width  The width of the platform.
     * @param height The height of the platform.
     * @param gameId The game session ID.
     */
    public Platform(String name, double x, double y, double width, double height, String gameId) {
        super(name, gameId);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Static platforms do not update their position.
     */
    @Override
    public void myUpdateLocal() {
        // No movement for static platforms.
    }

    /**
     * For now, static platforms don't process global updates.
     */
    @Override
    protected void myUpdateGlobal(Message msg) {
        // No global update needed for static platforms.
    }

    /**
     * Draws the platform as a dark gray rectangle with its name above it.
     */
    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(x, y, width, height);

        // Draw platform name
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        text.setFont(gc.getFont());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), x + width / 2 - textWidth / 2, y - 5);
    }

    /**
     * Returns the constructor parameter values for re-creating this object.
     */
    @Override
    public Object[] getConstructorParamValues() {
        return new Object[]{ getName(), x, y, width, height, getGameId() };
    }

    // --------------------
    // Bounding Box Methods for Collision Detection
    // --------------------

    @Override
    public float getX() {
        return (float) x;
    }

    @Override
    public float getY() {
        return (float) y;
    }

    @Override
    public float getWidth() {
        return (float) width;
    }

    @Override
    public float getHeight() {
        return (float) height;
    }

    /**
     * Since the platform is static, we override setX/setY to do nothing.
     */
    @Override
    public void setX(float x) {
        // Static platform: position should not change.
    }

    @Override
    public void setY(float y) {
        // Static platform: position should not change.
    }

    @Override
    public void setWidth(float width) {
        // Static platform: size should not change.
    }

    @Override
    public void setHeight(float height) {
        // Static platform: size should not change.
    }
}
