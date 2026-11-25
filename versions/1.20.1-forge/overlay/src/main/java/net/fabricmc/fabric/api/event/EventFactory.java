package net.fabricmc.fabric.api.event;

import java.lang.reflect.Array;
import java.util.function.Function;

public final class EventFactory
{
    public static <T> Event<T> createArrayBacked(Class<? super T> type, Function<T[], T> invokerFactory)
    {
        return new ArrayBacked<T>(type, null, invokerFactory);
    }

    public static <T> Event<T> createArrayBacked(Class<T> type, T emptyInvoker, Function<T[], T> invokerFactory)
    {
        return new ArrayBacked<T>(type, emptyInvoker, invokerFactory);
    }

    private static class ArrayBacked<T> extends Event<T>
    {
        private final Class<?> type;
        private final T empty;
        private final Function<T[], T> factory;
        private T[] handlers;

        @SuppressWarnings("unchecked")
        ArrayBacked(Class<?> type, T empty, Function<T[], T> factory)
        {
            this.type = type;
            this.empty = empty;
            this.factory = factory;
            this.handlers = (T[]) Array.newInstance(type, 0);

            this.rebuild();
        }

        private void rebuild()
        {
            this.invoker = this.handlers.length == 0 && this.empty != null ? this.empty : this.factory.apply(this.handlers);
        }

        @Override
        @SuppressWarnings("unchecked")
        public synchronized void register(T listener)
        {
            T[] next = (T[]) Array.newInstance(this.type, this.handlers.length + 1);

            System.arraycopy(this.handlers, 0, next, 0, this.handlers.length);
            next[this.handlers.length] = listener;

            this.handlers = next;
            this.rebuild();
        }
    }
}
