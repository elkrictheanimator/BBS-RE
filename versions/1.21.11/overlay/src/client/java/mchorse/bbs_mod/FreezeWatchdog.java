package mchorse.bbs_mod;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

/**
 * Diagnostic: when the render thread stops ticking for several seconds (a freeze), writes where
 * it is stuck to {@code bbs_freeze.txt} in the game folder and to the log.
 */
public class FreezeWatchdog
{
    private static volatile long beat = 0L;
    private static volatile boolean started;

    public static void start()
    {
        if (started)
        {
            return;
        }

        started = true;

        final Thread render = Thread.currentThread();

        ClientTickEvents.END_CLIENT_TICK.register((client) -> beat = System.currentTimeMillis());

        Thread dog = new Thread(() ->
        {
            long reported = 0L;

            while (true)
            {
                try
                {
                    Thread.sleep(1000L);
                }
                catch (InterruptedException e)
                {
                    return;
                }

                long last = beat;

                if (last == 0L || last == reported || System.currentTimeMillis() - last < 5000L)
                {
                    continue;
                }

                reported = last;

                StringBuilder sb = new StringBuilder("[BBS freeze] render thread has not ticked for 5s. Stack:\n");

                for (StackTraceElement e : render.getStackTrace())
                {
                    sb.append("    at ").append(e).append('\n');
                }

                for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet())
                {
                    if (entry.getKey() == render || entry.getValue().length == 0)
                    {
                        continue;
                    }

                    sb.append("  [").append(entry.getKey().getName()).append("]\n");

                    for (int i = 0; i < Math.min(8, entry.getValue().length); i++)
                    {
                        sb.append("    at ").append(entry.getValue()[i]).append('\n');
                    }
                }

                System.err.println(sb);

                try (FileWriter writer = new FileWriter(new File(MinecraftClient.getInstance().runDirectory, "bbs_freeze.txt"), true))
                {
                    writer.write(sb + "\n");
                }
                catch (Exception e)
                {}
            }
        }, "BBS freeze watchdog");

        dog.setDaemon(true);
        dog.start();
    }
}
