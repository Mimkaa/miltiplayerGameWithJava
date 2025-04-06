package ch.unibas.dmi.dbis.cs108.example.command.commandhandlers;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;

import java.net.InetSocketAddress;

public class WhisperCommandHandler implements CommandHandler {
    @Override
    public void handle(Server server, Message msg, String senderUsername) {
        // We expect the incoming "WHISPER" message to have parameters like:
        //   [0]: actualSenderName
        //   [1]: targetUsername
        //   [2]: messageContent
        //
        // For example, from the client side:
        //   new Message("WHISPER", new String[]{sender, targetUser, content}, "REQUEST")

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
