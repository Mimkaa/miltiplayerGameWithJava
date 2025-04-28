package ch.unibas.dmi.dbis.cs108.example.gameObjects;

/**
 * Interface for objects affected by gravity in the game world.
 * <p>
 * Implementing classes must provide mass information and
 * apply gravitational acceleration each update cycle.
 * </p>
 */
public interface IGravityAffected {

    /**
     * Returns the mass of this object.
     * <p>
     * Mass is used to calculate gravitational force and acceleration.
     * </p>
     *
     * @return the mass of the object in relevant units (e.g., kilograms)
     */
    float getMass();

    /**
     * Applies gravity to this object, updating its state based on elapsed time.
     * <p>
     * This method should calculate the gravitational acceleration (e.g., g = 9.81 m/s²)
     * and update the object's velocity and/or position accordingly.
     * </p>
     *
     * @param deltaTime time elapsed since the last update, in seconds
     */
    void applyGravity(float deltaTime);
}
