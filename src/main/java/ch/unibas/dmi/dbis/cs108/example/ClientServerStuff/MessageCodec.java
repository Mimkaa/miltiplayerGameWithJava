package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A utility class for encoding and decoding {@link Message} objects to and from string format.
 *
 * <p>
 * The <strong>encoding format</strong> is structured as:
 * </p>
 * <pre>
 *   TYPE {option}[parameters]|concealed1, concealed2, ..., uuid|
 * </pre>
 * <ul>
 *   <li><strong>TYPE</strong> – the message type (e.g., "MOVE", "ACK")</li>
 *   <li><strong>{option}</strong> – an optional field, present if not empty</li>
 *   <li><strong>[parameters]</strong> – a comma-separated list of typed parameters, enclosed in square brackets</li>
 *   <li><strong>|concealed parameters, uuid|</strong> – the concealed parameters plus the UUID at the end, each separated by a comma</li>
 * </ul>
 *
 * <p>
 * Each parameter in the <em>[parameters]</em> section is encoded with a type prefix (e.g., "I:" for integers, "F:" for floats),
 * facilitating a typed decoding process.
 * </p>
 */
public class MessageCodec {

    /**
     * Encodes a given {@link Message} object into a string according to the specified format:
     * <pre>
     *   TYPE {option}[parameters]|concealed1, concealed2, ..., uuid|
     * </pre>
     *
     * <p>
     * If {@code message.getOption()} is non-empty, it is enclosed in curly braces.
     * The parameters (if any) are enclosed in square brackets, with each parameter prefixed
     * by a type marker. Concealed parameters (if any) plus the message UUID are appended after
     * a pipe character (<em>|</em>).
     * </p>
     *
     * @param message the {@link Message} object to encode
     * @return a string representing the encoded message
     */
    public static String encode(Message message) {
        StringBuilder sb = new StringBuilder();
        sb.append(message.getMessageType());

        if (message.getOption() != null && !message.getOption().isEmpty()) {
            sb.append(" {").append(message.getOption()).append("}");
        }

        sb.append("[");
        Object[] params = message.getParameters();
        if (params != null && params.length > 0) {
            sb.append(Arrays.stream(params)
                    .map(MessageCodec::encodeWithTypePrefix)
                    .collect(Collectors.joining(", ")));
        }
        sb.append("]");

        sb.append("|");
        String[] concealed = message.getConcealedParameters();
        String uuidStr = message.getUUID();
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
     * Decodes a string into a {@link Message} object, assuming the string follows
     * the specified format:
     * <pre>
     *   TYPE {option}[parameters]|concealed1, concealed2, ..., uuid|
     * </pre>
     * <p>
     * The part before the first pipe (<em>|</em>) contains the message type,
     * optional field, and parameters. The portion between pipes contains the
     * concealed parameters plus the UUID (last).
     * </p>
     *
     * @param encodedMessage the string to decode into a {@link Message}
     * @return a {@link Message} object containing the decoded data
     * @throws IllegalArgumentException if the string does not match the expected format
     */
    public static Message decode(String encodedMessage) {
        String[] parts = encodedMessage.split("\\|", -1);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid encoded message format; missing concealed parameters");
        }
        String mainPart = parts[0].trim();
        String concealedPart = parts[1].trim();

        Message message = decodeMainPart(mainPart);

        String[] concealedParameters;
        if (concealedPart.isEmpty()) {
            concealedParameters = new String[0];
        } else {
            concealedParameters = concealedPart.split(",\\s*");
        }

        if (concealedParameters.length > 0) {
            String uuid = concealedParameters[concealedParameters.length - 1];
            message.setUUID(uuid);
            String[] newConcealed = new String[concealedParameters.length - 1];
            System.arraycopy(concealedParameters, 0, newConcealed, 0, concealedParameters.length - 1);
            message.setConcealedParameters(newConcealed);
        } else {
            message.setUUID(null);
        }

        return message;
    }

    /**
     * Encodes a single parameter value with a type prefix.
     * <ul>
     *   <li><em>I:</em> {@link Integer}</li>
     *   <li><em>L:</em> {@link Long}</li>
     *   <li><em>F:</em> {@link Float}</li>
     *   <li><em>D:</em> {@link Double}</li>
     *   <li><em>B:</em> {@link Boolean}</li>
     *   <li><em>S:</em> All other types (treated as strings)</li>
     * </ul>
     *
     * @param param the parameter to encode with a type prefix
     * @return a string of the form <em>{type}:{value}</em>
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
            // Default is string
            return "S:" + param.toString();
        }
    }

    /**
     * Decodes a comma-separated list of typed parameters (e.g., "I:10, F:1.23, S:hello")
     * into an array of {@link Object}.
     *
     * @param encodedParameters the string to decode
     * @return an array of objects representing the decoded parameters
     */
    public static Object[] decodeParameters(String encodedParameters) {
        if (encodedParameters == null || encodedParameters.trim().isEmpty()) {
            return new Object[0];
        }
        String[] tokens = encodedParameters.split(",\\s*");
        Object[] parameters = new Object[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            parameters[i] = decodeWithTypePrefix(tokens[i]);
        }
        return parameters;
    }

    /**
     * Decodes the main part of the encoded message (type, option, and parameters).
     * The syntax is assumed to be:
     * <pre>
     *   TYPE {option}[parameters]
     * </pre>
     *
     * @param mainPart the portion of the message before the concealed part
     * @return a partially constructed {@link Message} with type, parameters, and option
     * @throws IllegalArgumentException if the main part does not match the expected format
     */
    private static Message decodeMainPart(String mainPart) {
        String regex = "^(.+?)(?:\\s*\\{(.+?)\\})?\\s*\\[(.*)\\]$";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(mainPart);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid main part format in encoded message: " + mainPart);
        }
        String messageType = matcher.group(1).trim();
        String option = (matcher.group(2) != null) ? matcher.group(2).trim() : null;
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
     * Decodes a single parameter token (e.g., "I:10", "S:hello") into a typed {@link Object}.
     *
     * @param token a string containing the type prefix and value
     * @return an object parsed from the token
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
            // If no type prefix is found, return the token as-is (string).
            return token;
        }
    }
}
