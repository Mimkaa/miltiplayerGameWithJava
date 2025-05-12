package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class Platform extends GameObject {

    // Position and size fields (bounding box) as floats.
    private float x;
    private float y;
    private float width;
    private float height;

    private static final Image TEXTURE = new Image(Platform.class.getResource("/texture/floor.png").toExternalForm());

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
    public Platform(String name, float x, float y, float width, float height, String gameId) {
        super(name, gameId);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        setCollidable(true);
        setMovable(false);
    }

    /**
     * Static platforms do not update their position.
     */
    @Override
    public void myUpdateLocal() {
        // No movement for static platforms.
    }

    @Override
    public void myUpdateLocal(float deltaTime) {
        // No update needed for static platforms.
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
        // draw the floor texture at our width, preserve aspect:
        double drawW = width;
        double drawH = TEXTURE.getHeight() * (width / TEXTURE.getWidth());
        gc.drawImage(TEXTURE, x, y, drawW, drawH);

        //draw hitbox:
        /*
        gc.setGlobalAlpha(0.3);
        gc.setFill(Color.GRAY);
        gc.fillRect(x, y, width, height);
        gc.setGlobalAlpha(1.0);
        gc.setStroke(Color.DARKGRAY);
        gc.strokeRect(x, y, width, height);
*/
        // Draw platform name.
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
        // The order must match your constructor: name, x, y, width, height, gameId.
        return new Object[]{ getName(), x, y, width, height, getGameId() };
    }

    // --------------------
    // Bounding Box Methods for Collision Detection
    // --------------------

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return height;
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
    @Override
    public Message createSnapshot() {
        // Pack the position, velocity, and acceleration into an Object array.
        Object[] params = new Object[]{
            x, y,   // Position
            
        };
        // Create a new message with type "SNAPSHOT" and an appropriate option (e.g., "UPDATE").
        Message snapshotMsg = new Message("SNAPSHOT", params, "GAME");
        
        // Set the concealed parameters so receivers know the source of the snapshot.
        snapshotMsg.setConcealedParameters(new String[]{ getId(), getGameId() });
        
        return snapshotMsg;
    }
}
