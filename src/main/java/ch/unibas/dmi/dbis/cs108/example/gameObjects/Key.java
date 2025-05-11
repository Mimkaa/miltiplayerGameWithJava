package ch.unibas.dmi.dbis.cs108.example.gameObjects;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;
import java.util.Arrays;
import java.util.Set;

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
 * A JavaFX-based translation of the Pygame Player snippet:
 *   - position, velocity, acceleration
 *   - friction
 *   - horizontal movement with left/right
 *   - screen-wrapping on X-axis
 */
@Getter
@Setter
public class Key extends GameObject implements IGrabbable {

    // Constant for throwing mode and its parameters
    private boolean isThrowing = false;
    private float throwAngle = 90f;
    private float throwAngleDelta = 3.0f;
    private static final float MIN_THROW_ANGLE = 0f;
    private static final float MAX_THROW_ANGLE = 180f;
    private float throwMagnitude = 20f;

    // ---------------------------------
    // Constants matching the Python snippet
    // ---------------------------------
    private static final float PLAYER_ACC = 3.5f;       // Acceleration magnitude when pressing left/right
    private static final float PLAYER_FRICTION = -0.12f; // Negative for friction (slowing down)
    private static final float JUMP_FORCE = -40;         // The lower, the higher player can jump
    private static final float SCREEN_WIDTH = 800;
    private static final float SCREEN_HEIGHT = 600;      // Height is stored even though vertical wrap isn't used

    // ---------------------------------
    // Position, Velocity, Acceleration
    // ---------------------------------
    private Vector2 pos;      // Current position
    private Vector2 vel;      // Velocity
    private Vector2 acc;      // Acceleration

    // (Optional) Store previous position if you want to compare changes later.
    private Vector2 prevPos;

    // ---------------------------------
    // Rendering dimensions
    // ---------------------------------
    private float width;
    private float height;

    // ---------------------------------
    // Jump management flags
    // ---------------------------------
    private boolean jumped = false;
    // This flag can be used later if you need to detect key state changes.
    private boolean jumpKeyPressed = false;

    // ---------------------------------
    // Timer for sending MOVE messages at fixed intervals (not shown in snippet).
    // ---------------------------------
    private float moveMsgTimer = 0;

    // Ground collision flag.
    private boolean onGround = false;

    // Reference to the grabbed player, if any.
    private GameObject grabbedGuy = null;
    private static final float GRAB_RADIUS = 50.0f;
    private boolean iAmGrabbed = false;
    private String grabbedBy = null;

    // ---------------------------------
    // INTERPOLATION FIELDS for smoothing out server SNAPSHOT corrections
    // ---------------------------------
    private boolean interpolating = false;         // true if we're currently smoothing from an old state to a new state
    private Vector2 interpStartPos = new Vector2();  // starting position
    private Vector2 interpEndPos   = new Vector2();  // target position

    // --- New: interpolation fields for velocity ---
    private Vector2 interpStartVel = new Vector2();  // starting velocity
    private Vector2 interpEndVel   = new Vector2();  // target velocity

    // --- New: interpolation fields for acceleration ---
    private Vector2 interpStartAcc = new Vector2();  // starting acceleration
    private Vector2 interpEndAcc   = new Vector2();  // target acceleration

    private float interpElapsed  = 0f;              // time elapsed during interpolation
    private float interpDuration = 0.05f;            // duration (in seconds) over which to interpolate

    private int syncCounter = 0;
    private static final int SYNC_THRESHOLD = 0;     // only send snapshot every 50 KEY_PRESS messages


    /**
     * Simple constructor: place player in the middle of the screen with a fixed size.
     */
    public Key(String name, String gameId) {
        this(name, SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f, 30, 40, gameId);
    }

    /**
     * More flexible constructor if you want to pass different positions or sizes.
     */
    public Key(String name, float x, float y, float w, float h, String gameId) {
        super(name, gameId);
        this.pos = new Vector2(x, y);
        this.vel = new Vector2(0, 0);
        this.acc = new Vector2(0, 0);
        this.prevPos = new Vector2(x, y);  // Initialize previous position to the starting position.
        this.width = w;
        this.height = h;
        setCollidable(true);
        setMovable(true);
    }



