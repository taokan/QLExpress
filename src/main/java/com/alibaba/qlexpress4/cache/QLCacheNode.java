package com.alibaba.qlexpress4.cache;

import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Date;

/**
 * @Author TaoKan
 * @Date 2023/11/12 5:56 PM
 */
public class QLCacheNode<K,V> implements QLAccessOrderDeque.QLAccessOrder<QLCacheNode<K,V>>,QLWriteOrderDeque.QLWriteOrder<QLCacheNode<K,V>> {
    private long time;
    private int weight;
    volatile K key;
    volatile V value;

    public static final int WINDOW = 0;
    public static final int PROBATION = 1;
    public static final int PROTECTED = 2;


    public QLCacheNode(K key, V value){
        this.key = key;
        this.value = value;
        this.time = new Date().getTime();
        this.weight = 1;
    }

    public QLCacheNode(K key, V value, int weight){
        this.key = key;
        this.value = value;
        this.time = new Date().getTime();
        this.weight = weight;
    }

    public QLCacheNode() {

    }

    public K getKey() {return key;}

    public V getValue() {
        return value;
    }

    public long getTime() {
        return time;
    }

    public int getWeight() {
        return weight;
    }


    public void addWeight(int weight) {
        this.weight = weight + this.weight;
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


    /** Returns the variable expiration time, in nanoseconds. */
    public long getVariableTime() {
        return 0L;
    }

    /**
     * Sets the variable expiration time in nanoseconds. This update may be set lazily and rely on the
     * memory fence when the lock is released.
     */
    public void setVariableTime(long time) {}

    /**
     * Atomically sets the variable time to the given updated value if the current value equals the
     * expected value and returns if the update was successful.
     */
    public boolean casVariableTime(long expect, long update) {
        throw new UnsupportedOperationException();
    }

    // @GuardedBy("evictionLock")
    public QLCacheNode<K, V> getPreviousInVariableOrder() {
        throw new UnsupportedOperationException();
    }

    // @GuardedBy("evictionLock")
    public void setPreviousInVariableOrder(@Nullable QLCacheNode<K, V> prev) {
        throw new UnsupportedOperationException();
    }

    // @GuardedBy("evictionLock")
    public QLCacheNode<K, V> getNextInVariableOrder() {
        throw new UnsupportedOperationException();
    }

    // @GuardedBy("evictionLock")
    public void setNextInVariableOrder(@Nullable QLCacheNode<K, V> prev) {
        throw new UnsupportedOperationException();
    }


    public void setTime(long time) {
        this.time = time;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public boolean inMainProtected() {
        return getQueueType() == PROTECTED;
    }

    /** Sets the status to the Window queue. */
    public void makeWindow() {
        setQueueType(WINDOW);
    }

    /** Sets the status to the Main space's probation queue. */
    public void makeMainProbation() {
        setQueueType(PROBATION);
    }

    /** Sets the status to the Main space's protected queue. */
    public void makeMainProtected() {
        setQueueType(PROTECTED);
    }

    /** Returns the queue that the entry's resides in (window, probation, or protected). */
    public int getQueueType() {
        return WINDOW;
    }

    /** Set queue that the entry resides in (window, probation, or protected). */
    public void setQueueType(int queueType) {
        throw new UnsupportedOperationException();
    }
}
