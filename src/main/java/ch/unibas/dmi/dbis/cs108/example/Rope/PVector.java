package ch.unibas.dmi.dbis.cs108.example.Rope;

/**
 * A simple 2D vector class for mathematical vector operations.
 */
public class PVector {
    float x, y;

    /**
     * Constructs a PVector with the given x and y components.
     *
     * @param x the x component
     * @param y the y component
     */
    public PVector(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Subtracts two vectors and returns the result as a new vector.
     *
     * @param v1 the first vector
     * @param v2 the second vector
     * @return the result of v1 - v2
     */
    public static PVector sub(PVector v1, PVector v2) {
        return new PVector(v1.x - v2.x, v1.y - v2.y);
    }

    /**
     * Adds two vectors and returns the result as a new vector.
     *
     * @param v1 the first vector
     * @param v2 the second vector
     * @return the result of v1 + v2
     */
    public static PVector add(PVector v1, PVector v2) {
        return new PVector(v1.x + v2.x, v1.y + v2.y);
    }

    /**
     * Sets the x and y components of the vector.
     *
     * @param x the new x component
     * @param y the new y component
     */
    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Multiplies both components of the vector by a scalar.
     *
     * @param scalar the value to multiply the vector by
     */
    public void mult(float scalar) {
        this.x *= scalar;
        this.y *= scalar;
    }

    /**
     * Sets the magnitude of the vector while keeping its direction.
     *
     * @param mag the new magnitude
     */
    public void setMag(float mag) {
        float currentMag = mag();
        if (currentMag != 0) {
            this.mult(mag / currentMag);
        }
    }

    /**
     * Returns the heading (angle) of the vector in radians.
     *
     * @return the angle of the vector
     */
    public float heading() {
        return (float) Math.atan2(y, x);
    }

    /**
     * Returns the magnitude (length) of the vector.
     *
     * @return the magnitude of the vector
     */
    public float mag() {
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * Creates and returns a copy of the vector.
     *
     * @return a new PVector with the same components
     */
    public PVector copy() {
        return new PVector(this.x, this.y);
    }
}