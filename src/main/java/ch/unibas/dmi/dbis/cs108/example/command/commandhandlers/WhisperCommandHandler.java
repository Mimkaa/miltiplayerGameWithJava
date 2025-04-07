package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

/**
 * Handles the "WHISPER" command, which allows one client to send a private message
 * directly to another connected user.
 * <p>
 * The message should contain the sender's username, the target recipient's username,
 * and the message content. If the target is not found, a "WHISPER-FAILED" response
 * is sent back to the sender.
 * </p>
 *
 * <p>Expected parameters:</p>
 * <ul>
 *   <li>params[0] - sender username</li>
 *   <li>params[1] - target recipient username</li>
 *   <li>params[2] - message content</li>
 * </ul>
 */
public class WhisperCommandHandler implements CommandHandler {

    /**
     * Processes a WHISPER command by routing a private message from the sender
     * to the intended recipient. If the recipient is not found, the sender is notified.
     *
     * @param server the server handling the communication
     * @param msg the incoming WHISPER message
     * @param senderUsername the username of the client issuing the command
     */
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        Object[] params = msg.getParameters();
        if (params == null || params.length < 3) {
            System.out.println("WHISPER command missing parameters: [sender, target, content]");
            return;
        }

        // Parse the parameters
        String actualSender = params[0].toString();
        String targetUser   = params[1].toString();
        String content      = params[2].toString();

        // Look up the target user's socket
        InetSocketAddress targetSocket = server.getClientsMap().get(targetUser);
        if (targetSocket == null) {
            System.out.println("Whisper target user '" + targetUser + "' not found or offline.");
            // Optionally notify the sender that the user is offline:
            InetSocketAddress senderSocket = server.getClientsMap().get(actualSender);
            if (senderSocket != null) {
                Message offlineMsg = new Message("WHISPER-FAILED",
                        new Object[]{ "User '" + targetUser + "' not found or offline." },
                        "RESPONSE"
                );
                server.enqueueMessage(offlineMsg, senderSocket.getAddress(), senderSocket.getPort());
            }
            return;
        }

        // Build a new message to deliver to the target
        // We can reuse "WHISPER" as the type or something like "WHISPER-MSG"
        // The parameters might just be [sender, content], since the target knows it’s the target
        Message whisperToTarget = new Message(
                "WHISPER",
                new Object[]{ actualSender, content },
                "RESPONSE"
        );

        // Enqueue it for reliable sending
        server.enqueueMessage(whisperToTarget, targetSocket.getAddress(), targetSocket.getPort());
        System.out.println("Whisper from '" + actualSender + "' to '" + targetUser + "': " + content);


    }
}
