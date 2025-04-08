package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;

/**
 * A JavaFX-based translation of the Pygame Player snippet:
 *   - position, velocity, acceleration
 *   - friction
 *   - horizontal movement with left/right
 *   - screen-wrapping on X-axis
 */
@Getter
@Setter
public class Player2 extends GameObject {

    // ---------------------------------
    // Constants matching the Python snippet
    // ---------------------------------
    private static final float PLAYER_ACC = 0.5f;     // Acceleration magnitude when pressing left/right
    private static final float PLAYER_FRICTION = -0.12f; // Negative for friction (slowing down)
    private static final float JUMP_FORCE = -15;
    
    // For screen-wrapping
    private static final float SCREEN_WIDTH = 800;
    private static final float SCREEN_HEIGHT = 600; // We don't do vertical wrap in the snippet, but we store height

    // ---------------------------------
    // Position, Velocity, Acceleration
    // ---------------------------------
    private Vector2 pos;  // Matches "self.pos" in Python
    private Vector2 vel;  // Matches "self.vel"
    private Vector2 acc;  // Matches "self.acc"

    // For rendering a rectangle
    private float width;
    private float height;

    boolean jumped = false;

    /**
     * Simple constructor: place player in the middle of the screen with a fixed size.
     */
    public Player2(String name, String gameId) {
     
        this(name, SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f, 30, 40, gameId);
    }

    /**
     * More flexible constructor if you want to pass different positions or sizes.
     */
    public Player2(String name, float x, float y, float w, float h, String gameId) {
        super(name, gameId);
        this.pos = new Vector2(x, y);
        this.vel = new Vector2(0, 0);
        this.acc = new Vector2(0, 0);
        this.width = w;
        this.height = h;
        setCollidable(true);
        setMovable(true);
    }

    // ---------------------------------
    // Equivalent of "update()" in Pygame
    // ---------------------------------

    private void updateFromKeyboardInput() {
        // If left arrow is pressed:
        if (KeyboardState.isKeyPressed(KeyCode.LEFT)) {
            acc.x = -PLAYER_ACC;
            Message moveMsg = new Message("MOVE", new Object[]{
                pos.x, pos.y, vel.x, vel.y, acc.x, acc.y
            }, null);
            sendMessage(moveMsg);
        }
        // If right arrow is pressed:
        if (KeyboardState.isKeyPressed(KeyCode.RIGHT)) {
            acc.x = PLAYER_ACC;
            Message moveMsg = new Message("MOVE", new Object[]{
                pos.x, pos.y, vel.x, vel.y, acc.x, acc.y
            }, null);
            sendMessage(moveMsg);
        }
        // If up arrow (jump) is pressed:
        if (KeyboardState.isKeyPressed(KeyCode.UP)) {
            if (!jumped) {
                vel.y += JUMP_FORCE;
                jumped = true;
            }
            Message moveMsg = new Message("MOVE", new Object[]{
                pos.x, pos.y, vel.x, vel.y, acc.x, acc.y
            }, null);
            sendMessage(moveMsg);
        }
    }

    @Override
    public void myUpdateLocal(float deltaTime) {
        // 1) Reset acceleration each frame to (0,0)
        acc.x = 0;
        acc.y = 0.5f;

        if(isSelected())
        {
            updateFromKeyboardInput();
        }


        // 3) Apply friction: acc += vel * PLAYER_FRICTION
        acc.x += vel.x * PLAYER_FRICTION;
        //acc.y += vel.y * PLAYER_FRICTION;

        // 4) Equations of motion:
        //    vel += acc
        //    pos += vel + 0.5 * acc
        // (For simplicity, ignoring deltaTime. If you want time-based movement,
        // multiply each addition by deltaTime or scale accordingly.)
        vel.x += acc.x;
        vel.y += acc.y;

        pos.x += vel.x + 0.5f * acc.x;
        pos.y += vel.y + 0.5f * acc.y;

        // 5) Wrap around screen horizontally
        //if (pos.x > SCREEN_WIDTH) {
        //    pos.x = 0;
        //} else if (pos.x < 0) {
        //    pos.x = SCREEN_WIDTH;
        //}

        // 6) If we've "jumped," check collisions with any Platform in the game.
        //    If we collide, reset 'jumped' to false (meaning we have "landed").
        if (jumped) {
            // Make sure we have a parent game reference
            if (getParentGame() != null) {
                // Loop through all game objects in the same game
                for (GameObject obj : getParentGame().getGameObjects()) {
                    // If obj is a Platform and we intersect it, reset jumped
                    if (obj instanceof Platform && this.intersects(obj)) {
                        jumped = false;
                        // Optional: also set your vertical velocity to 0 if you want to stop falling
                        // vel.y = 0;
                        break; // exit loop early if we found a collision
                    }
                }
            }
        }

    }

    /**
     * We are not using these in this snippet, so you can leave them empty or remove them.
     */
    @Override
    public void myUpdateLocal() { }

    @Override
    protected void myUpdateGlobal(Message msg) {
        if ("MOVE".equals(msg.getMessageType())) {
            Object[] params = msg.getParameters();
            if (params != null && params.length >= 6) {
                // Parse the six parameters:
                float newX = Float.parseFloat(params[0].toString());
                float newY = Float.parseFloat(params[1].toString());
                float newVx = Float.parseFloat(params[2].toString());
                float newVy = Float.parseFloat(params[3].toString());
                float newAccX = Float.parseFloat(params[4].toString());
                float newAccY = Float.parseFloat(params[5].toString());
                
                // Update the local state.
                //pos.x = newX;
                //pos.y = newY;
                vel.x = newVx;
                vel.y = newVy;
                acc.x = newAccX;
                acc.y = newAccY;
                
                System.out.println("Processed MOVE for " + getName() +
                                ": pos=(" + newX + ", " + newY + "), " +
                                "vel=(" + newVx + ", " + newVy + "), " +
                                "acc=(" + newAccX + ", " + newAccY + ")");
            }
        } 
    }



    // ---------------------------------
    // Drawing the rectangle & name
    // ---------------------------------
    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.YELLOW);
        gc.fillRect(pos.x, pos.y, width, height);

        // Draw player name above
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), pos.x + width / 2 - textWidth / 2, pos.y - 5);
    }

    // ---------------------------------
    // Required by GameObject's interface
    // ---------------------------------
    @Override
    public float getX() {
        return pos.x;
    }
    @Override
    public float getY() {
        return pos.y;
    }
    @Override
    public void setX(float x) {
        pos.x = x;
    }
    @Override
    public void setY(float y) {
        pos.y = y;
    }
    @Override
    public float getWidth() {
        return width;
    }
    @Override
    public float getHeight() {
        return height;
    }
    @Override
    public void setWidth(float width) {
        this.width = width;
    }
    @Override
    public void setHeight(float height) {
        this.height = height;
    }

    // If you need constructor params for reflection-based creation:
    @Override
    public Object[] getConstructorParamValues() {
        // (String name, float x, float y, float w, float h, String gameId)
        return new Object[]{ getName(), pos.x, pos.y, width, height, getGameId() };
    }

    // ---------------------------------
    // Simple Vector2 class
    // ---------------------------------
    public static class Vector2 {
        public float x;
        public float y;
        public Vector2(float x, float y) { this.x = x; this.y = y; }
        public Vector2() { this(0,0); }
        public void set(float x, float y) {
            this.x = x; this.y = y;
        }
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
}
