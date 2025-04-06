package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.ThinkOutsideTheRoom;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GUI extends Application {


    @Override
    public void start(Stage stage) {
        System.out.println("Starting GUI");

        GameContext context = ThinkOutsideTheRoom.gameContext;
        if (context != null) {
            context.start(); // baut UI-Komponenten auf
            Platform.runLater(context::startGameLoop); // startet den Zeichenloop
        } else {
            System.err.println("Fehler: GameContext wurde nicht gesetzt!");
        }
        // Retrieve the singleton instance of the central graphical unit.
        CentralGraphicalUnit cgu = CentralGraphicalUnit.getInstance();

        // Optionally add a custom node (an HBox with a Label and a Button).
        //cgu.addNode(cgu.createCustomNode());

        // Create a Scene using the central graphical unit's main container.
        Scene scene = new Scene(cgu.getMainContainer(), 640, 480);

        // Set up the stage.
        stage.setScene(scene);
        stage.setTitle("Central Graphical Unit Example");
        stage.show();
    }
}
