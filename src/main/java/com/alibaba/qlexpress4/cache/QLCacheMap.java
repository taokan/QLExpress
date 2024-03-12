package com.alibaba.qlexpress4.cache;

import java.util.concurrent.TimeUnit;


/**
 * @Author TaoKan
 * @Date 2023/12/10 5:13 PM
 */
public class QLCacheMap<K,V> implements ICache<K,V> {

    /** The number of CPUs */
    static final int NCPU = Runtime.getRuntime().availableProcessors();
    /** The initial capacity of the write buffer. */
    static final int WRITE_BUFFER_MIN = 4;
    /** The maximum capacity of the write buffer. */
    static final int WRITE_BUFFER_MAX = 128 * ceilingPowerOfTwo(NCPU);
    /** The number of attempts to insert into the write buffer before yielding. */
    static final int WRITE_BUFFER_RETRIES = 100;
    /** The maximum weighted capacity of the map. */
    static final long MAXIMUM_CAPACITY = Long.MAX_VALUE - Integer.MAX_VALUE;
    /** The initial percent of the maximum weighted capacity dedicated to the main space. */
    static final double PERCENT_MAIN = 0.99d;
    /** The percent of the maximum weighted capacity dedicated to the main's protected space. */
    static final double PERCENT_MAIN_PROTECTED = 0.80d;
    /** The difference in hit rates that restarts the climber. */
    static final double HILL_CLIMBER_RESTART_THRESHOLD = 0.05d;
    /** The percent of the total size to adapt the window by. */
    static final double HILL_CLIMBER_STEP_PERCENT = 0.0625d;
    /** The rate to decrease the step size to adapt by. */
    static final double HILL_CLIMBER_STEP_DECAY_RATE = 0.98d;
    /** The minimum popularity for allowing randomized admission. */
    static final int ADMIT_HASHDOS_THRESHOLD = 6;
    /** The maximum number of entries that can be transferred between queues. */
    static final int QUEUE_TRANSFER_THRESHOLD = 1_000;
    /** The maximum time window between entry updates before the expiration must be reordered. */
    static final long EXPIRE_WRITE_TOLERANCE = TimeUnit.SECONDS.toNanos(1);
    /** The maximum duration before an entry expires. */
    static final long MAXIMUM_EXPIRY = (Long.MAX_VALUE >> 1); // 150 years
    /** The duration to wait on the eviction lock before warning of a possible misuse. */
    static final long WARN_AFTER_LOCK_WAIT_NANOS = TimeUnit.SECONDS.toNanos(30);
    /** The number of retries before computing to validate the entry's integrity; pow2 modulus. */
    static final int MAX_PUT_SPIN_WAIT_ATTEMPTS = 1024 - 1;
    /** The handle for the in-flight refresh operations. */
    final int segmentMask;
    final int segmentShift;
    final long maxSize;
    final QLCacheClearPolicy qlCacheClearPolicy;
    private long maxWeight;
    private int concurrencyLevel = -1;
    private int initialCapacity = -1;
    private QLSegment<K, V>[] segments;
    private QLFrequency<K> qlFrequency;
    private QLAccessOrderDeque accessOrderWindowDeque;
    private QLAccessOrderDeque accessOrderProbationDeque;
    private QLAccessOrderDeque accessOrderProtectedDeque;
    private MpscGrowableArrayQueue writeBuffer;

    long maximum;
    long weightedSize;
    long windowMaximum;
    long windowWeightedSize;
    long mainProtectedMaximum;
    long mainProtectedWeightedSize;
    double stepSize;
    long adjustment;
    int hitsInSample;
    int missesInSample;
    double previousSampleHitRate;


    public QLCacheMap(long maxSize){
        this.maxSize = maxSize;
        this.qlCacheClearPolicy = QLCacheClearPolicy.ACCESS_CLEAR;
        this.concurrencyLevel = Math.min(getConcurrencyLevel(), 65536);
        long initialCapacity = Math.min(getInitialCapacity(), 1073741824);
        this.maxWeight = maxSize;
        if (evictsBySize()) {
            initialCapacity = Math.min(initialCapacity, this.maxWeight);
        }
        int segmentShift = 0;
        int segmentCount;
        for(segmentCount = 1; segmentCount < this.concurrencyLevel && (!this.evictsBySize() || (long)(segmentCount * 20) <= this.maxWeight); segmentCount <<= 1) {
            ++segmentShift;
        }
        this.segmentShift = 32 - segmentShift;
        this.segmentMask = segmentCount - 1;
        this.segments = this.newSegmentArray(segmentCount);
        long segmentCapacity = initialCapacity / segmentCount;
        if (segmentCapacity * segmentCount < initialCapacity) {
            ++segmentCapacity;
        }
        int segmentSize;
        for(segmentSize = 1; segmentSize < segmentCapacity; segmentSize <<= 1) {
        }
        if (this.evictsBySize()) {
            long maxSegmentWeight = this.maxWeight / (long)segmentCount + 1L;
            long remainder = this.maxWeight % (long)segmentCount;
            for(int i = 0; i < this.segments.length; ++i) {
                if ((long)i == remainder) {
                    --maxSegmentWeight;
                }
                this.segments[i] = this.createSegment(segmentSize, maxSegmentWeight);
            }
        } else {
            for(int i = 0; i < this.segments.length; ++i) {
                this.segments[i] = this.createSegment(segmentSize, -1L);
            }
        }
        setMaximumSize(maxWeight);
        long capacity = Math.min(maxSize, getInitialCapacity());
        this.qlFrequency = new QLFrequency<>();
        this.qlFrequency.ensureCapacity(capacity);
        this.accessOrderWindowDeque = new QLAccessOrderDeque();
        this.accessOrderProbationDeque = new QLAccessOrderDeque();
        this.accessOrderProtectedDeque = new QLAccessOrderDeque();
        writeBuffer = new MpscGrowableArrayQueue<>(WRITE_BUFFER_MIN, WRITE_BUFFER_MAX);
    }


