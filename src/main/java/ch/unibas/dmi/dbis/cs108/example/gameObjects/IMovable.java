package ch.unibas.dmi.dbis.cs108.example.gameObjects;

/**
 * Interface for objects that can be moved within the game world.
 * <p>
 * Implementing classes should update their position based on the elapsed time
 * since the last update.</p>
 */
public interface IMovable {

    /**
     * Moves the object according to its internal logic, using the time
     * elapsed since the last update to calculate displacement.
     *
     * @param deltaTime time in seconds since the last update call
     */
    void move(float deltaTime);
}
