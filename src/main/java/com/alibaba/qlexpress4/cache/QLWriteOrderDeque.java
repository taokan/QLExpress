package com.alibaba.qlexpress4.cache;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @Author TaoKan
 * @Date 2023/12/10 2:12 PM
 */
final class QLWriteOrderDeque<E extends QLWriteOrderDeque.QLWriteOrder<E>> extends QLAbstractLinkedDeque<E> {
    QLWriteOrderDeque() {
    }

    public boolean contains(Object o) {
        return o instanceof QLWriteOrder && this.contains((QLWriteOrderDeque.QLWriteOrder)o);
    }

    boolean contains(QLWriteOrder<?> e) {
        return e.getPreviousInWriteOrder() != null || e.getNextInWriteOrder() != null || e == this.first;
    }

    public boolean remove(Object o) {
        return o instanceof QLWriteOrder && this.remove((QLWriteOrder)o);
    }

    public boolean remove(E e) {
        if (this.contains(e)) {
            this.unlink(e);
            return true;
        } else {
            return false;
        }
    }

    public @Nullable E getPrevious(E e) {
        return e.getPreviousInWriteOrder();
    }

    public void setPrevious(E e, @Nullable E prev) {
        e.setPreviousInWriteOrder(prev);
    }

    public @Nullable E getNext(E e) {
        return e.getNextInWriteOrder();
    }

    public void setNext(E e, @Nullable E next) {
        e.setNextInWriteOrder(next);
    }

    interface QLWriteOrder<T extends QLWriteOrder<T>> {
        @Nullable T getPreviousInWriteOrder();

        void setPreviousInWriteOrder(@Nullable T var1);

        @Nullable T getNextInWriteOrder();

        void setNextInWriteOrder(@Nullable T var1);
    }
}
