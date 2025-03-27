package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import java.util.concurrent.ConcurrentHashMap;

public class GameSessionManager {
    // Map of session IDs to Game objects.
    private final ConcurrentHashMap<String, Game> gameSessions = new ConcurrentHashMap<>();

    /**
     * Adds a new game session with a given session ID.
     *
     * @param sessionId the unique session ID
     * @param game the Game object representing the session
     */
    public void addGameSession(String sessionId, Game game) {
        gameSessions.put(sessionId, game);
    }

    /**
     * Retrieves a game session by its session ID.
     *
     * @param sessionId the session ID
     * @return the corresponding Game object, or null if not found
     */
    public Game getGameSession(String sessionId) {
        return gameSessions.get(sessionId);
    }

    /**
     * Removes a game session by its session ID.
     *
     * @param sessionId the session ID
     * @return the removed Game object, or null if no session existed for that ID
     */
    public Game removeGameSession(String sessionId) {
        return gameSessions.remove(sessionId);
    }
    
    /**
     * Returns the number of active game sessions.
     *
     * @return the number of game sessions
     */
    public int getSessionCount() {
        return gameSessions.size();
    }
}

