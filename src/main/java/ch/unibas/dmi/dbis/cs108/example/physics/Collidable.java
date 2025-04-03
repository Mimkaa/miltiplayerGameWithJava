package ch.unibas.dmi.dbis.cs108.example.physics;

import javafx.geometry.Rectangle2D;

public interface Collidable {
    /**
     * Returns the bounding box for collision detection.
     */
    Rectangle2D getBounds();

    /**
     * Checks whether this object intersects another collidable object.
     */
    default boolean intersects(Collidable other) {
        return getBounds().intersects(other.getBounds());
    }
}
