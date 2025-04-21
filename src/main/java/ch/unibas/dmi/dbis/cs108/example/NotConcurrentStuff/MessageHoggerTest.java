package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.CentralGraphicalUnit;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.UIManager;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.Random;
import java.util.UUID;

/**
 * Test class to demonstrate the usage of a custom {@link MessageHogger}
 * and interaction with the JavaFX-based UI via {@link CentralGraphicalUnit}.
 * <p>
 * This class initializes a simple UI with input and display components,
 * processes incoming "CHECK" messages to update the UI and draw randomly on the canvas,
 * and periodically sends messages from a background thread.
 * </p>
 */
public class MessageHoggerTest {

    // Declare the hogger as an instance attribute.
    private MessageHogger testHogger;
    
    // Local instance of UIManager.
    private final UIManager uiManager = new UIManager();


    /**
     * Constructs the test environment:
     * - Initializes UI components using {@link CentralGraphicalUnit}
     * - Registers all components in {@link UIManager}
     * - Sets up a MessageHogger that listens for "CHECK" messages
     *   and performs UI updates and canvas drawing accordingly.
     */
    public MessageHoggerTest() {
        // Start the UI initialization listener.
        // Wait for CentralGraphicalUnit and initialize the UI with custom initialization.
        uiManager.waitForCentralUnitAndInitialize(() -> {
            Platform.runLater(() -> {
                // Create and add an initial label.
                Label initLabel = new Label("UI Initialized");
                //CentralGraphicalUnit.getInstance().addNode(initLabel);
                uiManager.registerComponent("initLabel", initLabel);
    
                // Create and add a text area for user input.
                TextArea inputArea = new TextArea();
                inputArea.setPrefRowCount(5);
                inputArea.setPrefColumnCount(20);
                inputArea.setPromptText("Enter text here...");
                //CentralGraphicalUnit.getInstance().addNode(inputArea);
                uiManager.registerComponent("inputArea", inputArea);
    
                // Create and add a text field for displaying text.
                TextField displayField = new TextField();
                displayField.setEditable(false);
                displayField.setPromptText("Display text here...");
                //CentralGraphicalUnit.getInstance().addNode(displayField);
                uiManager.registerComponent("displayField", displayField);
    
                // Create and add a button to copy text from the input area to the display field.
                Button copyButton = new Button("Copy Text");
                copyButton.setOnAction(e -> {
                    String inputText = inputArea.getText();
                    displayField.setText(inputText);
                });
                //CentralGraphicalUnit.getInstance().addNode(copyButton);
                uiManager.registerComponent("copyButton", copyButton);
            });
        });

        // Initialize the custom MessageHogger.
        testHogger = new MessageHogger() {
            @Override
            protected void processMessage(Message msg) {
                if ("CHECK".equals(msg.getMessageType())) {
                    System.out.println("CheckMessage: " + msg);
                    Platform.runLater(() -> {
                        // Retrieve the TextArea from our local uiManager by its key.
                        Object comp = uiManager.getComponent("inputArea");
                        if (comp instanceof TextArea) {
                            TextArea inputArea = (TextArea) comp;
                            // Set the text from the message parameter.
                            inputArea.setText(msg.getParameters()[0].toString());
                        }
                        // Retrieve the GraphicsContext from CentralGraphicalUnit.
                        GraphicsContext gc = CentralGraphicalUnit.getInstance().getGraphicsContext();
                        // Create a Random instance.
                        Random random = new Random();
                        // Generate random coordinates between 0 and 150.
                        int x = random.nextInt(151);
                        int y = random.nextInt(151);
                        // Clear the entire canvas (assuming dimensions 400x400).
                        gc.clearRect(0, 0, 400, 400);
                        // Draw a circle at the random coordinates with a width and height of 50.
                        gc.strokeOval(x, y, 50, 50);
                    });
                }
            }
        };
    }
    /**
     * Starts a background thread that sends a "CHECK" message once per second.
     * The message contains a random UUID string as its content.
     */
    public void startSending() {
        Thread senderThread = new Thread(() -> {
            while (true) {
                // Create a new test message with a random UUID string as parameter.
                Message testMessage = new Message("CHECK", new Object[]{UUID.randomUUID().toString()}, "REQUEST");
                Client.sendMessageStatic(testMessage);
                try {
                    Thread.sleep(1000); // Sleep for 1000 milliseconds (1 second)
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        senderThread.start();
    }


    /**
     * Main entry point to start the MessageHoggerTest.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        MessageHoggerTest test = new MessageHoggerTest();
        test.startSending();
    }
}
