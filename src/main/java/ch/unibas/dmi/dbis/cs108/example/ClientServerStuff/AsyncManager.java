package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The {@code AsyncManager} class provides a set of static methods for executing tasks asynchronously
 * in a background thread pool. It encapsulates a fixed-size {@link ExecutorService} of 100 threads
 * and offers methods to run tasks that do not return a result ({@link #run(Runnable)}), tasks
 * that do return a result ({@link #run(Callable)}), and a task in a continuous loop
 * ({@link #runLoop(Runnable)}).
 * <p>
 * This class can be used whenever you need to offload work to a separate thread or set of threads,
 * without blocking the main application flow. Be sure to call {@link #shutdown()} when you no
 * longer need the service, to release resources properly.
 * </p>
 */
public class AsyncManager {

    /**
     * A fixed-size thread pool to handle all asynchronous tasks.
     */
    private static final ExecutorService executor = Executors.newFixedThreadPool(100);

    /**
     * Submits a {@link Runnable} task to the internal thread pool for asynchronous execution.
     * This method returns immediately, and the task will run in the background.
     *
     * @param task The {@link Runnable} to execute asynchronously.
     */
    public static void run(Runnable task) {
        executor.submit(task);
    }

    /**
     * Submits a {@link Callable} task to the internal thread pool for asynchronous execution,
     * returning a {@link Future} representing the pending result of the task.
     *
     * @param <T>  The type of the result returned by the {@link Callable}.
     * @param task The {@link Callable} to execute asynchronously.
     * @return A {@link Future} representing the pending completion of the task.
     */
    public static <T> Future<T> run(Callable<T> task) {
        return executor.submit(task);
    }

    /**
     * Runs a task in an infinite loop, asynchronously. Internally, this method submits a new task
     * that repeatedly invokes the provided {@link Runnable}, catching and logging any exceptions.
     * A brief sleep is added between iterations to avoid busy spinning.
     * <p>
     * Use this method when you want a background process running continuously until manually
     * interrupted, such as polling a resource or processing a queue.
     * </p>
     *
     * @param task The {@link Runnable} task to be executed in a loop.
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

    /**
     * Shuts down the internal thread pool immediately, attempting to halt all actively executing tasks.
     * Tasks that have not yet started may not be executed. Call this method when no more tasks
     * will be submitted, or during application shutdown, to release thread pool resources.
     */
    public static void shutdown() {
        executor.shutdownNow();
    }
}
