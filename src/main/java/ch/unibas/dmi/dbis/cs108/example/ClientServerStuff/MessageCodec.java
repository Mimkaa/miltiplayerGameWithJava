package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A utility class for encoding and decoding {@link Message} objects to and from strings.
 *
 * <p>The encoding format is defined as:</p>
 * <pre>
 * TYPE {option}[parameters]|concealed1, concealed2, ..., uuid|
 * </pre>
 *
 * <ul>
 *   <li><strong>TYPE</strong>: the message type (e.g., "MOVE", "ACK", etc.)</li>
 *   <li><strong>{option}</strong>: an optional field, enclosed in braces if present</li>
 *   <li><strong>[parameters]</strong>: a comma-separated list of typed parameters, enclosed in square brackets</li>
 *   <li><strong>|concealed parameters, uuid|</strong>: a pipe-delimited section containing any concealed parameters and the message UUID as the last element</li>
 * </ul>
 *
 * Each parameter is encoded with a type prefix to facilitate decoding. For example, an integer is stored as <em>I:123</em>,
 * a float as <em>F:1.23</em>, etc.
 */
public class MessageCodec {

    /**
     * Encodes a {@link Message} object into a string based on the defined format:
     * <pre>
     * TYPE {option}[parameters]|concealed1, concealed2, ..., uuid|
     * </pre>
     *
     * <p>The optional field is enclosed in braces if present, the parameters (if any) are
     * enclosed in square brackets and typed, and the concealed parameters (plus the UUID)
     * are appended after a pipe character.</p>
     *
     * @param message the {@link Message} object to encode
     * @return the encoded string representation of the message
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
     * Decodes a string into a {@link Message} object.
     * The expected format is:
     * <pre>
     * TYPE {option}[parameters]|concealed1, concealed2, ..., uuid|
     * </pre>
     *
     * <p>The main part before the first pipe (<em>|</em>) contains the message type, optional
     * field, and parameters. The portion between pipes (<em>|</em>) contains the concealed
     * parameters and the UUID (last element).</p>
     *
     * @param encodedMessage the encoded string representation of the message
     * @return a {@link Message} object corresponding to the encoded string
     * @throws IllegalArgumentException if the encoded message does not match the expected format
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
     * Encodes a single parameter with a type prefix:
     * <ul>
     *   <li><em>I:</em> for {@link Integer}</li>
     *   <li><em>L:</em> for {@link Long}</li>
     *   <li><em>F:</em> for {@link Float}</li>
     *   <li><em>D:</em> for {@link Double}</li>
     *   <li><em>B:</em> for {@link Boolean}</li>
     *   <li><em>S:</em> for others (treated as strings)</li>
     * </ul>
     *
     * @param param the parameter to encode
     * @return a {@code String} in the form <em>{type}:{value}</em>
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
            return "S:" + param.toString();
        }
    }

    /**
     * Decodes a comma-separated list of typed parameters (e.g., "I:10, F:1.23, S:hello")
     * into an array of Objects.
     *
     * @param encodedParameters the string representing the encoded parameters
     * @return an array of decoded Objects
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
     * Decodes the main part of the message containing the type, optional field, and parameters.
     * The expected syntax is:
     * <pre>
     * TYPE {option}[parameters]
     * </pre>
     *
     * @param mainPart the substring from the start of the encoded message up to the first pipe
     * @return a partially constructed {@link Message} with the type, parameters, and option
     * @throws IllegalArgumentException if the format is invalid
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
     * Decodes a single parameter token, which includes a type marker (I, L, F, D, B, or S).
     *
     * @param token the string token (e.g., "I:10", "S:hello")
     * @return the decoded parameter as an {@link Object}
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
            return token;
        }
    }
}
