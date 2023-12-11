package com.alibaba.qlexpress4.cache;

import org.checkerframework.checker.index.qual.NonNegative;

/**
 * @Author TaoKan
 * @Date 2023/12/10 12:53 PM
 */
public interface QLWeighter<K, V> {

    /**
     * Returns the weight of a cache entry. There is no unit for entry weights; rather they are simply
     * relative to each other.
     *
     * @param key the key to weigh
     * @param value the value to weigh
     * @return the weight of the entry; must be non-negative
     */
    @NonNegative
    int weigh(K key, V value);

    /**
     * Returns a weigher where an entry has a weight of {@code 1}.
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @return a weigher where an entry has a weight of {@code 1}
     */
    static <K, V> QLWeighter<K, V> singletonWeigher() {
        @SuppressWarnings("unchecked")
        QLWeighter<K, V> instance = (QLWeighter<K, V>) SingletonWeigher.INSTANCE;
        return instance;
    }
}

enum SingletonWeigher implements QLWeighter<Object, Object> {
    INSTANCE;

    @Override public int weigh(Object key, Object value) {
        return 1;
    }
}

