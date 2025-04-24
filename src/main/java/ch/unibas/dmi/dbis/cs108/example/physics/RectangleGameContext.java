package ch.unibas.dmi.dbis.cs108.example.physics;

import ch.unibas.dmi.dbis.cs108.example.gui.javafx.CentralGraphicalUnit;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.GUI;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class RectangleGameContext {

    // Rectangle 1 properties
    private double rect1X = 200;
    private double rect1Y = 150;
    private double rect1Width = 120;
    private double rect1Height = 60;
    // Velocity for rect1 (pixels per frame)
    private double rect1VX = 2;
    private double rect1VY = 1;

    // Rectangle 2 properties
    private double rect2X = 240;
    private double rect2Y = 140;
    private double rect2Width = 150;
    private double rect2Height = 80;
    // Velocity for rect2
    private double rect2VX = -1;
    private double rect2VY = -2;

    // Flag indicating collision state (for visual feedback)
    private boolean collisionDetected = false;

    /**
     * Initializes the UI (via CentralGraphicalUnit).
     */
    public void start() {
        // Ensure CentralGraphicalUnit is initialized.
        CentralGraphicalUnit.getInstance();
        System.out.println("RectangleGameContext UI initialized.");
    }

    /**
     * Starts the game loop that updates positions, resolves collisions, applies wrap-around,
     * and redraws the scene.
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
     * Updates the rectangles’ positions, resolves collisions, and applies wrap-around.
     */
    private void update() {
        GraphicsContext gc = CentralGraphicalUnit.getInstance().getGraphicsContext();
        double canvasWidth = gc.getCanvas().getWidth();
        double canvasHeight = gc.getCanvas().getHeight();

        System.out.println("Rect1 Position: " + rect1X + ", " + rect1Y);
        System.out.println("Rect2 Position: " + rect2X + ", " + rect2Y);

        // 1. Update positions based on velocity.
        rect1X += rect1VX;
        rect1Y += rect1VY;

        rect2X += rect2VX;
        rect2Y += rect2VY;

        // 2. Resolve collision if they overlap.
        if (checkCollision()) {
            collisionDetected = true;
            resolveCollision();
        } else {
            collisionDetected = false;
        }

        // 3. Apply wrap-around for rect1.
        if (rect1X > canvasWidth) {
            rect1X = -rect1Width;
        } else if (rect1X + rect1Width < 0) {
            rect1X = canvasWidth;
        }
        if (rect1Y > canvasHeight) {
            rect1Y = -rect1Height;
        } else if (rect1Y + rect1Height < 0) {
            rect1Y = canvasHeight;
        }

        // 4. Apply wrap-around for rect2.
        if (rect2X > canvasWidth) {
            rect2X = -rect2Width;
        } else if (rect2X + rect2Width < 0) {
            rect2X = canvasWidth;
        }
        if (rect2Y > canvasHeight) {
            rect2Y = -rect2Height;
        } else if (rect2Y + rect2Height < 0) {
            rect2Y = canvasHeight;
        }
    }

    /**
     * Checks if the two rectangles intersect using axis-aligned bounding boxes.
     * @return true if they intersect, false otherwise.
     */
    private boolean checkCollision() {
        return rect1X < rect2X + rect2Width &&
                rect1X + rect1Width > rect2X &&
                rect1Y < rect2Y + rect2Height &&
                rect1Y + rect1Height > rect2Y;
    }

    /**
     * Resolves the collision by calculating the overlap on the x and y axes
     * and then pushing the rectangles apart equally along the axis with the least penetration.
     */
    private void resolveCollision() {
        // Calculate the horizontal and vertical overlap.
        double overlapX = Math.min(rect1X + rect1Width, rect2X + rect2Width) - Math.max(rect1X, rect2X);
        double overlapY = Math.min(rect1Y + rect1Height, rect2Y + rect2Height) - Math.max(rect1Y, rect2Y);

        // Determine the axis of minimum penetration.
        if (overlapX < overlapY) {
            // Move rect1 to the left or right by half the overlap, and rect2 in the opposite direction.
            if (rect1X < rect2X) {
                rect1X -= overlapX / 2;
                rect2X += overlapX / 2;
            } else {
                rect1X += overlapX / 2;
                rect2X -= overlapX / 2;
            }
        } else {
            // Move rect1 up or down by half the overlap, and rect2 in the opposite direction.
            if (rect1Y < rect2Y) {
                rect1Y -= overlapY / 2;
                rect2Y += overlapY / 2;
            } else {
                rect1Y += overlapY / 2;
                rect2Y -= overlapY / 2;
            }
        }
    }

    /**
     * Draws the current state: clears the canvas and draws both rectangles.
     * Colors change when a collision is detected.
     */
    private void draw(GraphicsContext gc) {
        // Clear the canvas.
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        // Draw rectangle 1.
        if (collisionDetected) {
            gc.setFill(Color.GREEN);
        } else {
            gc.setFill(Color.BLUE);
        }
        gc.fillRect(rect1X, rect1Y, rect1Width, rect1Height);

        // Draw rectangle 2.
        if (collisionDetected) {
            gc.setFill(Color.CYAN);
        } else {
            gc.setFill(Color.RED);
        }
        gc.fillRect(rect2X, rect2Y, rect2Width, rect2Height);
    }

    public static void main(String[] args) {
        RectangleGameContext context = new RectangleGameContext();
        new Thread(() -> {
            context.start();
            Platform.runLater(() -> context.startGameLoop());
        }).start();

        // Launch the JavaFX GUI that provides the CentralGraphicalUnit.
        Application.launch(GUI.class, args);
    }
}
