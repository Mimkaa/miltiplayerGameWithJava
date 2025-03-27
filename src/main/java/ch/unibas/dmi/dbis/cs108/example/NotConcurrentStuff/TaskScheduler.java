package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskScheduler {
    // Internal queue for tasks.
    private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

    /**
     * Schedule a task to be executed later.
     *
     * @param task The task (Runnable) to schedule.
     */
    public void scheduleTask(Runnable task) {
        taskQueue.offer(task);
    }

    /**
     * Synchronously run all tasks currently in the queue.
     * This method runs tasks on the calling thread.
     */
    public void runAllTasks() {
        Runnable task;
        while ((task = taskQueue.poll()) != null) {
            task.run();
        }
    }

    
}

