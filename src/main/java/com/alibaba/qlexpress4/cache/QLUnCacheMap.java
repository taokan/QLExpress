package com.alibaba.qlexpress4.cache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author TaoKan
 * @Date 2023/12/10 5:10 PM
 */
public class QLUnCacheMap<K,V> implements ICache<K,V>{

    private ConcurrentHashMap<K,V> concurrentHashMap;

    public QLUnCacheMap(){
        this.concurrentHashMap = new ConcurrentHashMap<>();
    }
    @Override
    public void put(K key, V value) {
        concurrentHashMap.put(key,value);
    }

    @Override
    public V get(K key) {
        return concurrentHashMap.get(key);
    }
}
