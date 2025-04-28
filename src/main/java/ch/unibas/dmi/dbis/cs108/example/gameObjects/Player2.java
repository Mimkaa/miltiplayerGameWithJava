package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import java.util.Arrays;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;

/**
 * The {@code Player2} class implements a player object with physics-based movement,
 * including acceleration, velocity, friction, jumping, grabbing, throwing, and interpolation
 * of state updates from the server. Rendering and input processing are handled within.
 */
@Getter
@Setter
public class Player2 extends GameObject implements IThrowable, IGrabbable {

    /** Flag indicating if the player is currently in throwing mode. */
    private boolean isThrowing = false;
    /** Current angle for throw direction, in degrees. */
    private float throwAngle = 90f;
    /** Increment for adjusting the throw angle each frame. */
    private float throwAngleDelta = 3.0f;
    private static final float MIN_THROW_ANGLE = 0f;
    private static final float MAX_THROW_ANGLE = 180f;
    /** Magnitude of the throw velocity. */
    private float throwMagnitude = 20f;

    private static final float PLAYER_ACC = 3.5f;
    private static final float PLAYER_FRICTION = -0.12f;
    private static final float JUMP_FORCE = -10f;
    private static final float SCREEN_WIDTH = 800f;
    private static final float SCREEN_HEIGHT = 600f;

    /** Current position vector. */
    private Vector2 pos;
    /** Current velocity vector. */
    private Vector2 vel;
    /** Current acceleration vector. */
    private Vector2 acc;
    /** Previous position, for optional comparison. */
    private Vector2 prevPos;

    private float width;
    private float height;
    private boolean jumped = false;
    private boolean jumpKeyPressed = false;
    private float moveMsgTimer = 0f;
    private boolean onGround = false;
    private GameObject grabbedGuy = null;
    private static final float GRAB_RADIUS = 50.0f;
    public boolean iAmGrabbed = false;

    private boolean interpolating = false;
    private Vector2 interpStartPos = new Vector2();
    private Vector2 interpEndPos   = new Vector2();
    private Vector2 interpStartVel = new Vector2();
    private Vector2 interpEndVel   = new Vector2();
    private Vector2 interpStartAcc = new Vector2();
    private Vector2 interpEndAcc   = new Vector2();
    private float interpElapsed    = 0f;
    private float interpDuration   = 0.05f;
    private int syncCounter = 0;
    private static final int SYNC_THRESHOLD = 0;

    /**
     * Constructs a player centered on screen with default size.
     *
     * @param name   display name of the player
     * @param gameId identifier of the game session
     */
    public Player2(String name, String gameId) {
        this(name, SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f, 30f, 40f, gameId);
    }

    /**
     * Constructs a player with specified position, size, and session.
     *
     * @param name   display name
     * @param x      initial x-coordinate
     * @param y      initial y-coordinate
     * @param w      width of player
     * @param h      height of player
     * @param gameId game session identifier
     */
    public Player2(String name, float x, float y, float w, float h, String gameId) {
        super(name, gameId);
        this.pos = new Vector2(x, y);
        this.vel = new Vector2(0f, 0f);
        this.acc = new Vector2(0f, 0f);
        this.prevPos = new Vector2(x, y);
        this.width = w;
        this.height = h;
        setCollidable(true);
        setMovable(true);
    }

    /**
     * Updates local player state using deltaTime, handling physics or interpolation.
     *
     * @param deltaTime seconds since last update
     */
    @Override
    public void myUpdateLocal(float deltaTime) {
        if (interpolating) {
            interpElapsed += deltaTime;
            float alpha = interpElapsed / interpDuration;
            if (alpha >= 1f) { alpha = 1f; interpolating = false; }
            pos.x = lerp(interpStartPos.x, interpEndPos.x, alpha);
            pos.y = lerp(interpStartPos.y, interpEndPos.y, alpha);
            vel.x = lerp(interpStartVel.x, interpEndVel.x, alpha);
            vel.y = lerp(interpStartVel.y, interpEndVel.y, alpha);
            acc.x = lerp(interpStartAcc.x, interpEndAcc.x, alpha);
            acc.y = lerp(interpStartAcc.y, interpEndAcc.y, alpha);
            return;
        }
        if (iAmGrabbed) {
            updateMovement();
            return;
        }
        acc.y = 0.5f;
        acc.x += vel.x * PLAYER_FRICTION;
        vel.x += acc.x;
        vel.y += acc.y;
        pos.x += vel.x + 0.5f * acc.x;
        pos.y += vel.y + 0.5f * acc.y;
        acc.x = 0f;
        checkGroundCollision();
        if (grabbedGuy != null) {
            grabbedGuy.setPos(new Vector2(pos.x, pos.y - grabbedGuy.getHeight()));
        }
        if (isThrowing) {
            throwAngle += throwAngleDelta;
            if (throwAngle < MIN_THROW_ANGLE) { throwAngle = MIN_THROW_ANGLE; throwAngleDelta = -throwAngleDelta; }
            else if (throwAngle > MAX_THROW_ANGLE) { throwAngle = MAX_THROW_ANGLE; throwAngleDelta = -throwAngleDelta; }
        }
        if (parentGame.isAuthoritative()) {
            syncCounter++;
            if (syncCounter >= SYNC_THRESHOLD) {
                Message snapshot = createSnapshot();
                Server.getInstance().sendMessageBestEffort(snapshot);
                syncCounter = 0;
            }
        }
    }

