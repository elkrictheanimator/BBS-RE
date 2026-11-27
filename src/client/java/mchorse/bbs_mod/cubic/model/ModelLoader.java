package mchorse.bbs_mod.cubic.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * The background side of model loading: one permanent daemon worker with a direct queue
 * (scene/world models) and a bounded preview queue (form picker cells), so scrolling through
 * a large model folder never floods the CPU or starves the integrated server thread.
 */
public class ModelLoader
{
    /** Maximum preview models parsed/queued concurrently by the form picker. */
    public static final int MAX_PREVIEW_IN_FLIGHT = 2;

    /** Brief pause between background preview parses so 2-core mobile CPUs keep server ticks on time. */
    private static final long PREVIEW_YIELD_MS = 8L;

    private final ModelManager manager;
    private final Deque<String> queue = new ArrayDeque<>();
    private final Deque<String> previewQueue = new ArrayDeque<>();

    private Thread thread;
    private boolean previewActive;

    public ModelLoader(ModelManager manager)
    {
        this.manager = manager;
    }

    private void ensureThread()
    {
        if (this.thread == null || !this.thread.isAlive())
        {
            this.thread = new Thread(this::work, "BBS model loader");
            this.thread.setDaemon(true);
            this.thread.start();
        }
    }

    public synchronized void add(String key)
    {
        this.ensureThread();
        this.previewQueue.remove(key);
        this.queue.offerFirst(key);
        this.notifyAll();
    }

    /**
     * Enqueue a form-picker preview model only if the bounded preview window has room.
     * Unqueued cells stay lightweight placeholders until visible capacity opens up.
     */
    public synchronized boolean offerPreview(String key, boolean priority, Set<String> requested)
    {
        int limit = priority ? MAX_PREVIEW_IN_FLIGHT + 1 : MAX_PREVIEW_IN_FLIGHT;
        int active = (this.previewActive ? 1 : 0) + this.previewQueue.size();

        if (active >= limit)
        {
            return false;
        }

        if (!requested.add(key))
        {
            if (priority && this.previewQueue.remove(key))
            {
                this.previewQueue.offerFirst(key);
            }

            return false;
        }

        this.ensureThread();

        if (priority)
        {
            this.previewQueue.offerFirst(key);
        }
        else
        {
            this.previewQueue.offerLast(key);
        }

        this.notifyAll();

        return true;
    }

    public synchronized void promotePreview(String key)
    {
        if (this.previewQueue.remove(key))
        {
            this.previewQueue.offerFirst(key);
        }
    }

    /**
     * Drop preview requests that have not started loading yet (e.g. when leaving a folder),
     * releasing their marks in {@code requested} so visiting that folder later can load them.
     */
    public synchronized void clearPendingPreviews(Set<String> requested)
    {
        while (!this.previewQueue.isEmpty())
        {
            String pending = this.previewQueue.pollFirst();

            if (pending != null)
            {
                requested.remove(pending);
            }
        }
    }

    private void work()
    {
        while (true)
        {
            String model;
            boolean fromPreview = false;

            synchronized (this)
            {
                while (this.queue.isEmpty() && this.previewQueue.isEmpty())
                {
                    try
                    {
                        this.wait();
                    }
                    catch (InterruptedException e)
                    {
                        return;
                    }
                }

                model = this.queue.pollFirst();

                if (model == null)
                {
                    model = this.previewQueue.pollFirst();
                    fromPreview = true;
                    this.previewActive = true;
                }
            }

            try
            {
                this.manager.loadModel(model);
            }
            catch (Throwable e)
            {
                this.manager.markFailed(model);
                e.printStackTrace();
            }
            finally
            {
                if (fromPreview)
                {
                    synchronized (this)
                    {
                        this.previewActive = false;
                    }

                    try
                    {
                        Thread.sleep(PREVIEW_YIELD_MS);
                    }
                    catch (InterruptedException e)
                    {
                        return;
                    }
                }
            }
        }
    }
}
