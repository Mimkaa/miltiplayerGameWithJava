package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.Arrays;
import java.util.UUID;

/**
 * Represents a protocol message that includes a message type, an array of parameters,
 * an optional field, concealed parameters, a sequence number for reliable delivery,
 * and a unique identifier (UUID).
 *
 * <p>
 * The sequence number is intended to be appended as the last concealed parameter when
 * encoding or decoding this message. Concealed parameters are meant to contain
 * sensitive or hidden information (e.g., user or game session data) that is not
 * visible in the primary message body.
 * </p>
 */
public class Message {

    /**
     * The message type, e.g., "CREATE", "UPDATE", "CHAT", etc.
     */
    private String messageType;

    /**
     * The array of parameters associated with this message. For example, positional
     * data, names, or other domain-specific information.
     */
    private Object[] parameters;

    /**
     * An optional field that can further categorize or specify the purpose of this message.
     * Common examples might be "REQUEST", "GAME", "ACK", etc.
     */
    private String option;

    /**
     * An array of concealed parameters (strings only). These may include data such as
     * usernames or session identifiers that should be hidden or not displayed with
     * the primary message parameters.
     */
    private String[] concealedParameters;

    /**
     * A sequence number for reliable delivery. This can be used by higher-level systems
     * to track the order of messages or detect missing packets.
     */
    private long sequenceNumber;

    /**
     * A globally unique identifier (UUID) for this message instance.
     */
    private String uuid;

    /**
     * Constructs a new {@code Message} with all fields, including concealed parameters.
     *
     * @param messageType         The type of the message (e.g., "CREATE", "CHAT").
     * @param parameters          The array of parameters for the message.
     * @param option              An optional field associated with the message (e.g., "REQUEST").
     * @param concealedParameters An array of concealed parameters, or {@code null} if none.
     */
    public Message(String messageType, Object[] parameters, String option, String[] concealedParameters) {
        this.messageType = messageType;
        this.parameters = parameters;
        this.option = option;
        if (concealedParameters == null) {
            this.concealedParameters = new String[]{"gameObjectName", "gameSessionName"};
        } else {
            this.concealedParameters = concealedParameters;
        }
        this.sequenceNumber = 0; // default until set
        // Generate a new UUID upon creation.
        this.uuid = UUID.randomUUID().toString();
    }

    /**
     * Constructs a new {@code Message} without concealed parameters.
     * The concealed parameters are set to {@code null}.
     *
     * @param messageType The type of the message (e.g., "CREATE", "CHAT").
     * @param parameters  The array of parameters for the message.
     * @param option      An optional field associated with the message (e.g., "REQUEST").
     */
    public Message(String messageType, Object[] parameters, String option) {
        this(messageType, parameters, option, null);
    }

    /**
     * Retrieves the message type (e.g., "CREATE", "UPDATE", "CHAT").
     *
     * @return The current message type as a {@link String}.
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Sets the message type.
     *
     * @param messageType The new message type.
     */
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    /**
     * Retrieves the array of parameters associated with this message.
     *
     * @return An array of {@link Object} parameters.
     */
    public Object[] getParameters() {
        return parameters;
    }

    /**
     * Sets the array of parameters for this message.
     *
     * @param parameters The new parameters to associate with this message.
     */
    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    /**
     * Retrieves the optional field for this message, often used for sub-typing (e.g., "REQUEST").
     *
     * @return The current option as a {@link String}.
     */
    public String getOption() {
        return option;
    }

    /**
     * Sets the optional field for further categorizing this message.
     *
     * @param option The new option string (e.g., "GAME").
     */
    public void setOption(String option) {
        this.option = option;
    }

    /**
     * Retrieves the concealed parameters (strings) for this message.
     *
     * @return The concealed parameters as a {@link String} array.
     */
    public String[] getConcealedParameters() {
        return concealedParameters;
    }

    /**
     * Sets the concealed parameters for this message (e.g., user credentials, session info).
     *
     * @param concealedParameters A string array of concealed parameters.
     */
    public void setConcealedParameters(String[] concealedParameters) {
        this.concealedParameters = concealedParameters;
    }

    /**
     * Retrieves the sequence number for reliable delivery.
     *
     * @return The sequence number as a {@code long}.
     */
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Sets the sequence number, used for reliable delivery or ordered processing.
     *
     * @param sequenceNumber The new sequence number.
     */
    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Returns the UUID of this message.
     *
     * @return The UUID as a {@link String}.
     */
    public String getUUID() {
        return uuid;
    }

    /**
     * Sets a specific UUID for this message. Typically used if you need to match
     * a previously known UUID for correlation or acknowledgments.
     *
     * @param uuid The unique identifier to set.
     */
    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Returns a string representation of this message, including type, sequence number,
     * UUID, parameters, and concealed parameters.
     *
     * @return A formatted string describing the message.
     */
    @Override
    public String toString() {
        return "Message [type=" + messageType +
                ", seq=" + sequenceNumber +
                ", uuid=" + uuid +
                ", params=" + Arrays.toString(parameters) +
                ", option=" + option +
                ", concealed=" + Arrays.toString(concealedParameters) + "]";
    }

    /**
     * Creates and returns a shallow copy of this {@code Message}. Parameter arrays
     * and concealed parameter arrays are copied, but the objects within them
     * are not deeply cloned.
     *
     * <p>Note that the clone will receive a <strong>new</strong> UUID by default,
     * since it calls the constructor. Sequence number is copied to the new instance.</p>
     *
     * @return A shallow copy of the original {@code Message} instance.
     */
    @Override
    public Message clone() {
        // Create shallow copies of the parameters arrays.
        Object[] clonedParams = (this.parameters != null)
                ? Arrays.copyOf(this.parameters, this.parameters.length)
                : null;
        String[] clonedConcealed = (this.concealedParameters != null)
                ? Arrays.copyOf(this.concealedParameters, this.concealedParameters.length)
                : null;

        // Create a new Message instance with the copied values.
        Message clone = new Message(this.messageType, clonedParams, this.option, clonedConcealed);
        clone.setUUID(UUID.randomUUID().toString());

        // Copy the sequence number.
        clone.setSequenceNumber(this.sequenceNumber);

        // The clone will have a new UUID by default (from the constructor).
        // If desired, set it manually to match the original:
        // clone.setUUID(this.uuid);

        return clone;
    }
}
