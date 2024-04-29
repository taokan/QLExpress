package com.alibaba.qlexpress4.cache;

import java.util.concurrent.ConcurrentHashMap;

import static com.alibaba.qlexpress4.cache.QLCacheNode.*;


/**
 * @Author TaoKan
 * @Date 2023/12/10 3:14 PM
 */
public class QLSegment<K,V> {
    private ConcurrentHashMap<K, QLCacheNode<K,V>> table;
    private long maxSegmentWeight;
    private QLCacheMap<K,V> cacheV;
    private QLCacheWindow window;
    private QLCacheMainProtected mainProtected;
    private QLCacheStatistic statistic;

    private long weight;

    public QLSegment(QLCacheMap<K,V> cacheV, int initialCapacity, long maxSegmentWeight){
        this.table = new ConcurrentHashMap(initialCapacity);
        this.maxSegmentWeight = maxSegmentWeight;
        this.cacheV = cacheV;
        this.weight = 0;
        this.window = new QLCacheWindow();
        this.mainProtected = new QLCacheMainProtected();
        this.statistic = new QLCacheStatistic();
    }

    public void put(K key, int hash, V value){
        QLCacheNode<K,V> cacheNode = new QLCacheNode(key, value);
        QLCacheNode<K,V> cacheNodeOld = this.table.putIfAbsent(key, cacheNode);
        if (cacheNodeOld == null){
            onAccessAddWriteTask(cacheNode);
        }else {
            cacheNode.addWeight(cacheNodeOld.getWeight());
            this.table.put(key, cacheNode);
            onAccessUpdateWriteTask(cacheNode, cacheNodeOld);
        }
    }

    public V get(K key, int hash){
        QLCacheNode<K,V> cacheNode = this.table.get(key);
        if (cacheNode != null) {
            onAccessReadTask(cacheNode);
            return cacheNode.getValue();
        }
        return null;
    }


    private void onAccessAddWriteTask(QLCacheNode<K,V> cacheNode) {
        this.cacheV.getWriteBuffer().offer(new QLCacheAddTask(cacheNode, 1, this, this.cacheV.frequencySketch()));
    }
    private void onAccessUpdateWriteTask(QLCacheNode<K,V> cacheNodeNew, QLCacheNode<K,V> cacheNodeOld) {
        //TODO weightDiff
        this.cacheV.getWriteBuffer().offer(new QLCacheUpdateTask(cacheNodeNew, cacheNodeOld , 1, this, this.cacheV.frequencySketch(),0));
    }

    private void onAccessDeleteWriteTask(QLCacheNode<K,V> cacheNode){

    }

    private void onAccessReadTask(QLCacheNode<K,V> cacheNode) {

    }


    public ConcurrentHashMap<K, QLCacheNode<K, V>> getTable() {
        return table;
    }

    public void setTable(ConcurrentHashMap<K, QLCacheNode<K, V>> table) {
        this.table = table;
    }

    public long getMaxSegmentWeight() {
        return maxSegmentWeight;
    }

    public void setMaxSegmentWeight(long maxSegmentWeight) {
        this.maxSegmentWeight = maxSegmentWeight;
    }

    public QLCacheMap<K, V> getCacheV() {
        return cacheV;
    }

    public void setCacheV(QLCacheMap<K, V> cacheV) {
        this.cacheV = cacheV;
    }

    public QLCacheWindow getWindow() {
        return window;
    }

    public void setWindow(QLCacheWindow window) {
        this.window = window;
    }

    public QLCacheMainProtected getMainProtected() {
        return mainProtected;
    }

    public void setMainProtected(QLCacheMainProtected mainProtected) {
        this.mainProtected = mainProtected;
    }

    public long getWeight() {
        return weight;
    }

    public void setWeight(long weight) {
        this.weight = weight;
    }


    public QLCacheStatistic getStatistic() {
        return statistic;
    }

    public void setStatistic(QLCacheStatistic statistic) {
        this.statistic = statistic;
    }

