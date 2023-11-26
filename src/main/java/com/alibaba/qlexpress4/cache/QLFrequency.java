package com.alibaba.qlexpress4.cache;

/**
 * @Author TaoKan
 * @Date 2023/11/12 6:01 PM
 */
public class QLFrequency<E> {

    static final long RESET_MASK = 0x7777777777777777L;
    static final long ONE_MASK = 0x1111111111111111L;
    int sampleSize;
    int blockMask;
    long[] table;
    int size;

    public void ensureCapacity(long maximumSize) {
        int maximum = (int) Math.min(maximumSize, Integer.MAX_VALUE >>> 1);
        if ((table != null) && (table.length >= maximum)) {
            return;
        }

        table = new long[Math.max(ceilingPowerOfTwo(maximum), 8)];
        sampleSize = (maximumSize == 0) ? 10 : (10 * maximum);
        blockMask = (table.length >>> 3) - 1;
        if (sampleSize <= 0) {
            sampleSize = Integer.MAX_VALUE;
        }
        size = 0;
    }

    /**
     * Returns if the sketch has not yet been initialized, requiring that {@link #ensureCapacity} is
     * called before it begins to track frequencies.
     */
    public boolean isNotInitialized() {
        return (table == null);
    }

    /**
     * Returns the estimated number of occurrences of an element, up to the maximum (15).
     *
     * @param e the element to count occurrences of
     * @return the estimated number of occurrences of the element; possibly zero but never negative
     */
    public int frequency(E e) {
        if (isNotInitialized()) {
            return 0;
        }

        int[] count = new int[4];
        int blockHash = spread(e.hashCode());
        int counterHash = rehash(blockHash);
        int block = (blockHash & blockMask) << 3;
        for (int i = 0; i < 4; i++) {
            int h = counterHash >>> (i << 3);
            int index = (h >>> 1) & 15;
            int offset = h & 1;
            count[i] = (int) ((table[block + offset + (i << 1)] >>> (index << 2)) & 0xfL);
        }
        return Math.min(Math.min(count[0], count[1]), Math.min(count[2], count[3]));
    }

    /**
     * Increments the popularity of the element if it does not exceed the maximum (15). The popularity
     * of all elements will be periodically down sampled when the observed events exceed a threshold.
     * This process provides a frequency aging to allow expired long term entries to fade away.
     *
     * @param e the element to add
     */
    public void increment(E e) {
        if (isNotInitialized()) {
            return;
        }

        int[] index = new int[8];
        int blockHash = spread(e.hashCode());
        int counterHash = rehash(blockHash);
        int block = (blockHash & blockMask) << 3;
        for (int i = 0; i < 4; i++) {
            int h = counterHash >>> (i << 3);
            index[i] = (h >>> 1) & 15;
            int offset = h & 1;
            index[i + 4] = block + offset + (i << 1);
        }
        boolean added =
                incrementAt(index[4], index[0])
                        | incrementAt(index[5], index[1])
                        | incrementAt(index[6], index[2])
                        | incrementAt(index[7], index[3]);

        if (added && (++size == sampleSize)) {
            reset();
        }
    }

    /** Applies a supplemental hash functions to defends against poor quality hash. */
    static int spread(int x) {
        x ^= x >>> 17;
        x *= 0xed5ad4bb;
        x ^= x >>> 11;
        x *= 0xac4c1b51;
        x ^= x >>> 15;
        return x;
    }

    /** Applies another round of hashing for additional randomization. */
    static int rehash(int x) {
        x *= 0x31848bab;
        x ^= x >>> 14;
        return x;
    }

    /**
     * Increments the specified counter by 1 if it is not already at the maximum value (15).
     *
     * @param i the table index (16 counters)
     * @param j the counter to increment
     * @return if incremented
     */
    boolean incrementAt(int i, int j) {
        int offset = j << 2;
        long mask = (0xfL << offset);
        if ((table[i] & mask) != mask) {
            table[i] += (1L << offset);
            return true;
        }
        return false;
    }

    /** Reduces every counter by half of its original value. */
    void reset() {
        int count = 0;
        for (int i = 0; i < table.length; i++) {
            count += Long.bitCount(table[i] & ONE_MASK);
            table[i] = (table[i] >>> 1) & RESET_MASK;
        }
        size = (size - (count >>> 2)) >>> 1;
    }

    int ceilingPowerOfTwo(int x) {
        return 1 << -Integer.numberOfLeadingZeros(x - 1);
    }

    long ceilingPowerOfTwo(long x) {
        return 1L << -Long.numberOfLeadingZeros(x - 1);
    }

}
