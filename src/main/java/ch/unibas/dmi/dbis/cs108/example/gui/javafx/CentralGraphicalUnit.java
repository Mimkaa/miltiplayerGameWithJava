package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class CentralGraphicalUnit {
    private static CentralGraphicalUnit instance;
    
    private VBox mainContainer;
    private GraphicsContext graphicsContext;
    
    private CentralGraphicalUnit() {
        mainContainer = new VBox();
        Canvas canvas = new Canvas(400, 400);
        graphicsContext = canvas.getGraphicsContext2D();
        mainContainer.getChildren().add(canvas);
    }
    
    public static CentralGraphicalUnit getInstance() {
        if (instance == null) {
            instance = new CentralGraphicalUnit();
        }
        return instance;
    }
    
    public VBox getMainContainer() {
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
}
