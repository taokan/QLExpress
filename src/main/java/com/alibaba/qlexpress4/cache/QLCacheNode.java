package com.alibaba.qlexpress4.cache;

import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Date;

/**
 * @Author TaoKan
 * @Date 2023/11/12 5:56 PM
 */
public class QLCacheNode<K,V> implements QLAccessOrderDeque.QLAccessOrder<QLCacheNode<K,V>>,QLWriteOrderDeque.QLWriteOrder<QLCacheNode<K,V>> {
    private long time;
    volatile K key;
    volatile V value;

    public QLCacheNode(K key, V value){
        this.key = key;
        this.value = value;
        this.time = new Date().getTime();
    }

    public K getKey() {return key;}

    public V getValue() {
        return value;
    }

    public long getTime() {
        return time;
    }



    @Override
    public QLCacheNode<K,V> getPreviousInAccessOrder() {
        return null;
    }

    @Override
    public void setPreviousInAccessOrder(QLCacheNode<K,V> prev) {

    }

    @Override
    public QLCacheNode<K,V> getNextInAccessOrder() {
        return null;
    }

    @Override
    public void setNextInAccessOrder(QLCacheNode<K,V> next) {

    }

    @Override
    public @Nullable QLCacheNode<K,V> getPreviousInWriteOrder() {
        return null;
    }

    @Override
    public void setPreviousInWriteOrder(@Nullable QLCacheNode<K,V> var1) {

    }

    @Override
    public @Nullable QLCacheNode<K,V> getNextInWriteOrder() {
        return null;
    }

    @Override
    public void setNextInWriteOrder(@Nullable QLCacheNode<K,V> var1) {

    }
}
