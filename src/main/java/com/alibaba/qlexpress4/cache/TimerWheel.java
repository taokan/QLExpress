package com.alibaba.qlexpress4.cache;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import static com.alibaba.qlexpress4.cache.QLCacheMap.ceilingPowerOfTwo;

/**
 * @Author TaoKan
 * @Date 2024/3/25 2:16 PM
 */
public final class TimerWheel<K, V> implements Iterable<QLCacheNode<K, V>> {

    /*
     * A timer wheel [1] stores timer events in buckets on a circular buffer. A bucket represents a
     * coarse time span, e.g. one minute, and holds a doubly-linked list of events. The wheels are
     * structured in a hierarchy (seconds, minutes, hours, days) so that events scheduled in the
     * distant future are cascaded to lower buckets when the wheels rotate. This allows for events
     * to be added, removed, and expired in O(1) time, where expiration occurs for the entire bucket,
     * and the penalty of cascading is amortized by the rotations.
     *
     * [1] Hashed and Hierarchical Timing Wheels
     * http://www.cs.columbia.edu/~nahum/w6998/papers/ton97-timing-wheels.pdf
     */

    static final int[] BUCKETS = { 64, 64, 32, 4, 1 };
    static final long[] SPANS = {
            ceilingPowerOfTwo(TimeUnit.SECONDS.toNanos(1)), // 1.07s
            ceilingPowerOfTwo(TimeUnit.MINUTES.toNanos(1)), // 1.14m
            ceilingPowerOfTwo(TimeUnit.HOURS.toNanos(1)),   // 1.22h
            ceilingPowerOfTwo(TimeUnit.DAYS.toNanos(1)),    // 1.63d
            BUCKETS[3] * ceilingPowerOfTwo(TimeUnit.DAYS.toNanos(1)), // 6.5d
            BUCKETS[3] * ceilingPowerOfTwo(TimeUnit.DAYS.toNanos(1)), // 6.5d
    };
    static final long[] SHIFT = {
            Long.numberOfTrailingZeros(SPANS[0]),
            Long.numberOfTrailingZeros(SPANS[1]),
            Long.numberOfTrailingZeros(SPANS[2]),
            Long.numberOfTrailingZeros(SPANS[3]),
            Long.numberOfTrailingZeros(SPANS[4]),
    };

    final QLCacheNode<K, V>[][] wheel;

    long nanos;

