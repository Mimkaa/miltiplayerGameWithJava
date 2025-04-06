package ch.unibas.dmi.dbis.cs108.example.gameObjects;

public interface IThrowable extends IGrabbable {
    /**
     * Called when the object is thrown.
     * @param throwVx The horizontal velocity of the throw.
     * @param throwVy The vertical velocity of the throw.
     */
    void throwObject(float throwVx, float throwVy);
}
