package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;

import java.util.Scanner;

/**
 * Main entry point for starting Server and Client instances individually or simultaneously.
 */
public class ThinkOutsideTheRoom {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Choose mode to run:");
        System.out.println("1. Server");
        System.out.println("2. Client");
        System.out.println("3. Server & Client");
        System.out.print("Your choice: ");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                System.out.println("Starting server...");
                Server.main(args);
                break;

            case "2":
                System.out.println("Starting client...");
                Client.main(args);
                break;

            case "3":
                System.out.println("Starting server and client simultaneously...");
                new Thread(() -> Server.main(args)).start();

                // Kleine Verzögerung, damit der Server sicher zuerst startet
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}

                Client.main(args);
                break;

            default:
                System.out.println("Invalid choice. Exiting.");
        }
    }
}

