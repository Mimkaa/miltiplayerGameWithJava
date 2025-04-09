package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;

public class KeyboardState {
    // A shared set to track currently pressed keys.
    private static final Set<KeyCode> keysPressed = new HashSet<>();
    // A shared set to track keys that have been released since the last check.
    private static final Set<KeyCode> keysReleased = new HashSet<>();

    public static synchronized void keyPressed(KeyCode key) {
        keysPressed.add(key);
        // In case the key was recently released, remove it from the released set.
        keysReleased.remove(key);
    }

    public static synchronized void keyReleased(KeyCode key) {
        keysPressed.remove(key);
        keysReleased.add(key);
    }

    public static synchronized boolean isKeyPressed(KeyCode key) {
        return keysPressed.contains(key);
    }

    // Returns a new copy of the currently pressed keys.
    public static synchronized Set<KeyCode> getPressedKeys() {
        return new HashSet<>(keysPressed);
    }

    // Returns true if at least one key is currently pressed.
    public static synchronized boolean anyKeyPressed() {
        return !keysPressed.isEmpty();
    }

    // Returns true if at least one key has been released since the last time you checked.
    public static synchronized boolean anyKeyReleased() {
        return !keysReleased.isEmpty();
    }
    
    // Optionally, retrieve the set of released keys and clear the released set.
    public static synchronized Set<KeyCode> getAndClearReleasedKeys() {
        Set<KeyCode> released = new HashSet<>(keysReleased);
        keysReleased.clear();
        return released;
    }
}
