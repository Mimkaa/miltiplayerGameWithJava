package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The TaskScheduler uses a blocking queue to automatically consume and execute
 * scheduled Runnable tasks without requiring a sleep loop.
 */
class TaskScheduler {
    private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public TaskScheduler() {
        // Start a loop to take and run tasks as they arrive.
        AsyncManager.runLoop(() -> {
            while (running) {
                try {
                    Runnable task = taskQueue.take();
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    public void scheduleTask(Runnable task) {
        taskQueue.offer(task);
    }

    public void stop() {
        running = false;
    }
}