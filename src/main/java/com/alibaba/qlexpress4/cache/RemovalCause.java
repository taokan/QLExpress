package com.alibaba.qlexpress4.cache;

/**
 * @Author TaoKan
 * @Date 2024/2/18 10:11 PM
 */
public enum RemovalCause {
    EXPLICIT {
        public boolean wasEvicted() {
            return false;
        }
    },
    REPLACED {
        public boolean wasEvicted() {
            return false;
        }
    },
    COLLECTED {
        public boolean wasEvicted() {
            return true;
        }
    },
    EXPIRED {
        public boolean wasEvicted() {
            return true;
        }
    },
    SIZE {
        public boolean wasEvicted() {
            return true;
        }
    };

    private RemovalCause() {
    }

    public abstract boolean wasEvicted();
}
