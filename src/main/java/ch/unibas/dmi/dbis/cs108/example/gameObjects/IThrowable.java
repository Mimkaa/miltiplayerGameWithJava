package ch.unibas.dmi.dbis.cs108.example.gameObjects;

/**
 * Interface for objects that can be grabbed and then thrown by players.
 * <p>
 * Extends {@link IGrabbable} to include a throwing action with specified velocity.
 * </p>
 */
public interface IThrowable extends IGrabbable {

    /**
     * Called when the object is thrown by a player or entity.
     * Implementations should apply the given velocities to the object’s motion.
     *
     * @param throwVx the horizontal component of the throw velocity
     * @param throwVy the vertical component of the throw velocity
     */
    void throwObject(float throwVx, float throwVy);
}
