package ch.unibas.dmi.dbis.cs108.example;

import java.util.Scanner;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Nickname_Generator;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHoggerTest;
import javafx.application.Application;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.GUI;

public class Main {

    public static void main(String[] args) {
        // Launch client code in a separate thread.
        new Thread(() -> {
            try {
                // Get a nickname from the user or generate a suggested one.
                Scanner inputScanner = new Scanner(System.in);
                String suggestedNickname = Nickname_Generator.generateNickname();
                System.out.println("Suggested Nickname: " + suggestedNickname);
                System.out.println("Please press enter to use the suggested name, or type your own: ");
                String userName = inputScanner.nextLine();
                if (userName.isEmpty()) {
                    userName = suggestedNickname;
                }
                System.out.println("Entered nickname: " + userName);

                // Set up the client.
                Client client = new Client();
                client.setUsername(userName);
                System.out.println("Set client username: " + userName);

                // Start the MessageHoggerTest to send periodic CHECK messages.
                MessageHoggerTest messageHoggerTest = new MessageHoggerTest();
                messageHoggerTest.startSending();

                // Proceed with client operations.
                client.startConsoleReaderLoop();
                new Thread(client::run).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Launch the JavaFX GUI (this call blocks until the GUI is closed).
        Application.launch(GUI.class, args);
    }
}
