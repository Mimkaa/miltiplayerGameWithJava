package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Your own message types/codecs live in the same package, so no extra imports
// import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
// import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.MessageCodec;

public class BestEffortClientMessageSender implements Runnable {
    private final InetSocketAddress dest;
    private final DatagramSocket socket;
    private final BlockingQueue<Message> queue;
    private volatile boolean running = true;

    public BestEffortClientMessageSender(DatagramSocket socket,
                                        InetSocketAddress dest,
                                        int queueCapacity) {
        this.socket = socket;
        this.dest   = dest;
        this.queue  = new LinkedBlockingQueue<>(queueCapacity);
        new Thread(this, "BestEffortSender-" + dest).start();
    }

    /** Enqueue a message for this client. */
    public void enqueue(Message msg) throws InterruptedException {
        queue.put(msg);
    }

    /** Main loop: pull messages and send them over UDP. */
    @Override
    public void run() {
        try {
            while (running) {
                Message msg = queue.take();
                byte[] data = MessageCodec.encode(msg).getBytes(StandardCharsets.UTF_8);
                DatagramPacket pkt = new DatagramPacket(
                    data, data.length,
                    dest.getAddress(), dest.getPort()
                );
                socket.send(pkt);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // allow shutdown
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /** Stop the sender thread and clear any queued messages. */
    public void shutdown() {
        running = false;
        Thread.currentThread().interrupt();
        queue.clear();
    }
}
