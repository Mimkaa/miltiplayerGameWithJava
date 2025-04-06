package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Square extends GameObject {

    // For a square, the side length is used for both width and height.
    private float x;
    private float y;
    private float side;  // side length

    private float speed = 3.0f;
    private float vx = 1.0f;
    private float vy = 1.0f;

    /**
     * Constructs a Square game object.
     *
     * @param name   The object's name.
     * @param x      Starting x coordinate.
     * @param y      Starting y coordinate.
     * @param side   The length of each side.
     * @param gameId The game session ID.
     */
    public Square(String name, String gameId, float x, float y, float side) {
        super(name, gameId);
        this.x = x;
        this.y = y;
        this.side = side;
    }

    @Override
    public void myUpdateLocal() {
        float oldX = getX();
        float oldY = getY();
        setX(getX() + vx * speed);
        setY(getY() + vy * speed);
        if (getX() != oldX || getY() != oldY) {
            Message moveMsg = new Message("MOVE", new Object[]{getX(), getY()}, null);
            sendMessage(moveMsg);
        }
    }

    @Override
    public void myUpdateLocal(float deltaTime) {

    }

    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params.length >= 2) {
                float newX = (params[0] instanceof Number) ? ((Number) params[0]).floatValue() : Float.parseFloat(params[0].toString());
                float newY = (params[1] instanceof Number) ? ((Number) params[1]).floatValue() : Float.parseFloat(params[1].toString());
                synchronized (this) {
                    setX(newX);
                    setY(newY);
                }
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.RED);
        gc.fillRect(getX(), getY(), getWidth(), getHeight());
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        text.setFont(gc.getFont());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), getX() + getWidth() / 2 - textWidth / 2, getY() - 5);
    }

    @Override
    public Object[] getConstructorParamValues() {
        return new Object[]{ getName(), getGameId(), getX(), getY(), side };
    }

    // In a square, width and height are both equal to side.
    @Override
    public float getWidth() { return side; }
    @Override
    public float getHeight() { return side; }
    @Override
    public void setWidth(float width) { this.side = width; }
    @Override
    public void setHeight(float height) { this.side = height; }

    @Override
    public float getX() { return this.x; }
    @Override
    public float getY() { return this.y; }
    @Override
    public void setX(float x) { this.x = x; }
    @Override
    public void setY(float y) { this.y = y; }
}