    @Override
    public void myUpdateLocal(float deltaTime) {
        // If we're interpolating from a previous state to a new authoritative state, interpolate position, velocity, and acceleration.
        if (interpolating) {
            // Update elapsed time
            interpElapsed += deltaTime;

            // Compute the fraction of interpolation completed (0 to 1)
            float alpha = interpElapsed / interpDuration;
            if (alpha >= 1.0f) {
                alpha = 1.0f;
                interpolating = false; // Finished interpolation
            }

            // Interpolate position.
            pos.x = lerp((float)interpStartPos.x, (float)interpEndPos.x, alpha);
            pos.y = lerp((float)interpStartPos.y, (float)interpEndPos.y, alpha);

            // Interpolate velocity.
            vel.x = lerp((float)interpStartVel.x, (float)interpEndVel.x, alpha);
            vel.y = lerp((float)interpStartVel.y, (float)interpEndVel.y, alpha);

            // --- New: interpolate acceleration ---
            acc.x = lerp((float)interpStartAcc.x, (float)interpEndAcc.x, alpha);
            acc.y = lerp((float)interpStartAcc.y, (float)interpEndAcc.y, alpha);

            return;
        }

        // If not interpolating, execute normal local update logic:
        if (iAmGrabbed) {
            updateMovement();
            return;
        }
        // 1) Reset acceleration and apply gravity.
        acc.y = 5f;
        acc.x += vel.x * PLAYER_FRICTION;

        // 2) Update velocity.
        vel.x += acc.x;
        vel.y += acc.y;

        // 3) Update position.
        // 3) Update position.  <<<<<<<<<<<<<<< ONLY change is the * deltaTime
        pos.x += vel.x  + 0.5f * acc.x * deltaTime ;
        pos.y += vel.y  + 0.5f * acc.y * deltaTime ;


        // 4) Reset horizontal acceleration.
        acc.x = 0;

        // 5) Check for collision and update ground collision status.
        checkGroundCollision();

        

        // 6) If in throwing mode, update the throw angle with a windshield-wiper oscillation.
        if (isThrowing) {
            throwAngle += throwAngleDelta;
            if (throwAngle < MIN_THROW_ANGLE) {
                throwAngle = MIN_THROW_ANGLE;
                throwAngleDelta = -throwAngleDelta;
            } else if (throwAngle > MAX_THROW_ANGLE) {
                throwAngle = MAX_THROW_ANGLE;
                throwAngleDelta = -throwAngleDelta;
            }
        }

        //if (parentGame.isAuthoritative()) {
        //    syncCounter++;
        //    if (syncCounter >= SYNC_THRESHOLD) {
                // Broadcast a snapshot.
        //        Message snapshot = createSnapshot();
        //        Server.getInstance().sendMessageBestEffort(snapshot);

                // Reset the counter.
        //        syncCounter = 0;
        //    }
        //}
    }

    /**
     * Not used in this snippet but required by GameObject's contract.
     */
    @Override
    public void myUpdateLocal() {
        // No default logic here.
    }

    /**
     * Checks if this player is "on the ground" by testing collision below (with a tolerance).
     * When colliding with a supporting platform (or player), reset the jump flag.
     */
    private void checkGroundCollision() {
        Game currentGame = getParentGame();
        if (currentGame == null) return;

        final float tolerance = 10.0f;
        float bottom = getY() + getHeight();
        onGround = false;
        for (GameObject other : currentGame.getGameObjects()) {
            if (other == this || !other.isCollidable()) continue;
            float otherTop = other.getY();
            float myLeft = getX();
            float myRight = getX() + getWidth();
            float otherLeft = other.getX();
            float otherRight = other.getWidth() + other.getX();
            boolean horizontalOverlap = !(myRight <= otherLeft || myLeft >= otherRight);

            if (vel.y >= 0 && horizontalOverlap &&
                bottom >= otherTop - tolerance && bottom <= otherTop + tolerance) {

                setY(otherTop - getHeight());
                vel.y = 0;
                onGround = true;
                jumped = false;  // Reset jump flag on landing.
                break;
            }
        }
    }

