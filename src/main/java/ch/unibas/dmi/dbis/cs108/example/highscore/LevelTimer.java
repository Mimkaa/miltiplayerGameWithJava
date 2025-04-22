package ch.unibas.dmi.dbis.cs108.example.highscore;

public class LevelTimer {

    private long startTime;
    private long endTime;
    private boolean isRunning;

    public LevelTimer() {
        this.isRunning = false;
    }

    // starts the timer
    public void start() {
        if (!isRunning) {
            startTime = System.currentTimeMillis();
            isRunning = true;
        }
    }

    // Stopts the timer
    public void stop() {
        if (isRunning) {
            endTime = System.currentTimeMillis();
            isRunning = false;
        }
    }

    // returns the time in seconds
    public long getElapsedTimeInSeconds() {
        if (isRunning) {
            return (System.currentTimeMillis() - startTime) / 1000;
        }
        return (endTime - startTime) / 1000;
    }
}
