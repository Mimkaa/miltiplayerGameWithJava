package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

/**
 * Simple singleton for background music and sound effects.
 */
public class SoundManager {
    public static final String BACKGROUND_MUSIC_PATH = "/audio/thinkOutsideTheSoundtrack.mp3";
    public static final String JUMP_PATH = "/audio/jump.mp3";
    public static final String GRABB_PATH = "/audio/grabb.mp3";
    public static final String THROW_PATH = "/audio/throw.mp3";

    private static double effectsVolume = 1.0;




    private static MediaPlayer mediaPlayer;

    /**
     * Prepares the background track. Logs lookup, ready and error events.
     */
    public static void initBackgroundMusic() {
        // 1) verify the resource really exists
        URL url = SoundManager.class.getResource(BACKGROUND_MUSIC_PATH);
        System.out.println("BG music URL → " + url);
        if (url == null) {
            throw new IllegalStateException("Couldn’t find " + BACKGROUND_MUSIC_PATH);
        }

        // 2) build the Media / MediaPlayer
        String uri = url.toExternalForm();
        Media media = new Media(uri);
        mediaPlayer = new MediaPlayer(media);

        // 3) hook up ready / error listeners
        mediaPlayer.setOnError(() -> {
            System.err.println("MediaPlayer error: " + mediaPlayer.getError().getMessage());
            mediaPlayer.getError().printStackTrace();
        });
        mediaPlayer.setOnReady(() -> {
            System.out.println("Background track ready; duration = " +
                    mediaPlayer.getMedia().getDuration().toSeconds() + "s");
        });

        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer.setVolume(0.4);
    }

    /** Starts playback of the background track (looping). */
    public static void playBackground() {
        if (mediaPlayer != null) {
            System.out.println("Starting background music playback...");
            mediaPlayer.play();
        } else {
            System.err.println("Cannot play background music: MediaPlayer is null");
        }
    }

    public static void pauseBackground() {
        if (mediaPlayer != null) mediaPlayer.pause();
    }

    public static void stopBackground() {
        if (mediaPlayer != null) mediaPlayer.stop();
    }

    public static void setBackgroundVolume(double volume) {
        if (mediaPlayer != null) mediaPlayer.setVolume(volume);
    }

    /**
     * Play a one-shot sound effect given its resource path.
     * Logs any failures, so you'll see exactly what’s going on.
     */
    private static void playEffect(String resourcePath) {
        URL res = SoundManager.class.getResource(resourcePath);
        if (res == null) {
            System.err.println("Sound resource not found: " + resourcePath);
            return;
        }

        String uri = res.toExternalForm();
        System.out.println("Playing SFX from: " + uri);

        try {
            Media media = new Media(uri);
            MediaPlayer sfxPlayer = new MediaPlayer(media);
            sfxPlayer.setOnEndOfMedia(sfxPlayer::dispose);
            sfxPlayer.setVolume(effectsVolume);
            sfxPlayer.play();
        } catch (Exception ex) {
            System.err.println("Failed to play SFX: " + resourcePath);
            ex.printStackTrace();
        }
    }


    public static void setEffectsVolume(double volume) {
        effectsVolume = volume;
    }

    /** Play the predefined jump sound effect. */
    public static void playJump() {
        playEffect(JUMP_PATH);
    }

    public static void playGrabb() {
        playEffect(GRABB_PATH);
    }

    public static void playThrow() {
        playEffect(THROW_PATH);
    }
}

