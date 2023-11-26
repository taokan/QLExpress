package com.alibaba.qlexpress4.cache;

import java.util.Date;

/**
 * @Author TaoKan
 * @Date 2023/11/12 5:56 PM
 */
public class QLCacheNode<V> implements QLAccessOrderDeque.AccessOrder<QLCacheNode<V>> {

    private V value;
    private long time;

    public QLCacheNode(V value){
        this.value = value;
        this.time = new Date().getTime();
    }

    public V getValue() {
        return value;
    }

    public long getTime() {
        return time;
    }

    @Override
    public QLCacheNode<V> getPreviousInAccessOrder() {
        return null;
    }

    @Override
    public void setPreviousInAccessOrder(QLCacheNode<V> prev) {

    }

    @Override
    public QLCacheNode<V> getNextInAccessOrder() {
        return null;
    }

    @Override
    public void setNextInAccessOrder(QLCacheNode<V> next) {

    }
}
