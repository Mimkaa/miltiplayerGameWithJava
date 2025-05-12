package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;


import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;

/**
 * The {@code KeyboardState} class is responsible for tracking the state of the keyboard.
 * It keeps track of the keys that are currently pressed, as well as the keys that have been released.
 * It provides synchronized methods to handle key events and retrieve the current state of the keyboard.
 */
public class KeyboardState {

    // A shared set to track currently pressed keys.
    private static final Set<KeyCode> keysPressed = new HashSet<>();
    // A shared set to track keys that have been released since the last check.
    private static final Set<KeyCode> keysReleased = new HashSet<>();

    /**
     * Records that a key has been pressed.
     * If the key was recently released, it is removed from the released set.
     *
     * @param key The key that was pressed.
     */
    public static synchronized void keyPressed(KeyCode key) {
        keysPressed.add(key);
        // In case the key was recently released, remove it from the released set.
        keysReleased.remove(key);
    }

    /**
     * Records that a key has been released.
     *
     * @param key The key that was released.
     */
    public static synchronized void keyReleased(KeyCode key) {
        keysPressed.remove(key);
        keysReleased.add(key);
    }

    /**
     * Checks if a specific key is currently pressed.
     *
     * @param key The key to check.
     * @return {@code true} if the key is pressed, otherwise {@code false}.
     */
    public static synchronized boolean isKeyPressed(KeyCode key) {
        return keysPressed.contains(key);
    }

    /**
     * Retrieves a copy of the set of currently pressed keys.
     *
     * @return A new set containing all the currently pressed keys.
     */
    public static synchronized Set<KeyCode> getPressedKeys() {
        return new HashSet<>(keysPressed);
    }

    /**
     * Checks if at least one key is currently pressed.
     *
     * @return {@code true} if at least one key is pressed, otherwise {@code false}.
     */
    public static synchronized boolean anyKeyPressed() {
        return !keysPressed.isEmpty();
    }

    /**
     * Checks if at least one key has been released since the last time you checked.
     *
     * @return {@code true} if at least one key has been released, otherwise {@code false}.
     */
    public static synchronized boolean anyKeyReleased() {
        return !keysReleased.isEmpty();
    }

    /**
     * Retrieves the set of keys that have been released since the last check and clears the released set.
     *
     * @return A set containing all the keys that have been released since the last check.
     */
    public static synchronized Set<KeyCode> getAndClearReleasedKeys() {
        Set<KeyCode> released = new HashSet<>(keysReleased);
        keysReleased.clear();
        return released;
    }
}
