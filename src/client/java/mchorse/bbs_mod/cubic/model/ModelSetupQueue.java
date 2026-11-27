package mchorse.bbs_mod.cubic.model;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The main-thread tail of model loading — VAO baking and other GL work a freshly parsed model
 * needs — drained with a per-frame time budget.
 *
 * <p>These tasks used to go through the client executor, whose queue is emptied WHOLE once per
 * frame: every model the background loader finished since the last frame baked in the same
 * frame, which is a hard freeze when a palette's worth of models arrives at once. Here the
 * frame takes tasks until the budget runs out and leaves the rest for the next one — a model
 * appears a frame or two later, drawn through the CPU fallback in the meantime, and the frame
 * never stalls.</p>
 */
public class ModelSetupQueue
{
    /** How much of a frame the bakes may take. */
    private static final long BUDGET_NANOS = 4_000_000L;

    private static final Queue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger PENDING = new AtomicInteger();

    public static void add(Runnable task)
    {
        PENDING.incrementAndGet();
        QUEUE.add(task);
    }

    public static int size()
    {
        return Math.max(0, PENDING.get());
    }

    /** Called once per frame from the render begin hook, on the render thread. */
    public static void drain()
    {
        if (QUEUE.isEmpty())
        {
            return;
        }

        long start = System.nanoTime();
        Runnable task;

        while ((task = QUEUE.poll()) != null)
        {
            PENDING.decrementAndGet();

            try
            {
                task.run();
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }

            if (System.nanoTime() - start >= BUDGET_NANOS)
            {
                break;
            }
        }
    }
}
