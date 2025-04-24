package ch.unibas.dmi.dbis.cs108.example.highscore;

public class LevelTimer {

    // LevelTimer (Singleton)
    private static LevelTimer instance;

    // timevariables
    private long startTime;
    private long endTime;
    private boolean isRunning;
    private LevelTimer() {
        this.isRunning = false;
    }

    public static LevelTimer getInstance() {
        if (instance == null) {
            instance = new LevelTimer();
        }
        return instance;
    }


    public void start() {
        if (!isRunning) {
            startTime = System.currentTimeMillis();
            isRunning = true;
            System.out.println("Timer started at: " + startTime);
        }
    }

    // Stop the timer
    public void stop() {
        if (isRunning) {
            endTime = System.currentTimeMillis();
            isRunning = false;
            System.out.println("Timer stopped at: " + endTime);
        }
    }



    // returns the time
    public long getElapsedTimeInSeconds() {
        if (isRunning) {
            return (System.currentTimeMillis() - startTime) / 1000;
        }
        return (endTime - startTime) / 1000;
    }
}
