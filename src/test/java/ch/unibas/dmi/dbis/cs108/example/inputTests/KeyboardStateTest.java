package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link KeyboardState} helper class.
 * <p>
 * These tests verify that key presses and releases are tracked correctly,
 * that the snapshot methods return copies rather than live views, and that
 * the anyKeyPressed/anyKeyReleased flags behave as expected.
 * </p>
 */
class KeyboardStateTest {

    /**
     * Before each test we clear out any leftover pressed or released keys
     * so that each test starts with a clean keyboard state.
     */
    @BeforeEach
    void resetKeyboardState() {
        // release all currently pressed keys
        for (KeyCode k : KeyboardState.getPressedKeys()) {
            KeyboardState.keyReleased(k);
        }
        // clear any keys flagged as released
        KeyboardState.getAndClearReleasedKeys();
    }

    /**
     * Verifies that pressing a key causes {@link KeyboardState#isKeyPressed(KeyCode)}
     * and {@link KeyboardState#anyKeyPressed()} to return true, and that
     * {@link KeyboardState#getPressedKeys()} contains exactly that key.
     */
    @Test
    void testKeyPressed() {
        assertFalse(KeyboardState.anyKeyPressed(), "No keys should be pressed initially");
        assertFalse(KeyboardState.isKeyPressed(KeyCode.A), "Key A should not be pressed");

        KeyboardState.keyPressed(KeyCode.A);

        assertTrue(KeyboardState.anyKeyPressed(), "A key press should be detected");
        assertTrue(KeyboardState.isKeyPressed(KeyCode.A), "Key A should now be reported as pressed");

        Set<KeyCode> pressed = KeyboardState.getPressedKeys();
        assertEquals(Set.of(KeyCode.A), pressed, "Pressed set should contain only A");
    }

    /**
     * Ensures that the set returned by {@link KeyboardState#getPressedKeys()}
     * is a copy—mutating it does not affect the internal state.
     */
    @Test
    void testGetPressedKeysIsCopy() {
        KeyboardState.keyPressed(KeyCode.B);
        Set<KeyCode> copy = KeyboardState.getPressedKeys();

        // Mutate returned set
        copy.clear();

        // Internal state should remain unchanged
        assertTrue(KeyboardState.isKeyPressed(KeyCode.B), "Clearing the copy should not remove the real pressed key");
    }

    /**
     * Verifies that releasing a key clears it from the pressed set,
     * sets the released flag, and that {@link KeyboardState#getAndClearReleasedKeys()}
     * returns exactly that key and then resets the released set.
     */
    @Test
    void testKeyReleased() {
        KeyboardState.keyPressed(KeyCode.C);
        KeyboardState.keyReleased(KeyCode.C);

        assertFalse(KeyboardState.isKeyPressed(KeyCode.C), "Key C should no longer be pressed");
        assertTrue(KeyboardState.anyKeyReleased(), "A key release should be detected");

        Set<KeyCode> released = KeyboardState.getAndClearReleasedKeys();
        assertEquals(Set.of(KeyCode.C), released, "Released set should contain only C");

        // After clearing, no further releases should be reported
        assertFalse(KeyboardState.anyKeyReleased(), "Released set should be empty after clearing");
    }

    /**
     * Even if a key was never pressed, {@link KeyboardState#keyReleased(KeyCode)}
     * should still record it in the released set.
     */
    @Test
    void testReleaseWithoutPress() {
        KeyboardState.keyReleased(KeyCode.D);

        assertFalse(KeyboardState.isKeyPressed(KeyCode.D), "Key D should not be pressed");
        assertTrue(KeyboardState.anyKeyReleased(), "Release without press should still be recorded");

        Set<KeyCode> released = KeyboardState.getAndClearReleasedKeys();
        assertEquals(Set.of(KeyCode.D), released, "Released set should contain only D");
    }

    /**
     * Confirms that with no interactions, the keyboard state methods
     * report no keys pressed or released.
     */
    @Test
    void testAnyKeyPressedAndReleasedEmptyByDefault() {
        assertFalse(KeyboardState.anyKeyPressed(), "No key should be reported as pressed by default");
        assertFalse(KeyboardState.anyKeyReleased(), "No key should be reported as released by default");
        assertTrue(KeyboardState.getPressedKeys().isEmpty(), "Pressed-keys snapshot should be empty");
    }
}
