package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingDeque;

/**
 * Queues best‑effort messages for a single client onto a shared
 * {@code BlockingDeque<OutgoingMessage>} that a single drain thread
 * (owning the DatagramSocket) consumes.
 *
 *  - No internal thread or socket: this class is just an adapter.
 *  - FIFO ordering is preserved because we {@code putLast(...)}.
 */
public final class BestEffortClientMessageSender {

    private final BlockingDeque<OutgoingMessage> outDeque;
    private final int                            port;
    private final java.net.InetAddress           addr;

    public BestEffortClientMessageSender(java.net.InetSocketAddress dest,
                                         BlockingDeque<OutgoingMessage> outDeque) {
        this.addr     = dest.getAddress();
        this.port     = dest.getPort();
        this.outDeque = outDeque;
    }

    /** Encode {@code msg} and push it to the tail of the shared deque. */
    public void enqueue(Message msg) throws InterruptedException {
        // we store the *already‑encoded* byte[] to avoid repeating work
        //byte[] raw = MessageCodec.encode(msg).getBytes(StandardCharsets.UTF_8);
        outDeque.putLast(new OutgoingMessage(msg, addr, port));
    }
}
