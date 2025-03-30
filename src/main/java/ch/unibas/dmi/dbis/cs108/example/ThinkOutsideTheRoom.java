package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Nickname_Generator;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameContext;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Client;
import java.lang.Thread;

import java.util.Scanner;
import java.util.UUID;

/**
 * Main entry point for starting Server and Client instances individually or simultaneously.
 */
public class ThinkOutsideTheRoom {

    public static void main(String[] args) {



        GameContext neContect = new GameContext();
        neContect.start();

        Thread senderThread = new Thread(() -> {
        while (true) {
            neContect.sendCreateGametoClient("GameSession1");
            try {
                Thread.sleep(1000); // Sleep for 1000 milliseconds (1 second)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            neContect.printAllGameSessions();
        }
        });
        senderThread.start();


    }

}
