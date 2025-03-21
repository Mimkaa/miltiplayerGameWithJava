package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.Arrays;

/**
 * The {@code Message} class represents a protocol message with a specified type, a list of parameters,
 * an optional field, concealed parameters that do not appear in the main message part, a sequence
 * number for tracking reliable delivery, and a unique identifier (UUID).
 * <p>
 * The {@link #sequenceNumber} may be appended as the last concealed parameter when the message is
 * encoded for transmission, depending on the protocol requirements.
 * </p>
 */
public class Message {

    private String messageType;
    private Object[] parameters;
    private String option;
    private String[] concealedParameters;
    private long sequenceNumber;
    private String uuid;

    /**
     * Constructs a new {@code Message} with specified message type, parameters, an optional field,
     * and concealed parameters.
     *
     * @param messageType         the type (or category) of the message
     * @param parameters          an array of objects representing the message parameters
     * @param option              an optional field associated with the message
     * @param concealedParameters an array of strings that should remain hidden or not displayed
     */
    public Message(String messageType, Object[] parameters, String option, String[] concealedParameters) {
        this.messageType = messageType;
        this.parameters = parameters;
        this.option = option;
        this.concealedParameters = concealedParameters;
        this.sequenceNumber = 0;
        this.uuid = null;
    }

    /**
     * Constructs a new {@code Message} without specifying concealed parameters.
     * The concealed parameters are set to {@code null} by default.
     *
     * @param messageType the type of the message
     * @param parameters  an array of objects representing the message parameters
     * @param option      an optional field associated with the message
     */
    public Message(String messageType, Object[] parameters, String option) {
        this(messageType, parameters, option, null);
    }

    /**
     * Returns the message type.
     *
     * @return a {@code String} representing the message type
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Sets the message type.
     *
     * @param messageType a {@code String} representing the new message type
     */
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    /**
     * Returns the array of parameters associated with this message.
     *
     * @return an {@code Object[]} containing the message parameters
     */
    public Object[] getParameters() {
        return parameters;
    }

    /**
     * Sets the array of parameters for this message.
     *
     * @param parameters an {@code Object[]} containing the new parameters
     */
    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    /**
     * Returns the optional field associated with this message.
     *
     * @return a {@code String} representing an optional field
     */
    public String getOption() {
        return option;
    }

    /**
     * Sets the optional field for this message.
     *
     * @param option a {@code String} representing a new optional field
     */
    public void setOption(String option) {
        this.option = option;
    }

    /**
     * Returns the concealed parameters associated with this message.
     *
     * @return a {@code String[]} containing the concealed parameters
     */
    public String[] getConcealedParameters() {
        return concealedParameters;
    }

    /**
     * Sets the concealed parameters for this message.
     *
     * @param concealedParameters a {@code String[]} for the concealed parameters
     */
    public void setConcealedParameters(String[] concealedParameters) {
        this.concealedParameters = concealedParameters;
    }

    /**
     * Returns the sequence number for this message, used for reliable delivery.
     *
     * @return a {@code long} representing the sequence number
     */
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Sets the sequence number for this message.
     *
     * @param sequenceNumber a {@code long} representing a new sequence number
     */
    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Returns the unique identifier (UUID) of this message.
     *
     * @return a {@code String} containing the UUID, or {@code null} if not set
     */
    public String getUUID() {
        return uuid;
    }

    /**
     * Sets the unique identifier (UUID) of this message.
     *
     * @param uuid a {@code String} representing the new UUID
     */
    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Returns a string representation of this message, including its type, sequence number,
     * UUID, parameters, option, and concealed parameters.
     *
     * @return a {@code String} describing the current state of this message
     */
    @Override
    public String toString() {
        return "Message [type=" + messageType
                + ", seq=" + sequenceNumber
                + ", uuid=" + uuid
                + ", params=" + Arrays.toString(parameters)
                + ", option=" + option
                + ", concealed=" + Arrays.toString(concealedParameters) + "]";
    }

    /**
     * Creates a shallow copy of this message. Parameter arrays and concealed parameters
     * are copied, while the UUID is reset to {@code null} so that each cloned message
     * can be assigned its own unique identifier.
     *
     * @return a new {@code Message} instance with the same content as this one, but
     *         without a UUID
     */
    @Override
    public Message clone() {
        Object[] clonedParams = (this.parameters != null)
                ? Arrays.copyOf(this.parameters, this.parameters.length)
                : null;
        String[] clonedConcealed = (this.concealedParameters != null)
                ? Arrays.copyOf(this.concealedParameters, this.concealedParameters.length)
                : null;

        Message clone = new Message(this.messageType, clonedParams, this.option, clonedConcealed);
        clone.setSequenceNumber(this.sequenceNumber);
        clone.setUUID(null);

        return clone;
    }
}