    void evictEntries() {
        evictFromMain(evictFromWindow());
    }

    QLCacheNode<K, V> evictFromWindow() {
        QLCacheNode<K, V> first = null;
        QLCacheNode<K, V> node = this.getCacheV().accessOrderWindowDeque().peekFirst();
        while (this.window.getWeight() > this.window.getMaxNum()) {
            // The pending operations will adjust the size to reflect the correct weight
            if (node == null) {
                break;
            }

            QLCacheNode<K, V> next = node.getNextInAccessOrder();
            if (node.getWeight() != 0) {
                node.makeMainProbation();
                this.getCacheV().accessOrderWindowDeque().remove(node);
                this.getCacheV().accessOrderProbationDeque().offerLast(node);
                if (first == null) {
                    first = node;
                }
                this.window.setWeight(this.window.getWeight() - node.getWeight());
            }
            node = next;
        }

        return first;
    }

    void evictFromMain(QLCacheNode<K, V> candidate) {
        int victimQueue = PROBATION;
        int candidateQueue = PROBATION;
        QLCacheNode<K, V> victim = this.getCacheV().accessOrderProbationDeque().peekFirst();
        while (this.weight > this.maxSegmentWeight) {
            // Search the admission window for additional candidates
            if ((candidate == null) && (candidateQueue == PROBATION)) {
                candidate = this.getCacheV().accessOrderWindowDeque().peekFirst();
                candidateQueue = WINDOW;
            }

            // Try evicting from the protected and window queues
            if ((candidate == null) && (victim == null)) {
                if (victimQueue == PROBATION) {
                    victim = this.getCacheV().accessOrderProtectedDeque().peekFirst();
                    victimQueue = PROTECTED;
                    continue;
                } else if (victimQueue == PROTECTED) {
                    victim = this.getCacheV().accessOrderWindowDeque().peekFirst();
                    victimQueue = WINDOW;
                    continue;
                }

                // The pending operations will adjust the size to reflect the correct weight
                break;
            }

            // Skip over entries with zero weight
            if ((victim != null) && (victim.getWeight() == 0)) {
                victim = victim.getNextInAccessOrder();
                continue;
            } else if ((candidate != null) && (candidate.getWeight() == 0)) {
                candidate = candidate.getNextInAccessOrder();
                continue;
            }

            // Evict immediately if only one of the entries is present
            if (victim == null) {
                @SuppressWarnings("NullAway")
                QLCacheNode<K, V> previous = candidate.getNextInAccessOrder();
                QLCacheNode<K, V> evict = candidate;
                candidate = previous;
                evictEntry(evict, RemovalCause.SIZE, 0L);
                continue;
            } else if (candidate == null) {
                QLCacheNode<K, V> evict = victim;
                victim = victim.getNextInAccessOrder();
                evictEntry(evict, RemovalCause.SIZE, 0L);
                continue;
            }

            // Evict immediately if both selected the same entry
            if (candidate == victim) {
                victim = victim.getNextInAccessOrder();
                evictEntry(candidate, RemovalCause.SIZE, 0L);
                candidate = null;
                continue;
            }

            // Evict immediately if an entry was collected
            K victimKey = victim.getKey();
            K candidateKey = candidate.getKey();
            if (victimKey == null) {
                QLCacheNode<K, V> evict = victim;
                victim = victim.getNextInAccessOrder();
                evictEntry(evict, RemovalCause.COLLECTED, 0L);
                continue;
            } else if (candidateKey == null) {
                QLCacheNode<K, V> evict = candidate;
                candidate = candidate.getNextInAccessOrder();
                evictEntry(evict, RemovalCause.COLLECTED, 0L);
                continue;
            }
            // Evict immediately if the candidate's weight exceeds the maximum
            if (candidate.getWeight() > maxSegmentWeight) {
                QLCacheNode<K, V> evict = candidate;
                candidate = candidate.getNextInAccessOrder();
                evictEntry(evict, RemovalCause.SIZE, 0L);
                continue;
            }

            // Evict the entry with the lowest frequency
            if (this.getCacheV().admit(candidateKey, victimKey)) {
                QLCacheNode<K, V> evict = victim;
                victim = victim.getNextInAccessOrder();
                evictEntry(evict, RemovalCause.SIZE, 0L);
                candidate = candidate.getNextInAccessOrder();
            } else {
                QLCacheNode<K, V> evict = candidate;
                candidate = candidate.getNextInAccessOrder();
                evictEntry(evict, RemovalCause.SIZE, 0L);
            }
        }
    }