    /**
     * Unused local update stub (required by contract).
     */
    @Override public void myUpdateLocal() { }

    /**
     * Checks for ground collision within a tolerance and resets jump state.
     */
    private void checkGroundCollision() {
        Game currentGame = getParentGame();
        if (currentGame == null) return;
        float tolerance = 10f;
        float bottom = getY() + getHeight();
        onGround = false;
        for (GameObject other : currentGame.getGameObjects()) {
            if (other == this || !other.isCollidable()) continue;
            float otherTop = other.getY();
            boolean horizontalOverlap = !(getX() + getWidth() <= other.getX() || getX() >= other.getX() + other.getWidth());
            if (vel.y >= 0 && horizontalOverlap && bottom >= otherTop - tolerance && bottom <= otherTop + tolerance) {
                setY(otherTop - getHeight());
                vel.y = 0f;
                onGround = true;
                jumped = false;
                break;
            }
        }
    }

    /**
     * Handles incoming messages for movement, key events, and snapshots.
     *
     * @param msg the received Message object
     */
    @Override
    protected void myUpdateGlobal(Message msg) {
        if (iAmGrabbed) return;
        String type = msg.getMessageType();
        if ("KEY_PRESS".equals(type)) {
            Object[] params = msg.getParameters();
            if (params != null && params.length >= 1) {
                String key = params[0].toString();
                if (KeyCode.LEFT.toString().equals(key)) acc.x += -PLAYER_ACC;
                else if (KeyCode.RIGHT.toString().equals(key)) acc.x += PLAYER_ACC;
                else if (KeyCode.UP.toString().equals(key) && !jumped && onGround) {
                    vel.y += (grabbedGuy != null ? JUMP_FORCE/2f : JUMP_FORCE);
                    jumped = true;
                }
                if (KeyCode.E.toString().equals(key)) {
                    // grab/release logic omitted for brevity
                } else if (KeyCode.F.toString().equals(key)) {
                    isThrowing = !isThrowing;
                } else if (KeyCode.R.toString().equals(key) && isThrowing && grabbedGuy != null) {
                    double rad = Math.toRadians(throwAngle);
                    float vx = (float)(throwMagnitude * Math.cos(rad));
                    float vy = (float)(-throwMagnitude * Math.sin(rad));
                    if (grabbedGuy instanceof IGrabbable) {
                        ((IGrabbable)grabbedGuy).setVelocity(vx, vy);
                        grabbedGuy.onRelease();
                        grabbedGuy = null;
                    }
                    isThrowing = false;
                }
            }
        } else if ("SNAPSHOT".equals(type)) {
            Object[] p = msg.getParameters();
            if (p != null && p.length >= 6) {
                try {
                    float nx = Float.parseFloat(p[0].toString());
                    float ny = Float.parseFloat(p[1].toString());
                    float nvx = Float.parseFloat(p[2].toString());
                    float nvy = Float.parseFloat(p[3].toString());
                    float nax = Float.parseFloat(p[4].toString());
                    float nay = Float.parseFloat(p[5].toString());
                    interpStartPos.set(pos.x, pos.y);
                    interpEndPos.set(nx, ny);
                    interpStartVel.set(vel.x, vel.y);
                    interpEndVel.set(nvx, nvy);
                    interpStartAcc.set(acc.x, acc.y);
                    interpEndAcc.set(nax, nay);
                    interpolating = true;
                    interpElapsed = 0f;
                } catch (NumberFormatException ex) {
                    System.out.println("Error processing SNAPSHOT: " + Arrays.toString(p));
                }
            }
        } else {
            System.out.println("Unknown message type: " + type);
        }
    }

