package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.ThinkOutsideTheRoom;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Launches the JavaFX GUI and sizes the window to the current screen bounds
 * to avoid Canvas reallocation lag when maximizing.
 */
public class GUI extends Application {

    @Override
    public void start(Stage stage) {
        System.out.println("Starting GUI");

        GameContext context = ThinkOutsideTheRoom.gameContext;
        if (context != null) {
            context.start();             // build UI components
            Platform.runLater(context::startGameLoop);
        } else {
            System.err.println("Fehler: GameContext wurde nicht gesetzt!");
        }

        // Get CentralGraphicalUnit root
        CentralGraphicalUnit cgu = CentralGraphicalUnit.getInstance();

        // Determine usable screen size (excludes taskbar)
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double screenW = bounds.getWidth();
        double screenH = bounds.getHeight();

        // Create scene at full-screen size (without toggling maximize)
        Scene scene = new Scene(cgu.getMainContainer(), screenW, screenH);

        stage.setTitle("Central Graphical Unit Example");
        stage.setScene(scene);

        // Position window at top-left of visual bounds
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());

        // Show window
        //stage.setMaximized(true);
        stage.show();

        // Ensure focus for keyboard events
        Platform.runLater(() -> cgu.getMainContainer().requestFocus());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
