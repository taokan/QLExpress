package com.alibaba.qlexpress4.cache;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import static com.alibaba.qlexpress4.cache.QLCacheMap.MAXIMUM_CAPACITY;


/**
 * @Author TaoKan
 * @Date 2024/2/18 9:13 PM
 */
public class QLCacheAddTask<K,V> implements Runnable {
    final QLCacheNode<K, V> node;
    final int weight;
    final long maximum;
    final QLSegment<K,V> segment;
    final QLFrequency<K> frequency;

    QLCacheAddTask(QLCacheNode<K, V> node, int weight, QLSegment qlSegment, QLFrequency<K> qlFrequency) {
        this.weight = weight;
        this.node = node;
        this.maximum = qlSegment.getMaxSegmentWeight();
        this.segment = qlSegment;
        this.frequency = qlFrequency;
    }

    @Override
    @GuardedBy("evictionLock")
    @SuppressWarnings("FutureReturnValueIgnored")
    public void run() {
        this.segment.setWeight(this.segment.getWeight() + this.weight);
        this.segment.getWindow().setWeight(this.segment.getWindow().getWeight() + this.weight);
        this.node.setWeight(this.node.getWeight() + this.weight);
        if (this.segment.getWeight() >= (maximum >>> 1)) {
            if (this.segment.getWeight() > MAXIMUM_CAPACITY) {
                this.segment.evictEntries();
            } else {
                long capacity = this.segment.getTable().mappingCount();
                this.frequency.ensureCapacity(capacity);
            }
        }
        K key = node.getKey();
        if (key != null) {
            this.frequency.increment(key);
        }
        this.segment.getStatistic().setMissesInSample(this.segment.getStatistic().getMissesInSample() + 1);
        // ignore out-of-order write operations
        if (expiresAfterWrite()) {
            writeOrderDeque().offerLast(node);
        }
        if (expiresVariable()) {
            timerWheel().schedule(node);
        }
        if (weight > maximum) {
            evictEntry(node, RemovalCause.SIZE, expirationTicker().read());
        } else if (weight > this.segment.getWindow().getMaxNum()) {
            accessOrderWindowDeque().offerFirst(node);
        } else {
            accessOrderWindowDeque().offerLast(node);
        }
    }
}