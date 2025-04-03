package ch.unibas.dmi.dbis.cs108.example.Rope;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Represents a segment of a rope, defined by two points (a and b), a length, and an angle.
 * Segments can be connected hierarchically via a parent segment.
 */
public class Segment {
    /** Starting point of the segment */
    public PVector a;

    /** Ending point of the segment */
    public PVector b;

    /** Angle of the segment in radians */
    private float angle;

    /** Length of the segment */
    private float len;

    /** Optional parent segment that this segment follows */
    private Segment parent;

    /**
     * Constructs a root segment at the given position.
     *
     * @param x the x coordinate of the starting point
     * @param y the y coordinate of the starting point
     * @param len the length of the segment
     * @param angle the angle of the segment in radians
     */
    public Segment(float x, float y, float len, float angle) {
        this.a = new PVector(x, y);
        this.b = new PVector(0,0); // must add pVector without parameters
        this.angle = angle;
        this.len = len;
        calculateB();
    }

    /**
     * Constructs a child segment that is connected to a parent segment.
     *
     * @param parent the parent segment
     * @param len the length of the segment
     * @param angle the initial angle of the segment
     */
    public Segment(Segment parent, float len, float angle) {
        this.parent = parent;
        this.a = parent.b.copy();
        this.b = new PVector(0,0);
        this.angle = angle;
        this.len = len;
        calculateB();
    }

    /**
     * Calculates the end point (b) of the segment based on its start point (a), angle, and length.
     */
    public void calculateB() {
        float dx = (float) (len * Math.cos(angle));
        float dy = (float) (len * Math.sin(angle));
        b.set(this.a.x + dx, this.a.y + dy);
    }

    /**
     * Updates the segment's position based on its parent.
     * If a parent exists, the start point (a) is set to the end point of the parent.
     */
    public void update() {
        if (parent != null) {
            a = parent.b.copy();
        }
        calculateB();
    }

    /**
     * Makes the segment follow a target position by rotating and translating itself accordingly.
     *
     * @param targetX the x coordinate of the target
     * @param targetY the y coordinate of the target
     */
    public void follow(float targetX, float targetY) {
        PVector target = new PVector(targetX, targetY);
        PVector dir = PVector.sub(target, a);
        angle = dir.heading();
        dir.setMag(len);
        dir.mult(-1);
        a = PVector.add(target, dir);
        calculateB();
    }

    /**
     * Draws the segment on the canvas using the given graphics context.
     *
     * @param gc the graphics context used for drawing
     */
    public void show(GraphicsContext gc) {
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(10);
        gc.strokeLine(a.x, a.y, b.x, b.y);
    }
}
