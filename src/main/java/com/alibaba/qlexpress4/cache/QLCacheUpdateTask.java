package com.alibaba.qlexpress4.cache;

import com.google.errorprone.annotations.concurrent.GuardedBy;

import static com.alibaba.qlexpress4.cache.QLCacheMap.MAXIMUM_CAPACITY;

/**
 * @Author TaoKan
 * @Date 2024/2/18 9:15 PM
 */
public class QLCacheUpdateTask<K,V> implements Runnable {
    final QLCacheNode<K, V> node;
    final QLCacheNode<K, V> oldNode;
    final int weight;
    final long maximum;
    final QLSegment<K,V> segment;
    final QLFrequency<K> frequency;

    QLCacheUpdateTask(QLCacheNode<K, V> node,QLCacheNode<K, V> oldNode, int weight, QLSegment qlSegment, QLFrequency<K> qlFrequency) {
        this.weight = weight;
        this.node = node;
        this.oldNode = oldNode;
        this.maximum = qlSegment.getMaxSegmentWeight();
        this.segment = qlSegment;
        this.frequency = qlFrequency;
    }

    @Override
    @GuardedBy("evictionLock")
    @SuppressWarnings("FutureReturnValueIgnored")
    public void run() {
        if (this.segment.getCacheV().expiresAfterWrite()) {
            reorder(writeOrderDeque(), node);
        } else if (this.segment.getCacheV().expiresVariable()) {
            this.segment.getCacheV().timerWheel().reschedule(node);
        }
        if (evicts()) {
            int oldWeightedSize = node.getPolicyWeight();
            node.setPolicyWeight(oldWeightedSize + weightDifference);
            if (node.inWindow()) {
                setWindowWeightedSize(windowWeightedSize() + weightDifference);
                if (node.getPolicyWeight() > maximum) {
                    this.segment.evictEntry(node, RemovalCause.SIZE, this.segment.getCacheV().expirationTicker().read());
                } else if (node.getPolicyWeight() <= windowMaximum()) {
                    onAccess(node);
                } else if (this.segment.getCacheV().accessOrderWindowDeque().contains(node)) {
                    this.segment.getCacheV().accessOrderWindowDeque().moveToFront(node);
                }
            } else if (node.inMainProbation()) {
                if (node.getPolicyWeight() <= maximum) {
                    onAccess(node);
                } else {
                    this.segment.evictEntry(node, RemovalCause.SIZE, this.segment.getCacheV().expirationTicker().read());
                }
            } else if (node.inMainProtected()) {
                setMainProtectedWeightedSize(mainProtectedWeightedSize() + weightDifference);
                if (node.getPolicyWeight() <= maximum) {
                    onAccess(node);
                } else {
                    this.segment.evictEntry(node, RemovalCause.SIZE, expirationTicker().read());
                }
            }

            setWeightedSize(this.segment.getCacheV().weightedSize() + weightDifference);
            if (this.segment.getCacheV().weightedSize() > MAXIMUM_CAPACITY) {
                this.segment.evictEntries();
            }
        } else if (expiresAfterAccess()) {
            onAccess(node);
        }
    }
}
