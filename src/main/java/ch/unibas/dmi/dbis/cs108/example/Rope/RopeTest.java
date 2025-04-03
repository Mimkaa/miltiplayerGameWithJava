package ch.unibas.dmi.dbis.cs108.example.Rope;

import ch.unibas.dmi.dbis.cs108.example.Rope.Segment;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * A JavaFX application that visualizes a rope made of connected segments.
 * Each segment follows the position of the next one, and the rope follows the mouse cursor.
 */
public class RopeTest extends Application {

    /** Number of segments in the rope */
    private final int SEGMENT_COUNT = 15;

    /** Length of each rope segment */
    private final float SEGMENT_LENGTH = 20;

    /** Array to hold the rope segments */
    private Segment[] segments;

    /**
     * Entry point for the JavaFX application. Sets up the canvas and rope animation.
     *
     * @param stage the primary stage for this application
     */
    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Rope-array
        segments = new Segment[SEGMENT_COUNT];
        segments[0] = new Segment(400, 300, SEGMENT_LENGTH, 0);
        for (int i = 1; i < SEGMENT_COUNT; i++) {
            segments[i] = new Segment(segments[i - 1], SEGMENT_LENGTH, 0);
        }

        // to move with the mouse
        canvas.setOnMouseMoved(e -> {
            float mouseX = (float) e.getX();
            float mouseY = (float) e.getY();

            // put the connection (position)
            segments[SEGMENT_COUNT - 1].follow(mouseX, mouseY);
            for (int i = SEGMENT_COUNT - 2; i >= 0; i--) {
                segments[i].follow(segments[i + 1].a.x, segments[i + 1].a.y);
            }

            // puts the angle that each segments follows another
            for (int i = 1; i < SEGMENT_COUNT; i++) {
                segments[i].a = segments[i - 1].b.copy();
                segments[i].calculateB();
            }

            // drawing
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            for (Segment s : segments) {
                s.show(gc);
            }
        });

        Pane root = new Pane(canvas);
        stage.setScene(new Scene(root));
        stage.setTitle("Rope with segments");
        stage.show();
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch();
    }
}
