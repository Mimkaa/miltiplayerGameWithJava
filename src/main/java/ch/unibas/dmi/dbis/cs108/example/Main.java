package ch.unibas.dmi.dbis.cs108.example;

import java.util.Scanner;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Nickname_Generator;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.MessageHoggerTest;
import javafx.application.Application;
import ch.unibas.dmi.dbis.cs108.example.gui.javafx.GUI;

public class Main {

    public static void main(String[] args) {
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




    }

}
