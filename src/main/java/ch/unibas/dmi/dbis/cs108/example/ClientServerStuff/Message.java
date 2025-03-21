package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import lombok.Getter;

import java.util.Arrays;

/**
 * Represents a protocol message that includes a message type, an array of parameters,
 * an optional field, concealed parameters, a sequence number for reliable delivery, 
 * and a unique identifier (UUID).
 *
 * <p>
 * The sequence number is intended to be appended as the last concealed parameter when encoding.
 * </p>
 */
@Getter
public class Message {
    private String messageType;
    private Object[] parameters;
    private String option;               // Optional field
    private String[] concealedParameters; // Concealed parameters (all strings)
    private long sequenceNumber;
    @Getter// Sequence number for reliable delivery
    private String uuid;                 // Unique identifier for this message

    /**
     * Constructs a new Message.
     *
     * @param messageType         the type of the message
     * @param parameters          the array of parameters for the message
     * @param option              an optional field associated with the message
     * @param concealedParameters an array of concealed parameters (e.g., username) that are not visible in the main message part
     */
    public Message(String messageType, Object[] parameters, String option, String[] concealedParameters) {
        this.messageType = messageType;
        this.parameters = parameters;
        this.option = option;
        this.concealedParameters = concealedParameters;
        this.sequenceNumber = 0; // default until set
        this.uuid = null;// default until set
    }
    /**
     * Constructs a new Message.
     *
     * @param messageType         the type of the message
     * @param parameters          the array of parameters for the message
     * @param option              an optional field associated with the message
     * @param concealedParameters an array of concealed parameters (e.g., username) that are not visible in the main message part
     */
    public Message(String messageType, Object[] parameters, String option, String[] concealedParameters, String uuid) {
        this.messageType = messageType;
        this.parameters = parameters;
        this.option = option;
        this.concealedParameters = concealedParameters;
        this.sequenceNumber = 0; // default until set
        this.uuid = uuid;
    }



    /**
     * Constructs a new Message without concealed parameters.
     * The concealed parameters are set to null.
     *
     * @param messageType the type of the message
     * @param parameters  the array of parameters for the message
     * @param option      an optional field associated with the message
     */
    public Message(String messageType, Object[] parameters, String option) {
        this(messageType, parameters, option, null);
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public String[] getConcealedParameters() {
        return concealedParameters;
    }

    public void setConcealedParameters(String[] concealedParameters) {
        this.concealedParameters = concealedParameters;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }



    /**
     * Sets the UUID of the message.
     *
     * @param uuid the unique identifier to set.
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "Message [type=" + messageType +
               ", seq=" + sequenceNumber +
               ", uuid=" + uuid +
               ", params=" + Arrays.toString(parameters) +
               ", option=" + option +
               ", concealed=" + Arrays.toString(concealedParameters) + "]";
    }

    @Override
    public Message clone() {
        // Create copies of the parameters arrays (shallow copy is sufficient if the objects are immutable).
        Object[] clonedParams = (this.parameters != null) 
                ? Arrays.copyOf(this.parameters, this.parameters.length) 
                : null;
        String[] clonedConcealed = (this.concealedParameters != null) 
                ? Arrays.copyOf(this.concealedParameters, this.concealedParameters.length) 
                : null;
        
        // Create a new Message instance with the copied values.
        Message clone = new Message(this.messageType, clonedParams, this.option, clonedConcealed);
        
        // Optionally, copy the sequence number if it's relevant; however,
        // since sendMessage assigns a new sequence number, this may not be needed.
        clone.setSequenceNumber(this.sequenceNumber);
        
        // Reset the UUID so that sendMessage will generate a new one for each clone.
        clone.setUuid(null);
        
        return clone;
    }
}