    boolean evictEntry(QLCacheNode<K, V> node, RemovalCause cause, long now) {
        K key = node.getKey();
        @SuppressWarnings("unchecked")
        V[] value = (V[]) new Object[1];
        boolean[] removed = new boolean[1];
        boolean[] resurrect = new boolean[1];
        RemovalCause[] actualCause = new RemovalCause[1];

        this.table.computeIfPresent(key, (k, n) -> {
            if (n != node) {
                return n;
            }
            synchronized (n) {
                value[0] = n.getValue();

                if ((key == null) || (value[0] == null)) {
                    actualCause[0] = RemovalCause.COLLECTED;
                } else if (cause == RemovalCause.COLLECTED) {
                    resurrect[0] = true;
                    return n;
                } else {
                    actualCause[0] = cause;
                }

                if (actualCause[0] == RemovalCause.EXPIRED) {
                    boolean expired = false;
                    if (this.getCacheV().expiresAfterAccess()) {
                        expired |= ((now - n.getAccessTime()) >= this.getCacheV().expiresAfterAccessNanos());
                    }
                    if (this.getCacheV().expiresAfterWrite()) {
                        expired |= ((now - n.getWriteTime()) >= this.getCacheV().expiresAfterWriteNanos());
                    }
                    if (this.getCacheV().expiresVariable()) {
                        expired |= (n.getVariableTime() <= now);
                    }
                    if (!expired) {
                        resurrect[0] = true;
                        return n;
                    }
                } else if (actualCause[0] == RemovalCause.SIZE) {
                    int weight = node.getWeight();
                    if (weight == 0) {
                        resurrect[0] = true;
                        return n;
                    }
                }
                removed[0] = true;
            }
            return null;
        });

        if (resurrect[0]) {
            return false;
        }

        if (node.inWindow() && (evicts() || this.getCacheV().expiresAfterAccess())) {
            this.getCacheV().accessOrderWindowDeque().remove(node);
        } else if (evicts()) {
            if (node.inMainProbation()) {
                this.getCacheV().accessOrderProbationDeque().remove(node);
            } else {
                this.getCacheV().accessOrderProtectedDeque().remove(node);
            }
        }
        if (this.getCacheV().expiresAfterWrite()) {
            this.getCacheV().writeOrderDeque().remove(node);
        } else if (this.getCacheV().expiresVariable()) {
            this.getCacheV().timerWheel().deschedule(node);
        }

        synchronized (node) {
            makeDead(node);
        }

        if (removed[0]) {
            this.getCacheV().statsCounter().recordEviction(node.getWeight(), actualCause[0]);
        }

        return true;
    }

    void makeDead(QLCacheNode<K, V> node) {
        synchronized (node) {
            if (evicts()) {
                if (node.inWindow()) {
                    this.getCacheV().setWindowWeightedSize( this.getCacheV().windowWeightedSize() - node.getWeight());
                } else if (node.inMainProtected()) {
                    this.getCacheV().setMainProtectedWeightedSize( this.getCacheV().mainProtectedWeightedSize() - node.getWeight());
                }
                this.getCacheV().setWeightedSize(this.getCacheV().weightedSize() - node.getWeight());
            }
        }
    }

