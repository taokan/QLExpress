package com.alibaba.qlexpress4.cache;

/**
 * @Author TaoKan
 * @Date 2024/3/25 2:39 PM
 */
public interface QLTicker {

    /**
     * Returns the number of nanoseconds elapsed since this ticker's fixed point of reference.
     *
     * @return the number of nanoseconds elapsed since this ticker's fixed point of reference
     */
    long read();

    /**
     * Returns a ticker that reads the current time using {@link System#nanoTime}.
     *
     * @return a ticker that reads the current time using {@link System#nanoTime}
     */
    static QLTicker systemTicker() {
        return SystemTicker.INSTANCE;
    }

    /**
     * Returns a ticker that always returns {@code 0}.
     *
     * @return a ticker that always returns {@code 0}
     */
    static QLTicker disabledTicker() {
        return DisabledTicker.INSTANCE;
    }
}

enum SystemTicker implements QLTicker {
    INSTANCE;

    @Override public long read() {
        return System.nanoTime();
    }
}

enum DisabledTicker implements QLTicker {
    INSTANCE;

    @Override public long read() {
        return 0L;
    }
}
