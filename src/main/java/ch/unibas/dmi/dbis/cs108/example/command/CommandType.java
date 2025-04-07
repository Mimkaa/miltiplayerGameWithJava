package ch.unibas.dmi.dbis.cs108.example.command;

/**
 * Enumeration of all supported command types that can be sent between
 * client and server during the game.
 * <p>
 * Each value represents a specific type of operation or request
 * that the system can interpret and handle via a corresponding
 * {@link CommandHandler}.
 * </p>
 */
public enum CommandType {

    /** Create a new game object or entity. */
    CREATE,

    /** Ping command to check connectivity or responsiveness. */
    PING,

    /** Request or fetch an object's unique ID. */
    GETOBJECTID,

    /** Request to change the name of an object or user. */
    CHANGENAME,

    /** Notification that a user has joined. */
    USERJOINED,

    /** User logout operation. */
    LOGOUT,

    /** Shutdown or close the game/application. */
    EXIT,

    /** User login operation. */
    LOGIN,

    /** Delete a game object or entity. */
    DELETE,

    /** Create a new game session. */
    CREATEGAME
}