    @SuppressWarnings({"rawtypes", "unchecked"})
    TimerWheel() {
        wheel = new QLCacheNode[BUCKETS.length][];
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new QLCacheNode[BUCKETS[i]];
            for (int j = 0; j < wheel[i].length; j++) {
                wheel[i][j] = new TimerWheel.Sentinel<>();
            }
        }
    }

    /**
     * Advances the timer and evicts entries that have expired.
     *
     * @param cache the instance that the entries belong to
     * @param currentTimeNanos the current time, in nanoseconds
     */
    @SuppressWarnings("PMD.UnusedAssignment")
    public void advance(QLSegment<K, V> cache, long currentTimeNanos) {
        long previousTimeNanos = nanos;
        nanos = currentTimeNanos;

        // If wrapping then temporarily shift the clock for a positive comparison. We assume that the
        // advancements never exceed a total running time of Long.MAX_VALUE nanoseconds (292 years)
        // so that an overflow only occurs due to using an arbitrary origin time (System.nanoTime()).
        if ((previousTimeNanos < 0) && (currentTimeNanos > 0)) {
            previousTimeNanos += Long.MAX_VALUE;
            currentTimeNanos += Long.MAX_VALUE;
        }

        try {
            for (int i = 0; i < SHIFT.length; i++) {
                long previousTicks = (previousTimeNanos >>> SHIFT[i]);
                long currentTicks = (currentTimeNanos >>> SHIFT[i]);
                long delta = (currentTicks - previousTicks);
                if (delta <= 0L) {
                    break;
                }
                expire(cache, i, previousTicks, delta);
            }
        } catch (Throwable t) {
            nanos = previousTimeNanos;
            throw t;
        }
    }

    /**
     * Expires entries or reschedules into the proper bucket if still active.
     *
     * @param cache the instance that the entries belong to
     * @param index the timing wheel being operated on
     * @param previousTicks the previous number of ticks
     * @param delta the number of additional ticks
     */
    void expire(QLSegment<K, V> cache, int index, long previousTicks, long delta) {
        QLCacheNode<K, V>[] timerWheel = wheel[index];
        int mask = timerWheel.length - 1;

        // We assume that the delta does not overflow an integer and cause negative steps. This can
        // occur only if the advancement exceeds 2^61 nanoseconds (73 years).
        int steps = Math.min(1 + (int) delta, timerWheel.length);
        int start = (int) (previousTicks & mask);
        int end = start + steps;

        for (int i = start; i < end; i++) {
            QLCacheNode<K, V> sentinel = timerWheel[i & mask];
            QLCacheNode<K, V> prev = sentinel.getPreviousInVariableOrder();
            QLCacheNode<K, V> node = sentinel.getNextInVariableOrder();
            sentinel.setPreviousInVariableOrder(sentinel);
            sentinel.setNextInVariableOrder(sentinel);

            while (node != sentinel) {
                QLCacheNode<K, V> next = node.getNextInVariableOrder();
                node.setPreviousInVariableOrder(null);
                node.setNextInVariableOrder(null);

                try {
                    if (((node.getVariableTime() - nanos) > 0)
                            || !cache.evictEntry(node, RemovalCause.EXPIRED, nanos)) {
                        schedule(node);
                    }
                    node = next;
                } catch (Throwable t) {
                    node.setPreviousInVariableOrder(sentinel.getPreviousInVariableOrder());
                    node.setNextInVariableOrder(next);
                    sentinel.getPreviousInVariableOrder().setNextInVariableOrder(node);
                    sentinel.setPreviousInVariableOrder(prev);
                    throw t;
                }
            }
        }
    }

    /**
     * Schedules a timer event for the node.
     *
     * @param node the entry in the cache
     */
    public void schedule(QLCacheNode<K, V> node) {
        QLCacheNode<K, V> sentinel = findBucket(node.getVariableTime());
        link(sentinel, node);
    }

    /**
     * Reschedules an active timer event for the node.
     *
     * @param node the entry in the cache
     */
    public void reschedule(QLCacheNode<K, V> node) {
        if (node.getNextInVariableOrder() != null) {
            unlink(node);
            schedule(node);
        }
    }

    /**
     * Removes a timer event for this entry if present.
     *
     * @param node the entry in the cache
     */
    public void deschedule(QLCacheNode<K, V> node) {
        unlink(node);
        node.setNextInVariableOrder(null);
        node.setPreviousInVariableOrder(null);
    }

    /**
     * Determines the bucket that the timer event should be added to.
     *
     * @param time the time when the event fires
     * @return the sentinel at the head of the bucket
     */
    QLCacheNode<K, V> findBucket(long time) {
        long duration = time - nanos;
        int length = wheel.length - 1;
        for (int i = 0; i < length; i++) {
            if (duration < SPANS[i + 1]) {
                long ticks = (time >>> SHIFT[i]);
                int index = (int) (ticks & (wheel[i].length - 1));
                return wheel[i][index];
            }
        }
        return wheel[length][0];
    }

    /** Adds the entry at the tail of the bucket's list. */
    void link(QLCacheNode<K, V> sentinel, QLCacheNode<K, V> node) {
        node.setPreviousInVariableOrder(sentinel.getPreviousInVariableOrder());
        node.setNextInVariableOrder(sentinel);

        sentinel.getPreviousInVariableOrder().setNextInVariableOrder(node);
        sentinel.setPreviousInVariableOrder(node);
    }

    /** Removes the entry from its bucket, if scheduled. */
    void unlink(QLCacheNode<K, V> node) {
        QLCacheNode<K, V> next = node.getNextInVariableOrder();
        if (next != null) {
            QLCacheNode<K, V> prev = node.getPreviousInVariableOrder();
            next.setPreviousInVariableOrder(prev);
            prev.setNextInVariableOrder(next);
        }
    }

    /** Returns the duration until the next bucket expires, or {@link Long#MAX_VALUE} if none. */
    @SuppressWarnings("IntLongMath")
    public long getExpirationDelay() {
        for (int i = 0; i < SHIFT.length; i++) {
            QLCacheNode<K, V>[] timerWheel = wheel[i];
            long ticks = (nanos >>> SHIFT[i]);

            long spanMask = SPANS[i] - 1;
            int start = (int) (ticks & spanMask);
            int end = start + timerWheel.length;
            int mask = timerWheel.length - 1;
            for (int j = start; j < end; j++) {
                QLCacheNode<K, V> sentinel = timerWheel[(j & mask)];
                QLCacheNode<K, V> next = sentinel.getNextInVariableOrder();
                if (next == sentinel) {
                    continue;
                }
                long buckets = (j - start);
                long delay = (buckets << SHIFT[i]) - (nanos & spanMask);
                delay = (delay > 0) ? delay : SPANS[i];

                for (int k = i + 1; k < SHIFT.length; k++) {
                    long nextDelay = peekAhead(k);
                    delay = Math.min(delay, nextDelay);
                }

                return delay;
            }
        }
        return Long.MAX_VALUE;
    }

    /**
     * Returns the duration when the wheel's next bucket expires, or {@link Long#MAX_VALUE} if empty.
     *
     * @param index the timing wheel being operated on
     */
    long peekAhead(int index) {
        long ticks = (nanos >>> SHIFT[index]);
        QLCacheNode<K, V>[] timerWheel = wheel[index];

        long spanMask = SPANS[index] - 1;
        int mask = timerWheel.length - 1;
        int probe = (int) ((ticks + 1) & mask);
        QLCacheNode<K, V> sentinel = timerWheel[probe];
        QLCacheNode<K, V> next = sentinel.getNextInVariableOrder();
        return (next == sentinel) ? Long.MAX_VALUE : (SPANS[index] - (nanos & spanMask));
    }

    /**
     * Returns an iterator roughly ordered by the expiration time from the entries most likely to
     * expire (oldest) to the entries least likely to expire (youngest). The wheels are evaluated in
     * order, but the timers that fall within the bucket's range are not sorted.
     */
    @Override
    public Iterator<QLCacheNode<K, V>> iterator() {
        return new AscendingIterator();
    }

    /**
     * Returns an iterator roughly ordered by the expiration time from the entries least likely to
     * expire (youngest) to the entries most likely to expire (oldest). The wheels are evaluated in
     * order, but the timers that fall within the bucket's range are not sorted.
     */
    public Iterator<QLCacheNode<K, V>> descendingIterator() {
        return new DescendingIterator();
    }

    /** An iterator with rough ordering that can be specialized for either direction. */
    abstract class Traverser implements Iterator<QLCacheNode<K, V>> {
        final long expectedNanos;

        @Nullable QLCacheNode<K, V> current;
        @Nullable QLCacheNode<K, V> next;

        Traverser() {
            expectedNanos = nanos;
        }

        @Override
        public boolean hasNext() {
            if (nanos != expectedNanos) {
                throw new ConcurrentModificationException();
            } else if (next != null) {
                return true;
            } else if (isDone()) {
                return false;
            }
            next = computeNext();
            return (next != null);
        }

        @Override
        @SuppressWarnings("NullAway")
        public QLCacheNode<K, V> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            current = next;
            next = null;
            return current;
        }

        @Nullable QLCacheNode<K, V> computeNext() {
            QLCacheNode node = (current == null) ? sentinel() : current;
            for (;;) {
                node = traverse(node);
                if (node != sentinel()) {
                    return node;
                } else if ((node = goToNextBucket()) != null) {
                    continue;
                } else if ((node = goToNextWheel()) != null) {
                    continue;
                }
                return null;
            }
        }

        /** Returns if the iteration has completed. */
        abstract boolean isDone();

        /** Returns the sentinel at the current wheel and bucket position. */
        abstract QLCacheNode<K, V> sentinel();

        /** Returns the node's successor, or the bucket's sentinel if at the end. */
        abstract QLCacheNode<K, V> traverse(QLCacheNode<K, V> node);

        /** Returns the sentinel for the wheel's next bucket, or null if the wheel is exhausted. */
        abstract @Nullable QLCacheNode<K, V> goToNextBucket();

        /** Returns the sentinel for the next wheel's bucket position, or null if no more wheels. */
        abstract @Nullable QLCacheNode<K, V> goToNextWheel();
    }

    final class AscendingIterator extends Traverser {
        int wheelIndex;
        int steps;

        @Override boolean isDone() {
            return (wheelIndex == wheel.length);
        }
        @Override QLCacheNode<K, V> sentinel() {
            return wheel[wheelIndex][bucketIndex()];
        }
        @Override QLCacheNode<K, V> traverse(QLCacheNode<K, V> node) {
            return node.getNextInVariableOrder();
        }
        @Override @Nullable QLCacheNode<K, V> goToNextBucket() {
            return (++steps < wheel[wheelIndex].length)
                    ? wheel[wheelIndex][bucketIndex()]
                    : null;
        }
        @Override @Nullable QLCacheNode<K, V> goToNextWheel() {
            if (++wheelIndex == wheel.length) {
                return null;
            }
            steps = 0;
            return wheel[wheelIndex][bucketIndex()];
        }
        int bucketIndex() {
            int ticks = (int) (nanos >>> SHIFT[wheelIndex]);
            int bucketMask = wheel[wheelIndex].length - 1;
            int bucketOffset = (ticks & bucketMask) + 1;
            return (bucketOffset + steps) & bucketMask;
        }
    }

    final class DescendingIterator extends Traverser {
        int wheelIndex;
        int steps;

        DescendingIterator() {
            wheelIndex = wheel.length - 1;
        }
        @Override boolean isDone() {
            return (wheelIndex == -1);
        }
        @Override QLCacheNode<K, V> sentinel() {
            return wheel[wheelIndex][bucketIndex()];
        }
        @Override @Nullable QLCacheNode<K, V> goToNextBucket() {
            return (++steps < wheel[wheelIndex].length)
                    ? wheel[wheelIndex][bucketIndex()]
                    : null;
        }
        @Override @Nullable QLCacheNode<K, V> goToNextWheel() {
            if (--wheelIndex < 0) {
                return null;
            }
            steps = 0;
            return wheel[wheelIndex][bucketIndex()];
        }
        @Override QLCacheNode<K, V> traverse(QLCacheNode<K, V> node) {
            return node.getPreviousInVariableOrder();
        }
        int bucketIndex() {
            int ticks = (int) (nanos >>> SHIFT[wheelIndex]);
            int bucketMask = wheel[wheelIndex].length - 1;
            int bucketOffset = (ticks & bucketMask);
            return (bucketOffset - steps) & bucketMask;
        }
    }

    /** A sentinel for the doubly-linked list in the bucket. */
    static final class Sentinel<K, V> extends QLCacheNode<K, V> {
        QLCacheNode<K, V> prev;
        QLCacheNode<K, V> next;

        Sentinel() {
            super();
            prev = next = this;
        }

        @Override public QLCacheNode<K, V> getPreviousInVariableOrder() {
            return prev;
        }
        @SuppressWarnings("NullAway")
        @Override public void setPreviousInVariableOrder(@Nullable QLCacheNode<K, V> prev) {
            this.prev = prev;
        }
        @Override public QLCacheNode<K, V> getNextInVariableOrder() {
            return next;
        }
        @SuppressWarnings("NullAway")
        @Override public void setNextInVariableOrder(@Nullable QLCacheNode<K, V> next) {
            this.next = next;
        }

        @Override public @Nullable K getKey() { return null; }
//        @Override public Object getKeyReference() { throw new UnsupportedOperationException(); }
        @Override public @Nullable V getValue() { return null; }
//        @Override public Object getValueReference() { throw new UnsupportedOperationException(); }
//        @Override public void setValue(V value, @Nullable ReferenceQueue<V> referenceQueue) {}
//        @Override public boolean containsValue(Object value) { return false; }
//        @Override public boolean isAlive() { return false; }
//        @Override public boolean isRetired() { return false; }
//        @Override public boolean isDead() { return false; }
//        @Override public void retire() {}
//        @Override public void die() {}
    }
}
