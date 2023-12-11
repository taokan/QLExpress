package com.alibaba.qlexpress4.cache;

/**
 * @Author TaoKan
 * @Date 2023/12/10 3:46 PM
 */
public class QLExpressCacheV2<K,V> {
    private final boolean useCache;

    private ICache<K,V> cache;
    public QLExpressCacheV2(int maxSize, boolean useCache){
        this.useCache = useCache;
        if (!this.useCache) {
            this.cache = new QLUnCacheMap();
        }else {
            this.cache = new QLCacheMap(maxSize);
        }
    }
    public void put(K key, V value){
        this.cache.put(key, value);
    }
    public V get(K key){
        return this.cache.get(key);
    }

    public boolean isUseCache() {
        return useCache;
    }

}
