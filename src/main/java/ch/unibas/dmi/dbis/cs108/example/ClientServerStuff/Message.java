

package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

/**
 * Represents a protocol message that includes a message type, an array of parameters,
 * an optional field, and concealed parameters.
 */
public class Message {
    private String messageType;
    private Object[] parameters;
    private String option;               // Optional field
    private String[] concealedParameters; // Concealed parameters (all strings)

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


    /**
     * Returns the message type.
     *
     * @return the message type
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Sets the message type.
     *
     * @param messageType the message type to set
     */
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    /**
     * Returns the parameters array.
     *
     * @return an array of parameters
     */
    public Object[] getParameters() {
        return parameters;
    }

    /**
     * Sets the parameters array.
     *
     * @param parameters the array of parameters to set
     */
    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    /**
     * Returns the optional field.
     *
     * @return the optional field
     */
    public String getOption() {
        return option;
    }

    /**
     * Sets the optional field.
     *
     * @param option the optional field to set
     */
    public void setOption(String option) {
        this.option = option;
    }

    /**
     * Returns the concealed parameters.
     *
     * @return an array of concealed parameters
     */
    public String[] getConcealedParameters() {
        return concealedParameters;
    }

    /**
     * Sets the concealed parameters.
     *
     * @param concealedParameters the array of concealed parameters to set
     */
    public void setConcealedParameters(String[] concealedParameters) {
        this.concealedParameters = concealedParameters;
    }
}

