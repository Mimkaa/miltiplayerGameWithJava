package ch.unibas.dmi.dbis.cs108.example.gameObjects;

public class GravityEngine {
    // Gravity constant tuned for 2D platformer feel (in pixels/s²).
    public static final float GRAVITY = 1200.0f;

    /**
     * Applies gravity to each object that is gravity-affected.
     *
     * @param gameObjects an Iterable collection of GameObject instances.
     * @param deltaTime   time elapsed (in seconds) since the last update.
     */
    public static void updateGravity(Iterable<GameObject> gameObjects, float deltaTime) {
        for (GameObject obj : gameObjects) {
            if (obj instanceof IGravityAffected) {
                ((IGravityAffected) obj).applyGravity(deltaTime);
            }
        }
    }
}
