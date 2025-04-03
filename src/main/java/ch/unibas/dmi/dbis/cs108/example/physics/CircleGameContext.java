package ch.unibas.dmi.dbis.cs108.example.physics;

import ch.unibas.dmi.dbis.cs108.example.gui.javafx.CentralGraphicalUnit;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.GUI;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class CircleGameContext {

    // Positions and radii for the circles.
    private double circleX = 200;
    private double circleY = 150;
    private double circleRadius = 40;

    private double circle2X = 240;
    private double circle2Y = 140;
    private double circleRadius2 = 40;

    // Flag to indicate if a collision has been detected.
    private boolean collisionDetected = false;

    /**
     * Initializes the UI (via CentralGraphicalUnit).
     */
    public void start() {
        // Ensure the CentralGraphicalUnit is initialized.
        CentralGraphicalUnit.getInstance();
        System.out.println("CircleGameContext UI initialized.");
    }

    /**
     * Starts the game loop that updates positions and redraws the scene.
     */
    public void startGameLoop() {
        AnimationTimer timer = new AnimationTimer() {
            GraphicsContext gc = CentralGraphicalUnit.getInstance().getGraphicsContext();

            @Override
            public void handle(long now) {
                update();
                draw(gc);
            }
        };
        timer.start();
    }

    /**
     * Updates the positions of the circles and checks for collisions.
     */
    private void update() {
        // Move the first circle to the right.
        circleX += 1;
        if (circleX - circleRadius > CentralGraphicalUnit.getInstance().getGraphicsContext().getCanvas().getWidth()) {
            circleX = -circleRadius; // reset if off screen
        }
        // Move the second circle upward.
        circle2Y -= 1;
        if (circle2Y - circleRadius2<0) {
            circle2Y = CentralGraphicalUnit.getInstance().getGraphicsContext().getCanvas().getHeight(); // reset if off screen
        }


        // Check for collision between the two circles.
        collisionDetected = checkCollision();
        if (collisionDetected) {
            System.out.println("Collision detected!");
        }
    }

    /**
     * Performs a simple circle–circle collision check.
     * @return true if the distance between centers is less than the sum of the radii.
     */
    private boolean checkCollision() {
        double dx = circleX - circle2X;
        double dy = circleY - circle2Y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance < (circleRadius + circleRadius2);
    }

    /**
     * Draws the two circles on the canvas.
     * When a collision is detected, the first circle is drawn in green instead of blue.
     */
    private void draw(GraphicsContext gc) {
        // Clear the canvas.
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        // Draw the first circle.
        if (collisionDetected) {
            gc.setFill(Color.GREEN); // Change color on collision.
        } else {
            gc.setFill(Color.BLUE);
        }
        gc.fillOval(circleX - circleRadius, circleY - circleRadius, circleRadius * 2, circleRadius * 2);

        // Draw the second circle.

        // Draw the first circle.
        if (collisionDetected) {
            gc.setFill(Color.CYAN); // Change color on collision.
        } else {
            gc.setFill(Color.RED);
        }
        gc.fillOval(circle2X - circleRadius2, circle2Y - circleRadius2, circleRadius2 * 2, circleRadius2 * 2);
    }

    /**
     * Main method: starts the game context and launches the GUI.
     */
    public static void main(String[] args) {
        CircleGameContext context = new CircleGameContext();
        // Start the context and game loop on a separate thread.
        new Thread(() -> {
            context.start();
            Platform.runLater(() -> context.startGameLoop());
        }).start();

        // Launch the JavaFX GUI which provides the CentralGraphicalUnit.
        Application.launch(GUI.class, args);
    }
}
