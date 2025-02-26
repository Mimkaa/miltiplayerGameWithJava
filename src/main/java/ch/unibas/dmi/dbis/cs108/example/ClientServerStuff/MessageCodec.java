package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A utility class for encoding and decoding Message objects to and from strings.
 *
 * <p>
 * The encoding format is defined as:
 * <pre>
 * TYPE {option}[parameters]|concealed parameters|
 * </pre>
 * The option is enclosed in curly braces if present, the parameters are enclosed in square brackets,
 * and the concealed parameters (all strings) are appended after a pipe delimiter.
 * </p>
 */
public class MessageCodec {

    /**
     * Encodes a Message object into a string using the format:
     * <pre>
     * TYPE {option}[parameters]|concealed parameters|
     * </pre>
     *
     * @param message the Message object to encode
     * @return the encoded string representation of the message
     */
    public static String encode(Message message) {
        StringBuilder sb = new StringBuilder();
        // Append message type
        sb.append(message.getMessageType());
        // Append option if available
        if (message.getOption() != null && !message.getOption().isEmpty()) {
            sb.append(" {").append(message.getOption()).append("}");
        }
        // Append parameters in square brackets
        sb.append("[");
        Object[] params = message.getParameters();
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                sb.append(params[i].toString());
                if (i < params.length - 1) {
                    sb.append(", ");
                }
            }
        }
        sb.append("]");

        // Append concealed parameters between pipe delimiters
        sb.append("|");
        String[] concealed = message.getConcealedParameters();
        if (concealed != null && concealed.length > 0) {
            sb.append(String.join(", ", concealed));
        }
        sb.append("|");

        return sb.toString();
    }

    /**
     * Decodes an encoded message string into a Message object.
     * The expected format is:
     * <pre>
     * TYPE {option}[parameters]|concealed parameters|
     * </pre>
     *
     * @param encodedMessage the encoded string representation of the message
     * @return a Message object corresponding to the encoded string
     * @throws IllegalArgumentException if the encoded message does not match the expected format
     */
    public static Message decode(String encodedMessage) {
        // Split the encoded message into main part and concealed part.
        // The expected format has two pipe characters, e.g.:
        // mainPart|concealedPart|
        String[] parts = encodedMessage.split("\\|", -1);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid encoded message format; missing concealed parameters");
        }
        String mainPart = parts[0].trim();
        String concealedPart = parts[1].trim(); // concealed parameters list as a string

        // Decode the main part (format: TYPE {option}[parameters])
        Message message = decodeMainPart(mainPart);

        // Decode concealed parameters (if any) and set them in the Message
        String[] concealedParameters;
        if (concealedPart.isEmpty()) {
            concealedParameters = new String[0];
        } else {
            // Split concealed parameters by comma and optional whitespace.
            concealedParameters = concealedPart.split(",\\s*");
        }
        message.setConcealedParameters(concealedParameters);

        return message;
    }

    /**
     * Helper method to decode the main part of the encoded message (TYPE {option}[parameters]).
     *
     * @param mainPart the main part of the encoded message
     * @return a Message object with the messageType, parameters, and option set
     * @throws IllegalArgumentException if the main part does not match the expected format
     */
    private static Message decodeMainPart(String mainPart) {
        // Regular expression to capture the message type, optional option, and parameters.
        Pattern pattern = Pattern.compile("^(.+?)(?:\\s*\\{(.+?)\\})?\\s*\\[(.*)\\]$");
        Matcher matcher = pattern.matcher(mainPart);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid main part format in encoded message");
        }
        String messageType = matcher.group(1).trim();
        String option = matcher.group(2) != null ? matcher.group(2).trim() : null;
        String paramsContent = matcher.group(3).trim();

        // Parse parameters from the content between the square brackets.
        Object[] parameters;
        if (paramsContent.isEmpty()) {
            parameters = new Object[0];
        } else {
            String[] tokens = paramsContent.split(",\\s*");
            parameters = new Object[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                parameters[i] = parseParameter(tokens[i]);
            }
        }
        // Concealed parameters are handled separately.
        return new Message(messageType, parameters, option, null);
    }

    /**
     * Helper method to parse a parameter token into an appropriate type.
     * It attempts to parse the token as an {@code Integer}, then as a {@code Double},
     * and falls back to returning it as a {@code String} if both attempts fail.
     *
     * @param token the parameter token as a string
     * @return the parsed parameter as an Object
     */
    private static Object parseParameter(String token) {
        token = token.trim();
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            // Not an integer.
        }
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException e) {
            // Not a double.
        }
        return token;
    }
}
