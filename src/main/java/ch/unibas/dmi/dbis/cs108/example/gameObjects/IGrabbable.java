package ch.unibas.dmi.dbis.cs108.example.gameObjects;

/**
 * Interface for game objects that can be grabbed and moved by players.
 * <p>
 * Implementing classes support setting velocity, responding to grab/release events,
 * and tracking the grabbing player.</p>
 */
public interface IGrabbable {

    /**
     * Sets the current velocity of this object.
     *
     * @param vx velocity in x-direction (units per second)
     * @param vy velocity in y-direction (units per second)
     */
    void setVelocity(float vx, float vy);

    /**
     * Called when a player grabs this object.
     *
     * @param playerId identifier of the grabbing player
     */
    void onGrab(String playerId);

    /**
     * Called when this object is released by a player.
     */
    void onRelease();

    /**
     * Checks whether this object is currently grabbed.
     *
     * @return true if grabbed, false otherwise
     */
    boolean isGrabbed();

    /**
     * Retrieves the identifier of the player who has grabbed this object.
     *
     * @return playerId of the grabbing player, or null if not grabbed
     */
    String getGrabbedBy();

    /**
     * Sets the position of this object.
     *
     * @param x new x-coordinate
     * @param y new y-coordinate
     */
    void setPos(float x, float y);

    /**
     * Retrieves the display name of this object.
     *
     * @return name of the object
     */
    String getName();
}