    void expireEntries() {
        long now = this.getCacheV().expirationTicker().read();
        expireAfterAccessEntries(now);
        expireAfterWriteEntries(now);
        expireVariableEntries(now);

        //不用pacer，暂时没有功能
//        QLPacer pacer = pacer();
//        if (pacer != null) {
//            long delay = getExpirationDelay(now);
//            if (delay == Long.MAX_VALUE) {
//                pacer.cancel();
//            } else {
//                pacer.schedule(executor, drainBuffersTask, now, delay);
//            }
//        }
    }

    void expireAfterAccessEntries(long now) {
        if (!this.getCacheV().expiresAfterAccess()) {
            return;
        }
        expireAfterAccessEntries(this.getCacheV().accessOrderWindowDeque(), now);
        if (evicts()) {
            expireAfterAccessEntries(this.getCacheV().accessOrderProbationDeque(), now);
            expireAfterAccessEntries(this.getCacheV().accessOrderProtectedDeque(), now);
        }
    }

    /** Expires entries in an access-order queue. */
    void expireAfterAccessEntries(QLAccessOrderDeque<QLCacheNode<K, V>> accessOrderDeque, long now) {
        long duration = this.getCacheV().expiresAfterAccessNanos();
        for (;;) {
            QLCacheNode<K, V> node = accessOrderDeque.peekFirst();
            if ((node == null) || ((now - node.getAccessTime()) < duration)
                    || !evictEntry(node, RemovalCause.EXPIRED, now)) {
                return;
            }
        }
    }

    /** Expires entries on the write-order queue. */
    void expireAfterWriteEntries(long now) {
        if (!this.getCacheV().expiresAfterWrite()) {
            return;
        }
        long duration = this.getCacheV().expiresAfterWriteNanos();
        for (;;) {
            QLCacheNode<K, V> node = this.getCacheV().writeOrderDeque().peekFirst();
            if ((node == null) || ((now - node.getWriteTime()) < duration)
                    || !evictEntry(node, RemovalCause.EXPIRED, now)) {
                break;
            }
        }
    }

    /** Expires entries in the timer wheel. */
    void expireVariableEntries(long now) {
        if (this.getCacheV().expiresVariable()) {
            this.getCacheV().timerWheel().advance(this, now);
        }
    }


    public boolean evicts() {
        return false;
    }

    void onAccess(QLCacheNode<K, V> node) {
        if (evicts()) {
            K key = node.getKey();
            if (key == null) {
                return;
            }
            this.getCacheV().frequencySketch().increment(key);
            if (node.inWindow()) {
                reorder(this.getCacheV().accessOrderWindowDeque(), node);
            } else if (node.inMainProbation()) {
                reorderProbation(node);
            } else {
                reorder(this.getCacheV().accessOrderProtectedDeque(), node);
            }
            this.getCacheV().setHitsInSample(this.getCacheV().hitsInSample() + 1);
        } else if (this.getCacheV().expiresAfterAccess()) {
            reorder(this.getCacheV().accessOrderWindowDeque(), node);
        }
        if (this.getCacheV().expiresVariable()) {
            this.getCacheV().timerWheel().reschedule(node);
        }
    }

    void reorderProbation(QLCacheNode<K, V> node) {
        if (!this.getCacheV().accessOrderProbationDeque().contains(node)) {
            return;
        } else if (node.getPolicyWeight() > this.getCacheV().mainProtectedMaximum()) {
            reorder(this.getCacheV().accessOrderProbationDeque(), node);
            return;
        }

        this.getCacheV().setMainProtectedWeightedSize(this.getCacheV().mainProtectedWeightedSize() + node.getPolicyWeight());
        this.getCacheV().accessOrderProbationDeque().remove(node);
        this.getCacheV().accessOrderProtectedDeque().offerLast(node);
        node.makeMainProtected();
    }

    static <K, V> void reorder(QLLinkedDeque<QLCacheNode<K, V>> deque, QLCacheNode<K, V> node) {
        if (deque.contains(node)) {
            deque.moveToBack(node);
        }
    }

}
