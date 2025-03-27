package ch.unibas.dmi.dbis.cs108.example;

import java.util.Scanner;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Nickname_Generator;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHoggerTest;

/**
 * A simple Main class that starts the client and a periodic CHECK message sender.
 */
public class Main {

    public static void main(String[] args) {
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

            // Start the MessageHoggerTest to send CHECK messages every 1000ms on a separate thread.
            MessageHoggerTest messageHoggerTest = new MessageHoggerTest();
            messageHoggerTest.startSending();
            //MessageHoggerTest messageHoggerTest2 = new MessageHoggerTest();
            //messageHoggerTest2.startSending();

            // Proceed with client operations.
            client.startConsoleReaderLoop();
            new Thread(client::run).start();
            Thread.sleep(1000);
            // Optionally, you could also invoke client.login() or other methods here.
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
