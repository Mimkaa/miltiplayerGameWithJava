package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import lombok.Getter;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Getter
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
     * Creates and adds a new game session with a given session ID.
     *
     * @param sessionId the unique session ID
     * @param gameName the name of the created Game.
     */
    public void addGameSession(String sessionId, String gameName ) {
        Game game = new Game(sessionId, gameName);
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

    public ConcurrentHashMap<String, Game> getAllGameSessions() {
        return gameSessions;
    }
    public Collection<Game> getAllGameSessionsVals() {
        return gameSessions.values();
    }
}


