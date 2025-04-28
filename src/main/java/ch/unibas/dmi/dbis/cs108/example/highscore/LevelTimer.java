package ch.unibas.dmi.dbis.cs108.example.highscore;

/**
 * The {@code LevelTimer} class is a singleton responsible for measuring elapsed time
 * during a game level. It allows starting, stopping, and retrieving the elapsed time
 * in seconds.
 */
public class LevelTimer {

    // LevelTimer (Singleton)
    private static LevelTimer instance;

    // Time-related variables
    private long startTime;
    private long endTime;
    private boolean isRunning;

    /**
     * Private constructor for the singleton pattern. Initializes the timer to a stopped state.
     */
    private LevelTimer() {
        this.isRunning = false;
    }

    /**
     * Returns the single instance of the {@code LevelTimer}.
     *
     * @return The {@code LevelTimer} instance.
     */
    public static LevelTimer getInstance() {
        if (instance == null) {
            instance = new LevelTimer();
        }
        return instance;
    }

    /**
     * Starts the timer by recording the current system time.
     * If the timer is already running, this method has no effect.
     */
    public void start() {
        if (!isRunning) {
            startTime = System.currentTimeMillis();
            isRunning = true;
            System.out.println("Timer started at: " + startTime);
        }
    }

    /**
     * Stops the timer by recording the current system time and calculating the elapsed time.
     * If the timer is already stopped, this method has no effect.
     */
    public void stop() {
        if (isRunning) {
            endTime = System.currentTimeMillis();
            isRunning = false;
            System.out.println("Timer stopped at: " + endTime);
        }
    }

    /**
     * Returns the elapsed time in seconds. If the timer is still running, it calculates the time
     * since the timer started. If the timer is stopped, it returns the time between the start
     * and stop time.
     *
     * @return The elapsed time in seconds.
     */
    public long getElapsedTimeInSeconds() {
        if (isRunning) {
            return (System.currentTimeMillis() - startTime) / 1000;
        }
        return (endTime - startTime) / 1000;
    }
}
