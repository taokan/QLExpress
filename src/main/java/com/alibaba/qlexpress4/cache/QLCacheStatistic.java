package com.alibaba.qlexpress4.cache;

/**
 * @Author TaoKan
 * @Date 2024/2/18 10:03 PM
 */
public class QLCacheStatistic {
    private double stepSize;
    private long adjustment;
    private int hitsInSample;
    private int missesInSample;
    private double previousSampleHitRate;

    public double getStepSize() {
        return stepSize;
    }

    public void setStepSize(double stepSize) {
        this.stepSize = stepSize;
    }

    public long getAdjustment() {
        return adjustment;
    }

    public void setAdjustment(long adjustment) {
        this.adjustment = adjustment;
    }

    public int getHitsInSample() {
        return hitsInSample;
    }

    public void setHitsInSample(int hitsInSample) {
        this.hitsInSample = hitsInSample;
    }

    public int getMissesInSample() {
        return missesInSample;
    }

    public void setMissesInSample(int missesInSample) {
        this.missesInSample = missesInSample;
    }

    public double getPreviousSampleHitRate() {
        return previousSampleHitRate;
    }

    public void setPreviousSampleHitRate(double previousSampleHitRate) {
        this.previousSampleHitRate = previousSampleHitRate;
    }
}
