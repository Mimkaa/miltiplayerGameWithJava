package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AsyncManager {
    // Encapsulated executor service.
    private static final ExecutorService executor = Executors.newFixedThreadPool(100);

    // Run a task asynchronously without caring about the result.
    public static void run(Runnable task) {
        executor.submit(task);
    }

    // Run a task asynchronously and return a Future result.
    public static <T> Future<T> run(Callable<T> task) {
        return executor.submit(task);
    }
    
    /**
     * Runs a task continuously in a loop asynchronously.
     * This method submits a new task that repeatedly calls the provided Runnable.
     * A brief sleep is added between iterations to avoid busy spinning.
     *
     * @param task The Runnable task to be executed in a loop.
     */
    public static void runLoop(Runnable task) {
        executor.submit(() -> {
            while (true) {
                try {
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Pause briefly to avoid busy spinning.
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    // Shuts down the executor.
    public static void shutdown() {
        executor.shutdownNow();
    }
}
