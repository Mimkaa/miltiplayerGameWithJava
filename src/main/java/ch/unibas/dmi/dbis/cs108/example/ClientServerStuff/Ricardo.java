package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ricardo extends GameObject {

    private float x;
    private float y;
    private float width;
    private float height;
    private float rotation; // in degrees
    private float speed = 4.0f;
    private float vx = 1.0f;
    private float vy = 0.5f;
    private Image image;
    private String imagePath;

    /**
     * Constructs a Ricardo game object.
     *
     * @param name      The object's name.
     * @param gameId    The game session ID.
     * @param x         Starting x coordinate.
     * @param y         Starting y coordinate.
     * @param width     Width of the object.
     * @param height    Height of the object.
     * @param imagePath File path to an image.
     */
    public Ricardo(String name, String gameId, float x, float y, float width, float height, String imagePath) {
        super(name, gameId);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rotation = 0;
        this.imagePath = imagePath;
        try {
            image = new Image("file:" + imagePath);
        } catch (Exception e) {
            System.err.println("Error loading image for Ricardo: " + e.getMessage());
            image = null;
        }
    }

    @Override
    public void myUpdateLocal() {
        x += vx * speed;
        y += vy * speed;
        rotation += 2;
        if (rotation >= 360) rotation -= 360;
        Message moveMsg = new Message("MOVE", new Object[]{x, y, rotation}, null);
        sendMessage(moveMsg);
    }

    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params.length >= 3) {
                float newX = (params[0] instanceof Number) ? ((Number) params[0]).floatValue() : Float.parseFloat(params[0].toString());
                float newY = (params[1] instanceof Number) ? ((Number) params[1]).floatValue() : Float.parseFloat(params[1].toString());
                float newRotation = (params[2] instanceof Number) ? ((Number) params[2]).floatValue() : Float.parseFloat(params[2].toString());
                synchronized (this) {
                    this.x = newX;
                    this.y = newY;
                    this.rotation = newRotation;
                }
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.save();
        gc.translate(x + width / 2, y + height / 2);
        gc.rotate(rotation);
        if (image != null) {
            gc.drawImage(image, -width / 2, -height / 2, width, height);
        } else {
            gc.setFill(Color.DARKRED);
            gc.fillRect(-width / 2, -height / 2, width, height);
        }
        gc.restore();

        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        text.setFont(gc.getFont());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), x + width / 2 - textWidth / 2, y - 5);
    }

    @Override
    public Object[] getConstructorParamValues() {
        return new Object[]{ getName(), getGameId(), x, y, width, height, imagePath };
    }

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
}
