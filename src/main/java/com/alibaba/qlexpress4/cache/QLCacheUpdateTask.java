package com.alibaba.qlexpress4.cache;

import com.google.errorprone.annotations.concurrent.GuardedBy;

/**
 * @Author TaoKan
 * @Date 2024/2/18 9:15 PM
 */
public class QLCacheUpdateTask<K,V> implements Runnable {
    final QLCacheNode<K, V> node;
    final int weight;

    QLCacheUpdateTask(QLCacheNode<K, V> node, int weight) {
        this.weight = weight;
        this.node = node;
    }

    @Override
    @GuardedBy("evictionLock")
    @SuppressWarnings("FutureReturnValueIgnored")
    public void run() {

    }
}
