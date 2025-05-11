package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.KeyboardState;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.SoundManager;
import ch.unibas.dmi.dbis.cs108.example.ThinkOutsideTheRoom;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.Player2;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Singleton class representing the central graphical unit of the JavaFX-based game.
 * <p>
 * This class manages a resizable {@link Canvas} overlaid on a {@link StackPane},
 * handles global keyboard input events, and provides access to the {@link GraphicsContext}.
 * </p>
 *
 * <p>Responsibilities include:</p>
 * <ul>
 *     <li>Providing access to a central drawing surface</li>
 *     <li>Registering keyboard input via {@link KeyboardState}</li>
 *     <li>Allowing dynamic resizing and node layering in the game window</li>
 * </ul>
 */
public class CentralGraphicalUnit {
    // Eager initialization: the instance is created immediately.
    private static final CentralGraphicalUnit instance = new CentralGraphicalUnit();

    private final StackPane mainContainer; // Use StackPane for overlaying components.
    private final Canvas canvas;
    private final GraphicsContext graphicsContext;

    private boolean jumpPlayed = false;
    private boolean grabbPlayed = false;
    private boolean throwPlayed = false;


    // Private constructor to prevent external instantiation.
    private CentralGraphicalUnit() {
        mainContainer = new StackPane();
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double screenW = bounds.getWidth();
        double screenH = bounds.getHeight();

        // create the canvas at exactly that size
        canvas = new Canvas(screenW, screenH);
        

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

    private void setupKeyboardHandlers() {
        // Ensure the container is focus traversable and request focus.
        mainContainer.setFocusTraversable(true);
        mainContainer.requestFocus();
    
        // When a key is pressed, update KeyboardState and send a KEY_PRESS message.
        mainContainer.setOnKeyPressed((KeyEvent event) -> {
            KeyCode code = event.getCode();
            System.out.println("CentralGraphicalUnit - Key pressed: " + code);
            KeyboardState.keyPressed(event.getCode());

            // for Sound Effect:
            if (code == KeyCode.UP && !jumpPlayed) {
                    SoundManager.playJump();
                    jumpPlayed = true;
            }
            if (code == KeyCode.E) {
                SoundManager.playGrabb();
                grabbPlayed = true;
            }

            if (code == KeyCode.R /*&& !throwPlayed && isLocalPlayerHolding()*/) {
                SoundManager.playThrow();
                throwPlayed = true;
            }

        });

        // When a key is released, update KeyboardState and send a KEY_RELEASE message.
        mainContainer.setOnKeyReleased((KeyEvent event) -> {
            KeyCode code = event.getCode();
            //For Sound Effect:
            if (code == KeyCode.UP) {
                jumpPlayed = false;
            }
            if (code == KeyCode.E) {
                grabbPlayed = false;
            }
            if (code == KeyCode.R) {
                throwPlayed = false;
            }
            System.out.println("CentralGraphicalUnit - Key released: " + code);
            KeyboardState.keyReleased(event.getCode());

        });

    }

    private boolean isLocalPlayerHolding() {
        String gameId     = GameContext.getCurrentGameId();
        String selectedId = GameContext.getSelectedGameObjectId();
        if (gameId == null || selectedId == null) return false;
        Game game = GameContext.getInstance()
                .getGameSessionManager()
                .getGameSession(gameId);
        if (game == null) return false;
        for (GameObject go : game.getGameObjects()) {
            if (selectedId.equals(go.getId()) && go instanceof Player2) {
                Player2 me = (Player2)go;
                return me.isGrabbed();              // <-- now returns true when grabbed
                // or: return me.getGrabbedGuy() != null;
            }
        }
        return false;
    }



}
