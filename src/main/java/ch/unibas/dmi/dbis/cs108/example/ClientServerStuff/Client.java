package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {
    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 9876;

    // Global queue for outgoing messages.
    private final ConcurrentLinkedQueue<Message> outgoingQueue = new ConcurrentLinkedQueue<>();
    // Global queue for incoming messages.
    private final ConcurrentLinkedQueue<Message> incomingQueue = new ConcurrentLinkedQueue<>();

    // Make the Game an attribute of the Client.
    private final Game game;

    // Thread pool for executing tasks asynchronously.
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    // Constructor creates the Game object.
    public Client(String gameSessionName) {
        this.game = new Game(gameSessionName);
    }

    /**
     * Submits a task that runs continuously in a loop on a thread from the pool.
     * The task is wrapped in a while(true) loop.
     */
    public void addLoopTask(Runnable task) {
        threadPool.submit(() -> {
            while (true) {
                try {
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Optionally pause briefly to avoid busy spinning.
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * Submits a one-time asynchronous task to the thread pool.
     */
    public void addAsyncTask(Runnable task) {
        threadPool.submit(task);
    }

    public void run() {
        // Prompt the user to enter their name.
        String clientName;
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter your name: ");
            clientName = scanner.nextLine();
        }

        // Initialize the UI and tie it to the local player.
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

            // Receiver Task: Listen for UDP packets, decode them into Message objects,
            // and place them into the 'incomingQueue'.
            addLoopTask(() -> {
                try {
                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    clientSocket.receive(receivePacket);
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println("Received: " + response);
                    Message receivedMessage = MessageCodec.decode(response);
                    incomingQueue.offer(receivedMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // Consumer Task: Continuously poll 'incomingQueue' and push messages to the Game.
            addLoopTask(() -> {
                Message msg = incomingQueue.poll();
                if (msg != null) {
                    game.addIncomingMessage(msg);
                }
            });

            // Sender Task: Continuously poll 'outgoingQueue' and send messages over UDP to the server.
            addLoopTask(() -> {
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
                }
            });

            

            // Scheduled Game Updater at ~60 FPS.
            ScheduledExecutorService updateScheduler = Executors.newSingleThreadScheduledExecutor();
            updateScheduler.scheduleAtFixedRate(() -> {
                game.updateActiveObject(clientName, outgoingQueue);
            }, 0, 16, TimeUnit.MILLISECONDS);

            // Block the main thread indefinitely.
            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Main method creates an instance of Client and runs it.
    public static void main(String[] args) {
        Client client = new Client("GameSession1");
        client.run();
    }
}
