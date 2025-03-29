package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;

public class KeyboardState{
    // A shared set to track currently pressed keys.
    private static final Set<KeyCode> keysPressed = new HashSet<>();

    public static void keyPressed(KeyCode key) {
        keysPressed.add(key);
    }

    public static void keyReleased(KeyCode key) {
        keysPressed.remove(key);
    }

    public static boolean isKeyPressed(KeyCode key) {
        return keysPressed.contains(key);
    }
}