    protected final void setMaximum(long maximum) {
        this.maximum = maximum;
    }

    protected final long weightedSize() {
        return this.weightedSize;
    }

    protected final void setWindowMaximum(long windowMaximum) {
        this.windowMaximum = windowMaximum;
    }


    protected final void setMainProtectedMaximum(long mainProtectedMaximum) {
        this.mainProtectedMaximum = mainProtectedMaximum;
    }


    protected final void setStepSize(double stepSize) {
        this.stepSize = stepSize;
    }


    protected final void setHitsInSample(int hitsInSample) {
        this.hitsInSample = hitsInSample;
    }

    protected final void setMissesInSample(int missesInSample) {
        this.missesInSample = missesInSample;
    }

    protected final QLFrequency<K> frequencySketch() {
        return this.qlFrequency;
    }

    protected boolean fastpath() {
        return true;
    }

    protected final QLAccessOrderDeque<QLCacheNode<K, V>> accessOrderWindowDeque() {
        return this.accessOrderWindowDeque;
    }

    protected final QLAccessOrderDeque<QLCacheNode<K, V>> accessOrderProbationDeque() {
        return this.accessOrderProbationDeque;
    }

    protected final QLAccessOrderDeque<QLCacheNode<K, V>> accessOrderProtectedDeque() {
        return this.accessOrderProtectedDeque;
    }

    void setMaximumSize(long maximum) {
        long max = Math.min(maximum, MAXIMUM_CAPACITY);
        long window = max - (long) (PERCENT_MAIN * max);
        long mainProtected = (long) (PERCENT_MAIN_PROTECTED * (max - window));

        setMaximum(max);
        setWindowMaximum(window);
        setMainProtectedMaximum(mainProtected);

        setHitsInSample(0);
        setMissesInSample(0);
        setStepSize(-HILL_CLIMBER_STEP_PERCENT * max);

        if ((this.qlFrequency != null) && (weightedSize() >= (max >>> 1))) {
            // Lazily initialize when close to the maximum size
            this.qlFrequency.ensureCapacity(max);
        }
    }

    @Override
    public void put(K key, V value) {
        int hash = rehash(key.hashCode());
        this.segmentFor(hash).put(key, hash, value);
    }

    QLSegment<K, V> segmentFor(int hash) {
        return this.segments[hash >>> this.segmentShift & this.segmentMask];
    }

    @Override
    public V get(K key) {
        int hash = rehash(key.hashCode());
        return this.segmentFor(hash).get(key, hash);
    }


    int getConcurrencyLevel() {
        return this.concurrencyLevel == -1 ? 4 : this.concurrencyLevel;
    }

    int getInitialCapacity() {
        return this.initialCapacity == -1 ? 16 : this.initialCapacity;
    }

    boolean evictsBySize() {
        return this.maxWeight >= 0L;
    }

    final QLSegment<K, V>[] newSegmentArray(int size) {
        return new QLSegment[size];
    }

    QLSegment<K, V> createSegment(int initialCapacity, long maxSegmentWeight) {
        return new QLSegment(this, initialCapacity, maxSegmentWeight);
    }

    static int rehash(int h) {
        h += h << 15 ^ -12931;
        h ^= h >>> 10;
        h += h << 3;
        h ^= h >>> 6;
        h += (h << 2) + (h << 14);
        return h ^ h >>> 16;
    }

    static int ceilingPowerOfTwo(int x) {
        // From Hacker's Delight, Chapter 3, Harry S. Warren Jr.
        return 1 << -Integer.numberOfLeadingZeros(x - 1);
    }

    /** Returns the smallest power of two greater than or equal to {@code x}. */
    static long ceilingPowerOfTwo(long x) {
        // From Hacker's Delight, Chapter 3, Harry S. Warren Jr.
        return 1L << -Long.numberOfLeadingZeros(x - 1);
    }

    public MpscGrowableArrayQueue getWriteBuffer() {
        return writeBuffer;
    }


}
