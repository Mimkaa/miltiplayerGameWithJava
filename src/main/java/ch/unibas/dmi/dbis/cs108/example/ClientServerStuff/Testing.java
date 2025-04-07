package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.Arrays;

/**
 * A testing class that demonstrates the encoding and decoding functionality
 * of the {@link Message} class using {@link MessageCodec}.
 *
 * <p>This class serves as both:
 * <ul>
 *   <li>A demonstration of how to use the Message encoding/decoding system</li>
 *   <li>A test case for verifying the proper functioning of Message serialization</li>
 * </ul>
 *
 * <p>The test performs the following operations:
 * <ol>
 *   <li>Creates a sample Message object with various parameter types</li>
 *   <li>Encodes the message to a string representation</li>
 *   <li>Decodes the string back to a Message object</li>
 *   <li>Prints both the encoded and decoded versions for comparison</li>
 * </ol>
 */
public class Testing {

    /**
     * Main method that executes the Message encoding/decoding test.
     *
     * @param args Command line arguments (not used in this test)
     */
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