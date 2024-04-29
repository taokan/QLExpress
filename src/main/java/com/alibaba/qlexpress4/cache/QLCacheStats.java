package com.alibaba.qlexpress4.cache;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * @Author TaoKan
 * @Date 2024/4/29 3:45 PM
 */
public class QLCacheStats {
    private static final QLCacheStats EMPTY_STATS = of(0L, 0L, 0L, 0L, 0L, 0L, 0L);
    private final long hitCount;
    private final long missCount;
    private final long loadSuccessCount;
    private final long loadFailureCount;
    private final long totalLoadTime;
    private final long evictionCount;
    private final long evictionWeight;

    private QLCacheStats(long hitCount, long missCount, long loadSuccessCount, long loadFailureCount, long totalLoadTime, long evictionCount, long evictionWeight) {
        if (hitCount >= 0L && missCount >= 0L && loadSuccessCount >= 0L && loadFailureCount >= 0L && totalLoadTime >= 0L && evictionCount >= 0L && evictionWeight >= 0L) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.loadSuccessCount = loadSuccessCount;
            this.loadFailureCount = loadFailureCount;
            this.totalLoadTime = totalLoadTime;
            this.evictionCount = evictionCount;
            this.evictionWeight = evictionWeight;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static QLCacheStats of(long hitCount, long missCount, long loadSuccessCount, long loadFailureCount, long totalLoadTime, long evictionCount, long evictionWeight) {
        return new QLCacheStats(hitCount, missCount, loadSuccessCount, loadFailureCount, totalLoadTime, evictionCount, evictionWeight);
    }

    public static QLCacheStats empty() {
        return EMPTY_STATS;
    }

    public long requestCount() {
        return saturatedAdd(this.hitCount, this.missCount);
    }

    public long hitCount() {
        return this.hitCount;
    }

    public double hitRate() {
        long requestCount = this.requestCount();
        return requestCount == 0L ? 1.0 : (double)this.hitCount / (double)requestCount;
    }

    public long missCount() {
        return this.missCount;
    }

    public double missRate() {
        long requestCount = this.requestCount();
        return requestCount == 0L ? 0.0 : (double)this.missCount / (double)requestCount;
    }

    public long loadCount() {
        return saturatedAdd(this.loadSuccessCount, this.loadFailureCount);
    }

    public long loadSuccessCount() {
        return this.loadSuccessCount;
    }

    public long loadFailureCount() {
        return this.loadFailureCount;
    }

    public double loadFailureRate() {
        long totalLoadCount = saturatedAdd(this.loadSuccessCount, this.loadFailureCount);
        return totalLoadCount == 0L ? 0.0 : (double)this.loadFailureCount / (double)totalLoadCount;
    }

    public long totalLoadTime() {
        return this.totalLoadTime;
    }

    public double averageLoadPenalty() {
        long totalLoadCount = saturatedAdd(this.loadSuccessCount, this.loadFailureCount);
        return totalLoadCount == 0L ? 0.0 : (double)this.totalLoadTime / (double)totalLoadCount;
    }

    public long evictionCount() {
        return this.evictionCount;
    }

    public long evictionWeight() {
        return this.evictionWeight;
    }

    public QLCacheStats minus(QLCacheStats other) {
        return of(Math.max(0L, saturatedSubtract(this.hitCount, other.hitCount)), Math.max(0L, saturatedSubtract(this.missCount, other.missCount)), Math.max(0L, saturatedSubtract(this.loadSuccessCount, other.loadSuccessCount)), Math.max(0L, saturatedSubtract(this.loadFailureCount, other.loadFailureCount)), Math.max(0L, saturatedSubtract(this.totalLoadTime, other.totalLoadTime)), Math.max(0L, saturatedSubtract(this.evictionCount, other.evictionCount)), Math.max(0L, saturatedSubtract(this.evictionWeight, other.evictionWeight)));
    }

    public QLCacheStats plus(QLCacheStats other) {
        return of(saturatedAdd(this.hitCount, other.hitCount), saturatedAdd(this.missCount, other.missCount), saturatedAdd(this.loadSuccessCount, other.loadSuccessCount), saturatedAdd(this.loadFailureCount, other.loadFailureCount), saturatedAdd(this.totalLoadTime, other.totalLoadTime), saturatedAdd(this.evictionCount, other.evictionCount), saturatedAdd(this.evictionWeight, other.evictionWeight));
    }

    private static long saturatedSubtract(long a, long b) {
        long naiveDifference = a - b;
        return (a ^ b) >= 0L | (a ^ naiveDifference) >= 0L ? naiveDifference : Long.MAX_VALUE + (naiveDifference >>> 63 ^ 1L);
    }

    private static long saturatedAdd(long a, long b) {
        long naiveSum = a + b;
        return (a ^ b) < 0L | (a ^ naiveSum) >= 0L ? naiveSum : Long.MAX_VALUE + (naiveSum >>> 63 ^ 1L);
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.hitCount, this.missCount, this.loadSuccessCount, this.loadFailureCount, this.totalLoadTime, this.evictionCount, this.evictionWeight});
    }

    public boolean equals(@Nullable Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof QLCacheStats)) {
            return false;
        } else {
            QLCacheStats other = (QLCacheStats)o;
            return this.hitCount == other.hitCount && this.missCount == other.missCount && this.loadSuccessCount == other.loadSuccessCount && this.loadFailureCount == other.loadFailureCount && this.totalLoadTime == other.totalLoadTime && this.evictionCount == other.evictionCount && this.evictionWeight == other.evictionWeight;
        }
    }

    public String toString() {
        String var10000 = this.getClass().getSimpleName();
        return var10000 + "{hitCount=" + this.hitCount + ", missCount=" + this.missCount + ", loadSuccessCount=" + this.loadSuccessCount + ", loadFailureCount=" + this.loadFailureCount + ", totalLoadTime=" + this.totalLoadTime + ", evictionCount=" + this.evictionCount + ", evictionWeight=" + this.evictionWeight + "}";
    }
}
