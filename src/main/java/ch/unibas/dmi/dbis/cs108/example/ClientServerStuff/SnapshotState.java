package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

/**
 * Helper class for storing the target state from a snapshot.
 */
public class SnapshotState {
    public float targetX;
    public float targetY;
    public long timestamp; // when this snapshot was taken

    public SnapshotState(float targetX, float targetY, long timestamp) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.timestamp = timestamp;
    }

}