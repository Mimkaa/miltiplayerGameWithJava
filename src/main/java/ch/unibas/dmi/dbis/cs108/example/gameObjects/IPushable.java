package ch.unibas.dmi.dbis.cs108.example.gameObjects;

/**
 * Interface for objects that can respond to push events in the game world.
 * <p>
 * Implementing classes should define custom behavior when a push occurs,
 * such as changing position or triggering game logic.
 * </p>
 */
public interface IPushable {

    /**
     * Called when this object is pushed by another entity or force.
     * Implementations should update object state or position accordingly.
     */
    void onPush();
}
