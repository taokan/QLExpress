package com.alibaba.qlexpress4.cache;

/**
 * @Author TaoKan
 * @Date 2024/2/18 9:42 PM
 */
public class QLCacheMainProtected {
    private long maxNum;
    private long weight;

    QLCacheMainProtected(){
        this.maxNum = 0;
        this.weight = 0;
    }

    public long getMaxNum() {
        return maxNum;
    }

    public long getWeight() {
        return weight;
    }
    public void setMaxNum(long maxNum) {
        this.maxNum = maxNum;
    }

    public void setWeight(long weight) {
        this.weight = weight;
    }
}
