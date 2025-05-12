package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;

public interface FeatureProcessor {

    /**
     * Is called per frame.
     * @param game the current game
     * @param deltaTime Time since the last update in seconds
     */
    void process(Game game, float deltaTime);
}
