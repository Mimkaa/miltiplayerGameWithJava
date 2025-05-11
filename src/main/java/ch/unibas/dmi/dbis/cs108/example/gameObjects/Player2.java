package ch.unibas.dmi.dbis.cs108.example.gameObjects;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;
import java.util.Arrays;
import java.util.Set;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;

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
public class Player2 extends GameObject implements IThrowable, IGrabbable {

    // Constant for throwing mode and its parameters
    private boolean isThrowing = false;
    private float throwAngle = 90f;
    private float throwAngleDelta = 3.0f;
    private static final float MIN_THROW_ANGLE = 0f;
    private static final float MAX_THROW_ANGLE = 180f;
    private float throwMagnitude = 20f;

    // ---------------------------------
    // Movement constants
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
    public boolean iAmGrabbed = false;

    // ---------------------------------
    // INTERPOLATION FIELDS for smoothing out server SNAPSHOT corrections
    // ---------------------------------
    private boolean interpolating = false;         // true if we're currently smoothing from an old state to a new state
    private Vector2 interpStartPos = new Vector2();  // starting position
    private Vector2 interpEndPos   = new Vector2();  // target position
    // --- New: interpolation fields for velocity
    private Vector2 interpStartVel = new Vector2();  // starting velocity
    private Vector2 interpEndVel   = new Vector2();  // target velocity
    // --- New: interpolation fields for acceleration
    private Vector2 interpStartAcc = new Vector2();  // starting acceleration
    private Vector2 interpEndAcc   = new Vector2();  // target acceleration
    private float interpElapsed  = 0f;              // time elapsed during interpolation
    private float interpDuration = 0.05f;            // duration (in seconds) over which to interpolate

