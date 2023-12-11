package com.alibaba.qlexpress4.cache;

/**
 * @Author TaoKan
 * @Date 2023/12/10 3:14 PM
 */
public class QLSegment<K,V> {
    private QLFrequency<K> qlFrequency;

    public QLSegment(QLCacheMap<K,V> cacheV, int initialCapacity, long maxSegmentWeight, QLFrequency<K> qlFrequency){
        this.qlFrequency = qlFrequency;
    }


    public void put(K key, int hash, V value){
        QLCacheNode<K,V> cacheNode = new QLCacheNode(key, value);
//        putElementFromSegmentCache();
        onAccess(cacheNode);
    }

    public V get(K key, int hash){
//        QLCacheNode<K,V> cacheNode = getElementFromSegmentCache(key);
//        if(cacheNode == null) {
//            return null;
//        }else {
//            onAccess(cacheNode);
//            return cacheNode.getValue();
//        }
        return null;
    }

    void onAccess(QLCacheNode<K, V> node) {

    }
}
