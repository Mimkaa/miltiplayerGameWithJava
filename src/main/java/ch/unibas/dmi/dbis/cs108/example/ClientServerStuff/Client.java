package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {
    public static final String SERVER_ADDRESS = "25.12.99.19";
    public static final int SERVER_PORT = 9876;

    // Global queue for outgoing messages (e.g., for server-bound messages).
    private static final ConcurrentLinkedQueue<Message> outgoingQueue = new ConcurrentLinkedQueue<>();
    // Global queue for incoming messages.
    private static final ConcurrentLinkedQueue<Message> incomingQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {
        // Prompt the user to enter their name.
        String clientName;
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter your name: ");
            clientName = scanner.nextLine();
        }

        // Create the game object with a session name.
        Game game = new Game("GameSession1");  
        
        // (Optional) Add the local player if not already created in Game's constructor.
        // game.addPlayer(clientName);

        // Initialize the UI (this sets up the JFrame and ties input to the local player).
        game.initUI(clientName);

        // Show current players.
        System.out.println("Players now in the game on the client side:");
        for (GameObject p : game.getGameObjects()) {
            System.out.println(p);
        }

        // Optionally send a "mock" message to the server to register this client.
        Message mockMessage = new Message(
            "MOCK", 
            new Object[] {"Hello from " + clientName}, 
            null, 
            new String[] {clientName}
        );
        outgoingQueue.offer(mockMessage);

        try (DatagramSocket clientSocket = new DatagramSocket()) {
            InetAddress serverIP = InetAddress.getByName(SERVER_ADDRESS);

            // Receiver Thread: Listen for UDP packets, decode them into Message objects,
            // and place them into the 'incomingQueue'.
            Thread receiverThread = new Thread(() -> {
                while (true) {
                    try {
                        byte[] receiveData = new byte[1024];
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        clientSocket.receive(receivePacket);
                        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        System.out.println("Received: " + response);

                        // Decode into a Message, then enqueue it for local processing.
                        Message receivedMessage = MessageCodec.decode(response);
                        incomingQueue.offer(receivedMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            });
            receiverThread.start();

            // Consumer Thread: Continuously poll 'incomingQueue' and push messages to the Game.
            Thread consumerThread = new Thread(() -> {
                while (true) {
                    Message msg = incomingQueue.poll();
                    if (msg != null) {
                        // Route message to the appropriate player inside Game.
                        game.addIncomingMessage(msg);
                    } else {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            });
            consumerThread.start();

            // Sender Thread: Continuously poll 'outgoingQueue' and send messages over UDP to the server.
            Thread senderThread = new Thread(() -> {
                while (true) {
                    Message msg = outgoingQueue.poll();
                    if (msg != null) {
                        String msgStr = MessageCodec.encode(msg);
                        byte[] sendData = msgStr.getBytes();
                        try {
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIP, SERVER_PORT);
                            clientSocket.send(sendPacket);
                            System.out.println("Sent: " + msgStr);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            });
            senderThread.start();

            // Instead of a tight busy loop, use a ScheduledExecutorService to update at ~60 FPS.
            ScheduledExecutorService updateScheduler = Executors.newSingleThreadScheduledExecutor();
            updateScheduler.scheduleAtFixedRate(() -> {
                game.updateActiveObject(clientName, outgoingQueue);
            }, 0, 16, TimeUnit.MILLISECONDS);

            // The main thread can now wait, or perform other tasks.
            // For example, join on the updateScheduler if needed (or simply keep running).
            // Here we'll simply block the main thread indefinitely.
            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
