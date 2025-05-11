package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Simple singleton for background music and modular sound effects.
 * Background music path is fixed internally; no need to pass it from the GUI.
 */
public class SoundManager {
    private static final String BACKGROUND_MUSIC_PATH = "/audio/thinkOutsideTheSoundtrack.mp3";
    private static MediaPlayer mediaPlayer;

    /**
     * Initialize and loop the fixed background music. Call once on the JavaFX Application thread.
     */
    public static void initBackgroundMusic() {
        String uri = SoundManager.class
                .getResource(BACKGROUND_MUSIC_PATH)
                .toExternalForm();
        Media media = new Media(uri);
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer.setVolume(0.5);
    }

    /** Play or resume background music. */
    public static void playBackground() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }

    /** Pause background music. */
    public static void pauseBackground() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    /** Stop background music and reset to start. */
    public static void stopBackground() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    // --------------------
    // Key-to-sound binding
    // --------------------

    /**
     * Bind a specific KeyCode to play a sound effect when pressed.
     * @param scene the JavaFX Scene to attach the listener to
     * @param key the KeyCode that will trigger the sound
     * @param resourcePath path under /resources, e.g. "/audio/sfx/jump.mp3"
     */
    public static void bindKeyToSound(Scene scene, KeyCode key, String resourcePath) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, evt -> {
            // Only play effect if the Canvas (game area) has focus
            if (scene.getFocusOwner() instanceof Canvas && evt.getCode() == key) {
                playEffect(resourcePath);
            }
        });
    }

    // --------------------
    // Sound effect helpers
    // --------------------

    /**
     * Play a one-shot sound effect given its resource path.
     */
    private static void playEffect(String resourcePath) {
        String uri = SoundManager.class
                .getResource(resourcePath)
                .toExternalForm();
        Media media = new Media(uri);
        MediaPlayer sfxPlayer = new MediaPlayer(media);
        sfxPlayer.setOnEndOfMedia(sfxPlayer::dispose);
        sfxPlayer.play();
    }

    /** Play the predefined jump sound effect. */
    public static void playJump() {
        playEffect("/audio/jump.mp3");
    }

}
