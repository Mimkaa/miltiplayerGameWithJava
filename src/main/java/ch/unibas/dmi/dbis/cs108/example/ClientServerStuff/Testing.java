package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.Arrays;

public class Testing {
    public static void main(String[] args) {
        // Create a sample message.
        // For example:
        // - Message Type: "GAME"
        // - Parameters: "Start", 1, 3.14 (demonstrating a String, Integer, and Double)
        // - Option: "urgent"
        // - Concealed Parameters: "Alice", "GameSession1"
        Message message = new Message(
            "MOVE",
            new Object[]{100},
            "LEFT",
            new String[]{"Alice", "GameSession1"}
        );

        // Encode the message
        String encoded = MessageCodec.encode(message);
        System.out.println("Encoded message:");
        System.out.println(encoded);

        // Decode the message back to a Message object
        Message decoded = MessageCodec.decode(encoded);
        System.out.println("\nDecoded message:");
        System.out.println("Message Type: " + decoded.getMessageType());
        System.out.println("Option: " + decoded.getOption());
        System.out.println("Parameters: " + Arrays.toString(decoded.getParameters()));
        System.out.println((Integer)decoded.getParameters()[0]);
        System.out.println("Concealed Parameters: " + Arrays.toString(decoded.getConcealedParameters()));
    }
}
