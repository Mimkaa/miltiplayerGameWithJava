package ch.unibas.dmi.dbis.cs108.example.command;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;

public interface CommandHandler {
    /**
     * Handle the given message and perform the associated logic.
     *
     * @param server The server instance, in case this command needs to
     *               access or modify game state, broadcast messages, etc.
     * @param msg    The incoming message (type, parameters, etc.)
     * @param senderUsername The name of the user who sent the message
     */
    void handle(Server server, Message msg, String senderUsername);
}
