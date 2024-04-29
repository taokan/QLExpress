package com.alibaba.qlexpress4.cache;

/**
 * @Author TaoKan
 * @Date 2023/11/12 7:08 PM
 */
public class QLAccessOrderDeque <E extends QLAccessOrderDeque.QLAccessOrder<E>> extends QLAbstractLinkedDeque<E> {

    @Override
    public boolean contains(Object o) {
        return (o instanceof QLAccessOrderDeque.QLAccessOrder<?>) && contains((QLAccessOrderDeque.QLAccessOrder<?>) o);
    }

    // A fast-path containment check
    boolean contains(QLAccessOrderDeque.QLAccessOrder<?> e) {
        return (e.getPreviousInAccessOrder() != null)
                || (e.getNextInAccessOrder() != null)
                || (e == first);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object o) {
        return (o instanceof QLAccessOrderDeque.QLAccessOrder<?>) && remove((E) o);
    }

    // A fast-path removal
    boolean remove(E e) {
        if (contains(e)) {
            unlink(e);
            return true;
        }
        return false;
    }

    @Override
    public E getPrevious(E e) {
        return e.getPreviousInAccessOrder();
    }

    @Override
    public void setPrevious(E e, E prev) {
        e.setPreviousInAccessOrder(prev);
    }

    @Override
    public E getNext(E e) {
        return e.getNextInAccessOrder();
    }

    @Override
    public void setNext(E e, E next) {
        e.setNextInAccessOrder(next);
    }


    interface QLAccessOrder<T extends QLAccessOrderDeque.QLAccessOrder<T>> {

        T getPreviousInAccessOrder();

        void setPreviousInAccessOrder(T prev);


        T getNextInAccessOrder();

        void setNextInAccessOrder(T next);
    }
}
