package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

/**
 * Represents a snapshot of a target's state at a specific point in time.
 * This class is used to store position and timing information for synchronization
 * and state reconstruction purposes.
 *
 * <p>Typical use cases include:
 * <ul>
 *   <li>Network synchronization of game objects</li>
 *   <li>State reconstruction for lag compensation</li>
 *   <li>Temporal interpolation between states</li>
 * </ul>
 */
public class SnapshotState {
    /** The x-coordinate of the target's position in this snapshot */
    public float targetX;

    /** The y-coordinate of the target's position in this snapshot */
    public float targetY;

    /**
     * The timestamp when this snapshot was taken, typically in milliseconds
     * since epoch or relative to some game clock
     */
    public long timestamp;

    /**
     * Constructs a new SnapshotState with the specified position and timestamp.
     *
     * @param targetX   the x-coordinate of the target's position
     * @param targetY   the y-coordinate of the target's position
     * @param timestamp the time when this snapshot was taken
     */
    public SnapshotState(float targetX, float targetY, long timestamp) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.timestamp = timestamp;
    }
}