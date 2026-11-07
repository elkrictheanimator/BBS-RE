package com.bbsvfx.bbsvfx.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Lets each core shader be registered once per resource reload, however many times Fabric's
 * {@code CoreShaderRegistrationCallback} fires during it.
 *
 * <p>On some Fabric Loader / Fabric API combinations that callback was seen firing dozens of times
 * inside a single {@code GameRenderer.loadPrograms} call. Every firing makes {@code context.register}
 * build (compile + link) a fresh {@code ShaderProgram}, so one startup compiled the same shaders ~60
 * times - almost five minutes on an integrated GPU - and left every earlier copy unclosed.
 *
 * <p>All firings of one reload land inside one client tick (the reload is a single synchronous call
 * on the render thread), so the guard is simply reset at the start of each tick: the first firing of
 * a reload registers, the repeats inside it are skipped, and the next reload - always in a later
 * tick - starts clean and re-registers, which it must, since vanilla closes every program on reload.
 */
public final class ShaderRegistrationGuard
{
    private static final Logger LOG = LoggerFactory.getLogger("bbsvfx");

    /** One shader registration; may throw because {@code context.register} does. */
    @FunctionalInterface
    public interface Registration
    {
        void run() throws IOException;
    }

    private static final Set<String> DONE = new HashSet<>();
    private static int skipped;

    private ShaderRegistrationGuard()
    {}

    /** Forget what has been registered: the next reload registers everything again. */
    public static synchronized void newLoad()
    {
        if (skipped > 0)
        {
            LOG.info("[bbsvfx] skipped {} duplicate core shader registrations during the last shader load", skipped);
        }

        DONE.clear();
        skipped = 0;
    }

    /** True the first time the key is seen since the last {@link #newLoad()}, false for every repeat. */
    public static synchronized boolean first(String key)
    {
        if (DONE.add(key))
        {
            return true;
        }

        skipped += 1;

        return false;
    }

    /** Run the registration unless this key was already registered in the current shader load. */
    public static void once(String key, Registration registration) throws IOException
    {
        if (first(key))
        {
            registration.run();
        }
    }
}