    @Override
    protected void myUpdateGlobal(Message msg) {
        if (iAmGrabbed) {
            // If the player is grabbed, ignore external commands.
            return;
        }

        String type = msg.getMessageType();
        
            
        if ("SNAPSHOT".equals(type)) {
            // Process SNAPSHOT messages for non-authoritative clients.
            Object[] params = msg.getParameters();
            if (params != null && params.length >= 6) {
                try {
                    float newX    = Float.parseFloat(params[0].toString());
                    float newY    = Float.parseFloat(params[1].toString());
                    float newVelX = Float.parseFloat(params[2].toString());
                    float newVelY = Float.parseFloat(params[3].toString());
                    float newAccX = Float.parseFloat(params[4].toString());
                    float newAccY = Float.parseFloat(params[5].toString());

                    // Instead of snapping directly, set up interpolation:
                    interpStartPos.x = pos.x;
                    interpStartPos.y = pos.y;
                    interpEndPos.x   = newX;
                    interpEndPos.y   = newY;
                    
                    // --- New: store current and target velocity for interpolation ---
                    interpStartVel.x = vel.x;
                    interpStartVel.y = vel.y;
                    interpEndVel.x   = newVelX;
                    interpEndVel.y   = newVelY;
                    
                    // --- New: store current and target acceleration for interpolation ---
                    interpStartAcc.x = acc.x;
                    interpStartAcc.y = acc.y;
                    interpEndAcc.x   = newAccX;
                    interpEndAcc.y   = newAccY;
                    
                    interpolating = true;
                    interpElapsed = 0f;

                    // Optionally, if you want to immediately adopt the target acceleration after interpolation,
                    // you may comment out the following direct assignment.
                    // acc.x = newAccX;
                    // acc.y = newAccY;

                    System.out.println("Processed SNAPSHOT for " + getId()
                                       + ": pos=(" + newX + ", " + newY + ")");
                } catch (NumberFormatException ex) {
                    System.out.println("Error processing SNAPSHOT parameters: " + Arrays.toString(params));
                }
            } else {
                System.out.println("SNAPSHOT message for Player2 does not contain enough parameters.");
            }
        }
        else {
            System.out.println("Unknown message type: " + type);
        }
    }

    

    public void setVelocity(float vx, float vy) {
        this.vel.x = vx;
        this.vel.y = vy;
    }

    public void updateMovement() {
        // Update the position based on velocity.
        this.pos.x += this.vel.x;
        this.pos.y += this.vel.y;
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.GOLD);
        gc.fillRect(pos.x, pos.y, width, height);
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), pos.x + width / 2 - textWidth / 2, pos.y - 5);
    }


    @Override
    public Message createSnapshot() {
        // Grab the current tick from the parent game.
        long currentTick = 0;
        Game parentGame = getParentGame();
        if (parentGame != null) {
            currentTick = parentGame.getTickCount();
        }

        // Pack position, velocity, acceleration in the main parameter array.
        Object[] params = new Object[] {
            pos.x, pos.y,   // Position
            vel.x, vel.y,   // Velocity
            acc.x, acc.y    // Acceleration
        };

        // Create the snapshot message.
        Message snapshotMsg = new Message("SNAPSHOT", params, "GAME");

        // Build the concealed array.
        snapshotMsg.setConcealedParameters(new String[] {
            getId(),
            getGameId(),
            String.valueOf(currentTick)
        });

        return snapshotMsg;
    }

    @Override
    public void onGrab(String playerId) {
        this.iAmGrabbed = true;
        this.grabbedBy  = playerId;
    }

    @Override
    public void onRelease() {
        this.iAmGrabbed = false;
        this.grabbedBy  = null;
    }

    @Override
    public boolean isGrabbed() {
        return this.iAmGrabbed;         // ← no longer a stub
    }

    @Override
    public String getGrabbedBy() {
        return this.grabbedBy;          // ← so we know who holds it
    }

    @Override
    public void setPos(float x, float y) {
        // ← actually move the key!
        this.pos.x = x;
        this.pos.y = y;
    }

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

    @Override
    public Object[] getConstructorParamValues() {
        return new Object[] { getName(), pos.x, pos.y, width, height, getGameId() };
    }



    // ---------------------------------
    // Simple Vector2 class.
    // ---------------------------------
    public static class Vector2 {
        public float x;
        public float y;

        public Vector2(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public Vector2() {
            this(0, 0);
        }

        public void set(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    // ---------------------------------
    // Simple linear interpolation helper
    // ---------------------------------
    private float lerp(float start, float end, float alpha) {
        return start + (end - start) * alpha;
    }







}
