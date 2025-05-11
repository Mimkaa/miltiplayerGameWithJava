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



    private static MediaPlayer mediaPlayer;

    public static void initBackgroundMusic() {
        String uri = SoundManager.class.getResource(BACKGROUND_MUSIC_PATH).toExternalForm();
        Media media = new Media(uri);
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer.setVolume(0.5);
    }

    public static void playBackground() {
        if (mediaPlayer != null) mediaPlayer.play();
    }

    public static void pauseBackground() {
        if (mediaPlayer != null) mediaPlayer.pause();
    }

    public static void stopBackground() {
        if (mediaPlayer != null) mediaPlayer.stop();
    }

    /**
     * Play a one-shot sound effect given its resource path.
     * Logs any failures, so you'll see exactly what’s going on.
     */
    private static void playEffect(String resourcePath) {
        URL res = SoundManager.class.getResource(resourcePath);
        if (res == null) {
            System.err.println("❌ Sound resource not found: " + resourcePath);
            return;
        }

        String uri = res.toExternalForm();
        System.out.println("▶︎ Playing SFX from: " + uri);

        try {
            Media media = new Media(uri);
            MediaPlayer sfxPlayer = new MediaPlayer(media);
            sfxPlayer.setOnEndOfMedia(sfxPlayer::dispose);
            sfxPlayer.play();
        } catch (Exception ex) {
            System.err.println("⚠️ Failed to play SFX: " + resourcePath);
            ex.printStackTrace();
        }
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

