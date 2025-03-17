package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A utility class for encoding and decoding Message objects to and from strings.
 *
 * <p>
 * The encoding format is defined as:
 * <pre>
 * TYPE {option}[parameters]|concealed parameters, uuid|
 * </pre>
 * The option is enclosed in curly braces if present, the parameters are enclosed in square brackets,
 * and the concealed parameters (all strings) are appended after a pipe delimiter with the UUID as the last concealed parameter.
 * Each parameter is encoded with a type prefix.
 * </p>
 */
public class MessageCodec {

    /**
     * Encodes a Message object into a string.
     * The format is:
     * <pre>
     * TYPE {option}[parameters]|concealed1, concealed2, ..., uuid|
     * </pre>
     *
     * @param message the Message object to encode
     * @return the encoded string representation of the message
     */
    public static String encode(Message message) {
        StringBuilder sb = new StringBuilder();
        // Append message type.
        sb.append(message.getMessageType());
        // Append option if available.
        if (message.getOption() != null && !message.getOption().isEmpty()) {
            sb.append(" {").append(message.getOption()).append("}");
        }
        // Append parameters in square brackets.
        sb.append("[");
        Object[] params = message.getParameters();
        if (params != null && params.length > 0) {
            sb.append(Arrays.stream(params)
                            .map(MessageCodec::encodeWithTypePrefix)
                            .collect(Collectors.joining(", ")));
        }
        sb.append("]");
        
        // Append concealed parameters and the UUID between pipe delimiters.
        sb.append("|");
        String[] concealed = message.getConcealedParameters();
        String uuidStr = message.getUUID(); // Obtain the UUID from the message.
        if (concealed != null && concealed.length > 0) {
            sb.append(String.join(", ", concealed));
            sb.append(", ").append(uuidStr);
        } else {
            sb.append(uuidStr);
        }
        sb.append("|");
        
        return sb.toString();
    }
    
    /**
     * Helper method to encode a parameter with a type prefix.
     */
    private static String encodeWithTypePrefix(Object param) {
        if (param instanceof Integer) {
            return "I:" + param;
        } else if (param instanceof Long) {
            return "L:" + param;
        } else if (param instanceof Float) {
            return "F:" + param;
        } else if (param instanceof Double) {
            return "D:" + param;
        } else if (param instanceof Boolean) {
            return "B:" + param;
        } else {
            // Default to string.
            return "S:" + param.toString();
        }
    }

    /**
     * Decodes an encoded message string into a Message object.
     * The expected format is:
     * <pre>
     * TYPE {option}[parameters]|concealed parameters, uuid|
     * </pre>
     *
     * @param encodedMessage the encoded string representation of the message
     * @return a Message object corresponding to the encoded string
     * @throws IllegalArgumentException if the encoded message does not match the expected format
     */
    public static Message decode(String encodedMessage) {
        // Split the encoded message into main part and concealed part.
        // Expected format: mainPart|concealedPart|
        String[] parts = encodedMessage.split("\\|", -1);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid encoded message format; missing concealed parameters");
        }
        String mainPart = parts[0].trim();
        String concealedPart = parts[1].trim();
        
        // Decode the main part (format: TYPE {option}[parameters]).
        Message message = decodeMainPart(mainPart);
        
        // Decode concealed parameters.
        String[] concealedParameters;
        if (concealedPart.isEmpty()) {
            concealedParameters = new String[0];
        } else {
            concealedParameters = concealedPart.split(",\\s*");
        }
        
        // Expect that the last concealed parameter is the UUID.
        if (concealedParameters.length > 0) {
            String uuid = concealedParameters[concealedParameters.length - 1];
            message.setUUID(uuid);
            // Remove the last element from the concealed parameters.
            String[] newConcealed = new String[concealedParameters.length - 1];
            System.arraycopy(concealedParameters, 0, newConcealed, 0, concealedParameters.length - 1);
            message.setConcealedParameters(newConcealed);
        } else {
            message.setUUID(null);
        }
        
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
        String regex = "^(.+?)(?:\\s*\\{(.+?)\\})?\\s*\\[(.*)\\]$";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(mainPart);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid main part format in encoded message: " + mainPart);
        }
        String messageType = matcher.group(1).trim();
        String option = matcher.group(2) != null ? matcher.group(2).trim() : null;
        String paramsContent = matcher.group(3).trim();
        
        Object[] parameters;
        if (paramsContent.isEmpty()) {
            parameters = new Object[0];
        } else {
            String[] tokens = paramsContent.split(",\\s*");
            parameters = new Object[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                parameters[i] = decodeWithTypePrefix(tokens[i]);
            }
        }
        return new Message(messageType, parameters, option, null);
    }
    
    /**
     * Helper method to decode a parameter token that includes a type marker.
     */
    private static Object decodeWithTypePrefix(String token) {
        if (token.startsWith("I:")) {
            return Integer.parseInt(token.substring(2));
        } else if (token.startsWith("L:")) {
            return Long.parseLong(token.substring(2));
        } else if (token.startsWith("F:")) {
            return Float.parseFloat(token.substring(2));
        } else if (token.startsWith("D:")) {
            return Double.parseDouble(token.substring(2));
        } else if (token.startsWith("B:")) {
            return Boolean.parseBoolean(token.substring(2));
        } else if (token.startsWith("S:")) {
            return token.substring(2);
        } else {
            // Fallback: if no marker is found, return the token as is.
            return token;
        }
    }
}
