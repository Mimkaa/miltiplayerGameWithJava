package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The {@code AsyncManager} class provides a set of static methods for executing tasks asynchronously
 * in a background thread pool. It encapsulates a fixed-size {@link ExecutorService} with
 * 100 threads, offering methods to run tasks that do not return a result ({@link #run(Runnable)}),
 * tasks that return a result ({@link #run(Callable)}), and tasks running in a continuous loop
 * ({@link #runLoop(Runnable)}).
 *
 * <p>
 * Use this class whenever you need to offload work to a separate thread or set of threads,
 * without blocking the main application flow. Make sure to invoke {@link #shutdown()} when
 * the service is no longer needed, in order to release system resources properly.
 * </p>
 */
public class AsyncManager {

    /**
     * A fixed-size thread pool used to handle all asynchronous tasks.
     */
    private static final ExecutorService executor = Executors.newFixedThreadPool(100);

    /**
     * Submits a {@link Runnable} task for asynchronous execution.
     * This method returns immediately, and the task will run in the background.
     *
     * @param task The {@link Runnable} to execute asynchronously.
     */
    public static void run(Runnable task) {
        executor.submit(task);
    }

    /**
     * Submits a {@link Callable} task for asynchronous execution,
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
     * A brief sleep is inserted between iterations to prevent busy spinning.
     *
     * <p>
     * Use this method when you need a background process running continuously until manually
     * interrupted, such as polling a resource or continuously processing a queue.
     * </p>
     *
     * @param task The {@link Runnable} task to execute repeatedly.
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
                ///try {
                    //Thread.sleep(10);
                //} catch (InterruptedException e) {
                    //Thread.currentThread().interrupt();
                    //break;
                //}
            }
        });
    }

    /**
     * Immediately shuts down the internal thread pool, attempting to halt all actively
     * executing tasks. Tasks that have not yet started may not be executed.
     *
     * <p>
     * Call this method when no additional tasks will be submitted, or during application
     * shutdown, to release thread pool resources.
     * </p>
     */
    public static void shutdown() {
        executor.shutdownNow();
    }
}
