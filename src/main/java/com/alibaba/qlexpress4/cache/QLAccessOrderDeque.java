package com.alibaba.qlexpress4.cache;

/**
 * @Author TaoKan
 * @Date 2023/11/12 7:08 PM
 */
public class QLAccessOrderDeque <E extends QLAccessOrderDeque.AccessOrder<E>> extends QLAbstractLinkedDeque<E> {

    @Override
    public boolean contains(Object o) {
        return (o instanceof QLAccessOrderDeque.AccessOrder<?>) && contains((QLAccessOrderDeque.AccessOrder<?>) o);
    }

    // A fast-path containment check
    boolean contains(QLAccessOrderDeque.AccessOrder<?> e) {
        return (e.getPreviousInAccessOrder() != null)
                || (e.getNextInAccessOrder() != null)
                || (e == first);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object o) {
        return (o instanceof QLAccessOrderDeque.AccessOrder<?>) && remove((E) o);
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


    interface AccessOrder<T extends QLAccessOrderDeque.AccessOrder<T>> {

        /**
         * Retrieves the previous element or <tt>null</tt> if either the element is unlinked or the
         * first element on the deque.
         */
        T getPreviousInAccessOrder();

        /** Sets the previous element or <tt>null</tt> if there is no link. */
        void setPreviousInAccessOrder(T prev);

        /**
         * Retrieves the next element or <tt>null</tt> if either the element is unlinked or the last
         * element on the deque.
         */
        T getNextInAccessOrder();

        /** Sets the next element or <tt>null</tt> if there is no link. */
        void setNextInAccessOrder(T next);
    }
}
