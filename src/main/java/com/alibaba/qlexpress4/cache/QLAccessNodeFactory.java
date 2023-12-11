package com.alibaba.qlexpress4.cache;


import java.lang.ref.ReferenceQueue;

/**
 * @Author TaoKan
 * @Date 2023/12/10 2:23 PM
 */
//public class QLAccessNodeFactory<K,V> extends QLCacheNode<V>{
//    int queueType;
//
//
//    QLAccessNodeFactory(K key, ReferenceQueue<K> keyReferenceQueue, V value, ReferenceQueue<V> valueReferenceQueue, int weight, long now) {
//        super(key, keyReferenceQueue, value, valueReferenceQueue, weight, now);
//    }
//
//    QLAccessNodeFactory(Object keyReference, V value, ReferenceQueue<V> valueReferenceQueue, int weight, long now) {
//        super(keyReference, value, valueReferenceQueue, weight, now);
//    }
//
//    public int getQueueType() {
//        return this.queueType;
//    }
//
//    public void setQueueType(int queueType) {
//        this.queueType = queueType;
//    }
//
//    public QLCacheNode<V> newNode(K key, ReferenceQueue<K> keyReferenceQueue, V value, ReferenceQueue<V> valueReferenceQueue, int weight, long now) {
//        return new PSAMS(key, keyReferenceQueue, value, valueReferenceQueue, weight, now);
//    }
//
//    public QLCacheNode<V> newNode(Object keyReference, V value, ReferenceQueue<V> valueReferenceQueue, int weight, long now) {
//        return new PSAMS(keyReference, value, valueReferenceQueue, weight, now);
//    }
//}
