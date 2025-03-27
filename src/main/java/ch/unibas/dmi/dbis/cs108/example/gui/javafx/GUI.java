package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
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
