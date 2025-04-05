package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class CentralGraphicalUnit {
    // Eager initialization: the instance is created immediately.
    private static final CentralGraphicalUnit instance = new CentralGraphicalUnit();

    private final StackPane mainContainer; // Use StackPane for overlaying components.
    private final Canvas canvas;
    private final GraphicsContext graphicsContext;

    // Private constructor to prevent external instantiation.
    private CentralGraphicalUnit() {
        mainContainer = new StackPane();
        canvas = new Canvas(400, 400);

        // Bind the canvas size to the StackPane size for dynamic resizing.
        canvas.widthProperty().bind(mainContainer.widthProperty());
        canvas.heightProperty().bind(mainContainer.heightProperty());

        // Ensure the canvas is focusable.
        canvas.setFocusTraversable(true);
        canvas.requestFocus();

        graphicsContext = canvas.getGraphicsContext2D();

        // Add the canvas to the main container.
        mainContainer.getChildren().add(canvas);

        // Setup keyboard event handlers on the container.
        setupKeyboardHandlers();

        // When the user clicks anywhere in the container, request focus.
        mainContainer.setOnMouseClicked(e -> mainContainer.requestFocus());

        System.out.println("CentralGraphicalUnit instance created eagerly with keyboard handlers.");
    }

    // ================================
    // Static Accessors
    // ================================

    public static CentralGraphicalUnit getInstance() {
        return instance;
    }

    /**
     * Returns the current width of the mainContainer StackPane, or 0.0 if not laid out yet.
     */
    public static double getContainerWidth() {
        return instance.mainContainer.getWidth();
    }

    /**
     * Returns the current height of the mainContainer StackPane, or 0.0 if not laid out yet.
     */
    public static double getContainerHeight() {
        return instance.mainContainer.getHeight();
    }

    /**
     * Returns the current window (Stage) width if available, otherwise 0.0 if the pane is not yet attached to a Scene.
     */
    public static double getWindowWidth() {
        if (instance.mainContainer.getScene() != null && instance.mainContainer.getScene().getWindow() != null) {
            return instance.mainContainer.getScene().getWindow().getWidth();
        }
        return 0.0;
    }

    /**
     * Returns the current window (Stage) height if available, otherwise 0.0 if the pane is not yet attached to a Scene.
     */
    public static double getWindowHeight() {
        if (instance.mainContainer.getScene() != null && instance.mainContainer.getScene().getWindow() != null) {
            return instance.mainContainer.getScene().getWindow().getHeight();
        }
        return 0.0;
    }

    // ================================
    // Non-Static Methods
    // ================================

    public StackPane getMainContainer() {
        return mainContainer;
    }

    public GraphicsContext getGraphicsContext() {
        return graphicsContext;
    }

    // Adds a Node to the central container.
    public void addNode(Node node) {
        mainContainer.getChildren().add(node);
    }

    // Creates a custom Node with additional FX components.
    public Node createCustomNode() {
        HBox customBox = new HBox(10);
        Label label = new Label("Custom Label:");
        Button button = new Button("Click Me");
        customBox.getChildren().addAll(label, button);
        return customBox;
    }

    // Setup keyboard event handlers so that the container receives focus and key events.
    private void setupKeyboardHandlers() {
        // Ensure the container is focusTraversable.
        mainContainer.setFocusTraversable(true);
        // Request focus (this works once the container is added to a scene).
        mainContainer.requestFocus();

        // When a key is pressed, update the global KeyboardState.
        mainContainer.setOnKeyPressed((KeyEvent event) -> {
            System.out.println("CentralGraphicalUnit - Key pressed: " + event.getCode());
            KeyboardState.keyPressed(event.getCode());
        });

        // When a key is released, update the global KeyboardState.
        mainContainer.setOnKeyReleased((KeyEvent event) -> {
            System.out.println("CentralGraphicalUnit - Key released: " + event.getCode());
            KeyboardState.keyReleased(event.getCode());
        });
    }
}
