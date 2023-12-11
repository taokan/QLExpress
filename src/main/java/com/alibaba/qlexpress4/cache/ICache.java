package com.alibaba.qlexpress4.cache;

/**
 * @Author TaoKan
 * @Date 2023/12/10 5:10 PM
 */
public interface ICache<K,V> {
    void put(K key, V value);

    V get(K key);
}
