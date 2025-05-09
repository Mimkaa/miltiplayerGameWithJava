package ch.unibas.dmi.dbis.cs108.example.gui.javafx;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UIManager {
    // Use a ConcurrentHashMap for thread-safe access to the component registry.
    private final Map<String, Node> componentRegistry = new ConcurrentHashMap<>();

    // Regular constructor; no singleton pattern here.
    public UIManager() {}

    // Register a component with a unique key.
    public void registerComponent(String key, Node component) {
        componentRegistry.put(key, component);
    }

    // Get a component by its key.
    public Node getComponent(String key) {
        return componentRegistry.get(key);
    }

    // Update a text field by key.
    public void updateTextField(String key, String text) {
        Platform.runLater(() -> {
            Node node = componentRegistry.get(key);
            if (node instanceof TextField) {
                ((TextField) node).setText(text);
            }
        });
    }


    public void initializeUI(Runnable customInit) {
        // --- Execute custom initialization logic, if provided.
        Platform.runLater(() -> {


        if (customInit != null) {
            customInit.run();
        }
        });
    }

    public void hideAllComponents() {
        Platform.runLater(() -> {
            for (Node node : componentRegistry.values()) {
                if (node != null) {
                    node.setVisible(false);
                }
            }
        });
    }

    /**
     * Waits until CentralGraphicalUnit is ready, then calls initializeUI(customInit).
     *
     * @param customInit a Runnable containing custom UI initialization logic (can be null)
     */
    public void waitForCentralUnitAndInitialize(Runnable customInit) {
        new Thread(() -> {
            while (CentralGraphicalUnit.getInstance() == null) {
                try {
                    Thread.sleep(100); // wait for 100 ms before checking again.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            initializeUI(customInit);
            System.out.println("CentralGraphicalUnit is ready. UI initialized.");
        }).start();
    }
}
