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

    public void advance(QLSegment<K, V> cache, long currentTimeNanos) {
        long previousTimeNanos = nanos;
        nanos = currentTimeNanos;
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

    public void schedule(QLCacheNode<K, V> node) {
        QLCacheNode<K, V> sentinel = findBucket(node.getVariableTime());
        link(sentinel, node);
    }

    public void reschedule(QLCacheNode<K, V> node) {
        if (node.getNextInVariableOrder() != null) {
            unlink(node);
            schedule(node);
        }
    }

    public void deschedule(QLCacheNode<K, V> node) {
        unlink(node);
        node.setNextInVariableOrder(null);
        node.setPreviousInVariableOrder(null);
    }

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

    void link(QLCacheNode<K, V> sentinel, QLCacheNode<K, V> node) {
        node.setPreviousInVariableOrder(sentinel.getPreviousInVariableOrder());
        node.setNextInVariableOrder(sentinel);

        sentinel.getPreviousInVariableOrder().setNextInVariableOrder(node);
        sentinel.setPreviousInVariableOrder(node);
    }

    void unlink(QLCacheNode<K, V> node) {
        QLCacheNode<K, V> next = node.getNextInVariableOrder();
        if (next != null) {
            QLCacheNode<K, V> prev = node.getPreviousInVariableOrder();
            next.setPreviousInVariableOrder(prev);
            prev.setNextInVariableOrder(next);
        }
    }

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

    @Override
    public Iterator<QLCacheNode<K, V>> iterator() {
        return new AscendingIterator();
    }

    public Iterator<QLCacheNode<K, V>> descendingIterator() {
        return new DescendingIterator();
    }

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

        abstract boolean isDone();

        abstract QLCacheNode<K, V> sentinel();

        abstract QLCacheNode<K, V> traverse(QLCacheNode<K, V> node);

        abstract @Nullable QLCacheNode<K, V> goToNextBucket();

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
        @Override public void setPreviousInVariableOrder(@Nullable QLCacheNode<K, V> prev) {
            this.prev = prev;
        }
        @Override public QLCacheNode<K, V> getNextInVariableOrder() {
            return next;
        }
        @Override public void setNextInVariableOrder(@Nullable QLCacheNode<K, V> next) {
            this.next = next;
        }

        @Override public @Nullable K getKey() { return null; }
        @Override public @Nullable V getValue() { return null; }

    }
}
