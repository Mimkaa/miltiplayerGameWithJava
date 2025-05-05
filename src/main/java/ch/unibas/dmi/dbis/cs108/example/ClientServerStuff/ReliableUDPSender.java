package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sends OutgoingMessage reliably over UDP using a sliding window + timeout.
 * On timeout it re-offers the original OutgoingMessage back into your queue.
 */
public class ReliableUDPSender {

    private final DatagramSocket socket;
    private final int windowSize;
    private final long timeoutMillis;
    private final LinkedBlockingQueue<OutgoingMessage> requeueOnTimeout;
    private final AtomicInteger nextSeqNum = new AtomicInteger(1);

    // UUID → pending info
    private final ConcurrentHashMap<String, PendingOutgoing> pending = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> inFlight = new ConcurrentLinkedQueue<>();

    private static class PendingOutgoing {
        final OutgoingMessage om;
        volatile long lastSentTime;
        final String uuid;
        final int seq;

        PendingOutgoing(OutgoingMessage om, String uuid, int seq, long sentAt) {
            this.om           = om;
            this.uuid         = uuid;
            this.seq          = seq;
            this.lastSentTime = sentAt;
        }
    }

    /**
     * @param socket           socket to send on
     * @param windowSize       max unacked messages
     * @param timeoutMillis    ms until retransmit+requeue
     * @param requeueOnTimeout queue to re-offer timed-out OutgoingMessages
     */
    public ReliableUDPSender(DatagramSocket socket,
                             int windowSize,
                             long timeoutMillis,
                             LinkedBlockingQueue<OutgoingMessage> requeueOnTimeout) {
        this.socket           = socket;
        this.windowSize       = windowSize;
        this.timeoutMillis    = timeoutMillis;
        this.requeueOnTimeout = requeueOnTimeout;
        AsyncManager.runLoop(this::checkTimeouts);
    }

    /** Enqueue an OutgoingMessage for reliable send. */
    public void send(OutgoingMessage om) {
        AsyncManager.run(() -> {
            if (pending.size() >= windowSize) {
                System.out.println("Window full, dropping: " + om.msg);
                return;
            }
            String uuid = om.msg.getUUID();
            int seq     = nextSeqNum.getAndIncrement();
            om.msg.setSequenceNumber(seq);
            
            if (!inFlight.contains(uuid))
            {
                pending.put(uuid, new PendingOutgoing(om, uuid, seq, System.currentTimeMillis()));
            }
            inFlight.offer(uuid);
            sendPacket(om);
        });
    }

    private void sendPacket(OutgoingMessage om) {
        AsyncManager.run(() -> {
            try {
                String encoded = MessageCodec.encode(om.msg);
                byte[] data    = encoded.getBytes();
                socket.send(new DatagramPacket(
                    data, data.length,
                    om.address, om.port
                ));
                System.out.println("[RELIABLE] Sending to " + om.address.getHostAddress() + ":" + om.port);
                System.out.println("→ Sent reliably: " + encoded);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /** Called by your ACK-handler to clear out a UUID. */
    public void acknowledge(String uuid) {
        inFlight.remove(uuid);
        if (pending.remove(uuid) != null) {
            System.out.println("ACKed UUID=" + uuid);
        }
    }

    /** Periodically scans for timeouts, retransmits & re-queues. */
    private void checkTimeouts() {
        long now = System.currentTimeMillis();
        if (pending.isEmpty()) return;

        List<PendingOutgoing> list = new ArrayList<>(pending.values());
        list.sort(Comparator.comparingInt(po -> po.seq));
        //System.out.println("[RELIABLE] requeueOnTimeout size=" + requeueOnTimeout.size());
        for (PendingOutgoing po : list) {
            if (now - po.lastSentTime >= timeoutMillis) {
                // retransmit
                //sendPacket(po.om);
                // reset timer
                po.lastSentTime = now;
                // re-queue the original OutgoingMessage for any higher-level logic
                requeueOnTimeout.offer(po.om);
                System.out.println("Timeout → resent & requeued UUID="
                    + po.uuid + " seq=" + po.seq);
            }
        }
    }

    /** Returns how many are still un-acked. */
    public int hasBacklog() {
        return pending.size();
    }
}