    //-----------------------------------
    // Network sync
    //-----------------------------------
    private int syncCounter = 0;
    private static final int SYNC_THRESHOLD = 0;     // only send snapshot every 50 KEY_PRESS messages


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
        this.prevPos = new Vector2(x, y);  // Initialize previous position to the starting position.
        this.width = w;
        this.height = h;
        setCollidable(true);
        setMovable(true);
    }



    @Override
    public void myUpdateLocal(float deltaTime) {
        // 1) Interpolation überspringen …
        if (interpolating) {
            interpElapsed += deltaTime;
            float alpha = interpElapsed / interpDuration;
            if (alpha >= 1.0f) {
                alpha = 1.0f;
                interpolating = false; // Finished interpolation
            }

            // Interpolate position.
            pos.x = lerp(interpStartPos.x, interpEndPos.x, alpha);
            pos.y = lerp(interpStartPos.y, interpEndPos.y, alpha);

            // Interpolate velocity.
            vel.x = lerp(interpStartVel.x, interpEndVel.x, alpha);
            vel.y = lerp(interpStartVel.y, interpEndVel.y, alpha);

            // --- New: interpolate acceleration ---
            acc.x = lerp(interpStartAcc.x, interpEndAcc.x, alpha);
            acc.y = lerp(interpStartAcc.y, interpEndAcc.y, alpha);
            // KEIN return – danach Input & Physik
        }

        // 2) INPUT-POLLING: Links/Rechts/Sprung
        acc.x = 0;
        if (KeyboardState.isKeyPressed(KeyCode.LEFT)) {
            acc.x -= PLAYER_ACC;
        }
        if (KeyboardState.isKeyPressed(KeyCode.RIGHT)) {
            acc.x += PLAYER_ACC;
        }
        // Springen nur einmal pro Taste und nur wenn auf dem Boden
        if (KeyboardState.isKeyPressed(KeyCode.UP) && onGround && !jumped) {
            vel.y = JUMP_FORCE;   // JUMP_FORCE aus Deinem ersten Snippet (z.B. -300)
            jumped = true;
        }

        // 3) Wenn gegriffen, nur Positions-Update
        if (iAmGrabbed) {
            updateMovement();
            return;
        }

        // 4) Gravitation & Reibung
        acc.y = GravityEngine.GRAVITY;              // z.B. +500 oder was immer Deine Engine nutzt
        acc.x += vel.x * PLAYER_FRICTION;           // PLAYER_FRICTION aus zweitem Snippet

        // 5) Geschwindigkeit aktualisieren
        vel.x += acc.x * deltaTime;
        vel.y += acc.y * deltaTime;

        // 6) Vertikale Geschwindigkeit begrenzen (optional)
        if (vel.y < -600f) vel.y = -600f;
        if (vel.y >  600f) vel.y =  600f;

        // 7) Position aktualisieren
        pos.x += vel.x * deltaTime;
        pos.y += vel.y * deltaTime + 0.5f * acc.y * deltaTime * deltaTime;

        // 8) Reset Horizontal-Beschleunigung für die nächste Runde
        acc.x = 0;

        // 9) Bodenkollision prüfen und Jump-Flag ggf. zurücksetzen
        checkGroundCollision();

        // 10) Gegriffene Objekte mitschleifen
        if (grabbedGuy != null) {
            grabbedGuy.setPos(new Vector2(pos.x, pos.y - grabbedGuy.getHeight()));
        }

        // 11) Throwing-Modus beibehalten …
        if (isThrowing) {
            throwAngle += throwAngleDelta;
            if (throwAngle < MIN_THROW_ANGLE || throwAngle > MAX_THROW_ANGLE) {
                throwAngleDelta = -throwAngleDelta;
                throwAngle = Clamp(throwAngle, MIN_THROW_ANGLE, MAX_THROW_ANGLE);
            }
        }
    }

    private float Clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
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
        if ("KEY_PRESS".equals(type) && parentGame.isAuthoritative()) {
            // Process key press events.
            Object[] params = msg.getParameters();
            if (params != null && params.length >= 1) {
                String keyString = params[0].toString();

                // Basic movement logic.
                if (KeyCode.LEFT.toString().equals(keyString)) {
                    acc.x -= PLAYER_ACC;
                } else if (KeyCode.RIGHT.toString().equals(keyString)) {
                    acc.x += PLAYER_ACC;
                } else if (KeyCode.UP.toString().equals(keyString) && onGround) {
                    // Impuls-Style: überschreibt vel.y, stapelt also nicht
                    if (grabbedGuy != null) {
                        vel.y = JUMP_FORCE / 2;
                    } else {
                        vel.y = JUMP_FORCE;
                    }
                    jumped = true;   // optional – ground-Flag reicht meist
                }


                // Grabbing Logic
                if (KeyCode.E.toString().equals(keyString)) {
                    // If an object is already grabbed, release it
                    if (grabbedGuy != null) {
                        if (grabbedGuy instanceof IGrabbable) {
                            IGrabbable grabbable = (IGrabbable) grabbedGuy;
                            grabbable.onRelease();  // Release the object
                            System.out.println("Released grabbed object: " + grabbedGuy.getName());
                        }
                        grabbedGuy = null;
                        return;
                    }

                    // Otherwise, find the nearest grabbable object
                    Game parentGame = getParentGame();
                    if (parentGame != null) {
                        IGrabbable closest = null;
                        double minDistance = Double.MAX_VALUE;
                        float myCenterX = getX() + getWidth() / 2;
                        float myCenterY = getY() + getHeight() / 2;

                        for (GameObject obj : parentGame.getGameObjects()) {
                            if (obj.getId().equals(getId())) continue;  // Skip the player's own object.
                            if (!(obj instanceof IGrabbable)) continue;  // Only consider grabbable objects.

                            IGrabbable candidate = (IGrabbable) obj;
                            if (candidate.isGrabbed()) continue;  // Skip objects that are already grabbed.

                            float candidateCenterX = obj.getX() + (obj.getWidth() / 2);
                            float candidateCenterY = obj.getY() + (obj.getHeight() / 2);
                            double dx = myCenterX - candidateCenterX;
                            double dy = myCenterY - candidateCenterY;
                            double distance = Math.sqrt(dx * dx + dy * dy);

                            if (distance < minDistance) {
                                minDistance = distance;
                                closest = candidate;
                            }
                        }

                        // Grab the object if it's within grab radius.
                        if (closest != null && minDistance <= GRAB_RADIUS) {
                            grabbedGuy = (GameObject) closest;  // Grab the object
                            grabbedGuy.onGrab(getId());  // Call onGrab method
                            System.out.println("Grabbed object at distance " + minDistance);
                        } else {
                            System.out.println("No object found within grab radius.");
                        }
                    }
                }

                // Toggle throwing mode
                else if (KeyCode.F.toString().equals(keyString)) {
                    if (!isThrowing) {
                        isThrowing = true;
                        throwAngle = 90f;
                        throwAngleDelta = 3.0f;
                        System.out.println("Entered throwing mode.");
                    } else {
                        isThrowing = false;
                        System.out.println("Exiting throwing mode.");
                    }
                }

                // Handle throw logic
                else if (KeyCode.R.toString().equals(keyString)) {
                    if (isThrowing && grabbedGuy != null) {
                        double rad = Math.toRadians(throwAngle);
                        float throwVx = (float) (throwMagnitude * Math.cos(rad));
                        float throwVy = (float) (throwMagnitude * Math.sin(rad));
                        throwVy = -throwVy;

                        // Check if the grabbed object implements `IGrabbable`
                        if (grabbedGuy instanceof IGrabbable) {
                            IGrabbable grabbableObject = (IGrabbable) grabbedGuy;
                            grabbableObject.setVelocity(throwVx, throwVy);
                            System.out.println("Thrown object with velocity: Vx=" + throwVx + ", Vy=" + throwVy);
                            // Release the grabbed object after throwing
                            grabbedGuy.onRelease();
                            grabbedGuy = null;
                        } else {
                            System.out.println("The grabbed object cannot be thrown.");
                        }

                        isThrowing = false;
                    }
                }
            }
        }

        else if ("SNAPSHOT".equals(type)) {
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

    @Override
    public void throwObject(float throwVx, float throwVy) {
        if (grabbedGuy != null) {
            if (grabbedGuy instanceof IGrabbable) {
                IGrabbable grabbableObject = (IGrabbable) grabbedGuy;
                grabbableObject.setVelocity(throwVx, throwVy);  // Set the velocity of the thrown object.
                System.out.println("Threw object with velocity: Vx=" + throwVx + ", Vy=" + throwVy);
                ((IGrabbable) grabbedGuy).onRelease();  // Release the object after throwing it.
                grabbedGuy = null;
            }
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
        // Draw the player rectangle.
        gc.setFill(Color.MEDIUMPURPLE);
        gc.fillRect(pos.x, pos.y, width, height);

        // Draw the player's name above the rectangle.
        gc.setFill(Color.BLACK);
        Text text = new Text(getName());
        double textWidth = text.getLayoutBounds().getWidth();
        gc.fillText(getName(), pos.x + width / 2 - textWidth / 2, pos.y - 5);

        // If in throwing mode, draw an indicator for the throw angle.
        if (isThrowing) {
            double centerX = pos.x + width / 2;
            double centerY = pos.y + height / 2;
            double indicatorLength = 50;  // Length of the throw indicator line.
            double rad = Math.toRadians(throwAngle);
            double endX = centerX + Math.cos(rad) * indicatorLength;
            double endY = centerY - Math.sin(rad) * indicatorLength; // Adjust if Y increases downward.
            gc.setStroke(Color.RED);
            gc.strokeLine(centerX, centerY, endX, endY);
        }
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

    @Override
    public void onGrab(String playerId) {
        // When grabbed, mark the player as grabbed.
        iAmGrabbed = true;
    }

    @Override
    public void onRelease() {
        iAmGrabbed = false;
    }

    @Override
    public boolean isGrabbed() {
        return false;
    }

    @Override
    public String getGrabbedBy() {
        return "";
    }

    /**
     * @param x
     * @param y
     */
    @Override
    public void setPos(float x, float y) {
        this.pos = new Vector2(x, y);
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



    /**
     * Client‑side keyboard polling.  Called every frame from myUpdateLocal().
     * Mirrors the logic you already have in myUpdateGlobal(KEY_PRESS),
     * but drives it directly from the static KeyboardState.
     */
    @Override
    public void processKeyboardState() {

        if (iAmGrabbed) {                      // still carried by somebody else
            return;
        }

        /* ------------------------------------------------------------
        *  Continuous actions  (while the key is held down)
        * ---------------------------------------------------------- */
        if (KeyboardState.isKeyPressed(KeyCode.LEFT)) {
            acc.x += -PLAYER_ACC;
        }
        if (KeyboardState.isKeyPressed(KeyCode.RIGHT)) {
            acc.x +=  PLAYER_ACC;
        }
        if (KeyboardState.isKeyPressed(KeyCode.UP)) {
            vel.y = JUMP_FORCE;
        }

        /* ------------------------------------------------------------
        *  Edge‑triggered actions  (once per key *press*)
        *
        *  We consider the moment the key is *released* as the
        *  end‑of‑press so we can use KeyboardState.getAndClearReleasedKeys().
        * ---------------------------------------------------------- */
        Set<KeyCode> released = KeyboardState.getAndClearReleasedKeys();

        /* ----------  Grab / release  -------------------------------- */
        if (released.contains(KeyCode.E)) {
            if (grabbedGuy != null) {                  // already holding → drop
                if (grabbedGuy instanceof IGrabbable) {
                    ((IGrabbable) grabbedGuy).onRelease();
                }
                grabbedGuy = null;
            } else {                                   // otherwise try to grab
                attemptGrabNearest();
            }
        }

        /* ----------  Toggle throwing mode  -------------------------- */
        if (released.contains(KeyCode.F)) {
            isThrowing = !isThrowing;
            if (isThrowing) {
                throwAngle      = 90f;                 // reset arc UI
                throwAngleDelta = 3.0f;
                System.out.println("Entered throwing mode.");
            } else {
                System.out.println("Exited throwing mode.");
            }
        }

        /* ----------  Perform the throw  ----------------------------- */
        if (released.contains(KeyCode.R)) {
            if (isThrowing && grabbedGuy != null) {
                performThrow();                        // helper below
                isThrowing = false;
            }
        }
    }

    /* --------------------------------------------------------------
    *  Helper: grab the closest IGrabbable within GRAB_RADIUS
    * ------------------------------------------------------------ */
    private void attemptGrabNearest() {
        Game parent = getParentGame();
        if (parent == null) return;

        IGrabbable closest = null;
        double     minDist = Double.MAX_VALUE;
        float      cx      = getX() + getWidth()  / 2f;
        float      cy      = getY() + getHeight() / 2f;

        for (GameObject obj : parent.getGameObjects()) {
            if (obj == this || !(obj instanceof IGrabbable)) continue;
            IGrabbable g = (IGrabbable) obj;
            if (g.isGrabbed()) continue;

            float ox = obj.getX() + obj.getWidth()  / 2f;
            float oy = obj.getY() + obj.getHeight() / 2f;
            double dx = cx - ox, dy = cy - oy;
            double d  = Math.sqrt(dx * dx + dy * dy);

            if (d < minDist) { minDist = d; closest = g; }
        }

        if (closest != null && minDist <= GRAB_RADIUS) {
            grabbedGuy = (GameObject) closest;
            closest.onGrab(getId());
            System.out.println("Grabbed object at distance " + minDist);
        } else {
            System.out.println("No object within grab radius.");
        }
    }

    /* --------------------------------------------------------------
    *  Helper: apply velocity to the grabbed object, then release it
    * ------------------------------------------------------------ */
    private void performThrow() {
        double rad      = Math.toRadians(throwAngle);
        float  throwVx  = (float) (throwMagnitude * Math.cos(rad));
        float  throwVy  = (float) (throwMagnitude * Math.sin(rad)) * -1f;

        if (grabbedGuy instanceof IGrabbable gObj) {
            gObj.setVelocity(throwVx, throwVy);
            gObj.onRelease();
            grabbedGuy = null;

            System.out.printf("Thrown object: Vx=%.2f Vy=%.2f%n", throwVx, throwVy);
        }
    }

}
