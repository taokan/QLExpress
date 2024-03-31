package com.alibaba.qlexpress4.cache;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import org.checkerframework.checker.nullness.qual.Nullable;
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
        this.cacheV.getWriteBuffer().offer(new QLCacheUpdateTask(cacheNodeNew, cacheNodeOld , 1, this, this.cacheV.frequencySketch()));
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

    @GuardedBy("evictionLock")
    @Nullable QLCacheNode<K, V> evictFromWindow() {
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

    @GuardedBy("evictionLock")
    void evictFromMain(@Nullable QLCacheNode<K, V> candidate) {
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

    @GuardedBy("evictionLock")
    @SuppressWarnings({"GuardedByChecker", "NullAway", "PMD.CollapsibleIfStatements"})
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
                        expired |= ((now - n.getAccessTime()) >= expiresAfterAccessNanos());
                    }
                    if (this.getCacheV().expiresAfterWrite()) {
                        expired |= ((now - n.getWriteTime()) >= expiresAfterWriteNanos());
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

                notifyEviction(key, value[0], actualCause[0]);
                discardRefresh(key);
                removed[0] = true;
                node.retire();
            }
            return null;
        });

        // The entry is no longer eligible for eviction
        if (resurrect[0]) {
            return false;
        }

        // If the eviction fails due to a concurrent removal of the victim, that removal may cancel out
        // the addition that triggered this eviction. The victim is eagerly unlinked and the size
        // decremented before the removal task so that if an eviction is still required then a new
        // victim will be chosen for removal.
        if (node.inWindow() && (evicts() || expiresAfterAccess())) {
            this.getCacheV().accessOrderWindowDeque().remove(node);
        } else if (evicts()) {
            if (node.inMainProbation()) {
                this.getCacheV().accessOrderProbationDeque().remove(node);
            } else {
                this.getCacheV().accessOrderProtectedDeque().remove(node);
            }
        }
        if (expiresAfterWrite()) {
            this.getCacheV().writeOrderDeque().remove(node);
        } else if (expiresVariable()) {
            this.getCacheV().timerWheel().deschedule(node);
        }

        synchronized (node) {
            logIfAlive(node);
            makeDead(node);
        }

        if (removed[0]) {
            statsCounter().recordEviction(node.getWeight(), actualCause[0]);
            notifyRemoval(key, value[0], actualCause[0]);
        }

        return true;
    }

    public void expireVariableEntries(long now) {
        if (this.getCacheV().expiresVariable()) {
            this.getCacheV().timerWheel().advance(this, now);
        }
    }

    void expireEntries() {
        long now = this.getCacheV().expirationTicker().read();
        expireAfterAccessEntries(now);
        expireAfterWriteEntries(now);
        expireVariableEntries(now);

        Pacer pacer = pacer();
        if (pacer != null) {
            long delay = getExpirationDelay(now);
            if (delay == Long.MAX_VALUE) {
                pacer.cancel();
            } else {
                pacer.schedule(executor, drainBuffersTask, now, delay);
            }
        }
    }

}