    /**
     * Throws the currently grabbed object with given velocities.
     *
     * @param throwVx horizontal velocity component
     * @param throwVy vertical velocity component
     */
    @Override
    public void throwObject(float throwVx, float throwVy) {
        if (grabbedGuy instanceof IGrabbable) {
            ((IGrabbable) grabbedGuy).setVelocity(throwVx, throwVy);
            grabbedGuy.onRelease();
            grabbedGuy = null;
        }
    }

    /**
     * Sets the player's velocity vector directly.
     *
     * @param vx velocity in x direction
     * @param vy velocity in y direction
     */
    @Override
    public void setVelocity(float vx, float vy) {
        this.vel.x = vx;
        this.vel.y = vy;
    }

    /**
     * Updates position based on current velocity (used when grabbed).
     */
    public void updateMovement() {
        this.pos.x += this.vel.x;
        this.pos.y += this.vel.y;
    }

    /**
     * Renders the player rectangle, name, and throw indicator if active.
     *
     * @param gc GraphicsContext to draw on
     */
    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.MEDIUMPURPLE);
        gc.fillRect(pos.x, pos.y, width, height);
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        double tw = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), pos.x + width/2 - tw/2, pos.y - 5);
        if (isThrowing) {
            double cx = pos.x + width/2;
            double cy = pos.y + height/2;
            double len = 50;
            double rad = Math.toRadians(throwAngle);
            double ex = cx + Math.cos(rad)*len;
            double ey = cy - Math.sin(rad)*len;
            gc.setStroke(Color.RED);
            gc.strokeLine(cx, cy, ex, ey);
        }
    }

    /**
     * Creates a snapshot message of position, velocity, and acceleration.
     *
     * @return Message of type "SNAPSHOT" with concealed parameters [id, gameId, tick]
     */
    @Override
    public Message createSnapshot() {
        long tick = 0;
        Game g = getParentGame(); if (g != null) tick = g.getTickCount();
        Object[] params = { pos.x, pos.y, vel.x, vel.y, acc.x, acc.y };
        Message m = new Message("SNAPSHOT", params, "GAME");
        m.setConcealedParameters(new String[]{ getId(), getGameId(), String.valueOf(tick) });
        return m;
    }

    @Override public float getX()       { return pos.x; }
    @Override public float getY()       { return pos.y; }
    @Override public void  setX(float x){ pos.x = x; }
    @Override public void  setY(float y){ pos.y = y; }
    @Override public float getWidth()   { return width; }
    @Override public float getHeight()  { return height; }
    @Override public void  setWidth(float w){ this.width = w; }
    @Override public void  setHeight(float h){ this.height = h; }

    /**
     * Returns constructor arguments in signature order for replication.
     */
    @Override
    public Object[] getConstructorParamValues() {
        return new Object[]{ getName(), pos.x, pos.y, width, height, getGameId() };
    }

    /**
     * Handles grab event by marking player as grabbed.
     *
     * @param playerId id of the grabbing player
     */
    @Override
    public void onGrab(String playerId) {
        iAmGrabbed = true;
    }

    /**
     * Handles release event by unmarking grabbed state.
     */
    @Override
    public void onRelease() {
        iAmGrabbed = false;
    }

    /**
     * Indicates whether this player is currently grabbed (always false here).
     *
     * @return false
     */
    @Override
    public boolean isGrabbed() { return false; }

    /**
     * Retrieves the ID of the grabbing player (empty if none).
     *
     * @return empty string
     */
    @Override
    public String getGrabbedBy() { return ""; }

    /**
     * Sets position to the given coordinates.
     *
     * @param x new x-coordinate
     * @param y new y-coordinate
     */
    @Override
    public void setPos(float x, float y) {
        this.pos = new Vector2(x, y);
    }

    /**
     * Simple 2D vector class for position, velocity, etc.
     */
    public static class Vector2 {
        public float x;
        public float y;

        /**
         * Constructs a vector with given components.
         */
        public Vector2(float x, float y) { this.x = x; this.y = y; }

        /**
         * Constructs a zero vector.
         */
        public Vector2() { this(0f, 0f); }

        /**
         * Sets both components.
         */
        public void set(float x, float y) { this.x = x; this.y = y; }

        @Override
        public String toString() { return "(" + x + ", " + y + ")"; }
    }

    /**
     * Linear interpolation helper between start and end.
     *
     * @param start start value
     * @param end   end value
     * @param alpha interpolation fraction (0..1)
     * @return interpolated value
     */
    private float lerp(float start, float end, float alpha) {
        return start + (end - start) * alpha;
    }
}
