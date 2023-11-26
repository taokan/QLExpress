package com.alibaba.qlexpress4.cache;

import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @Author TaoKan
 * @Date 2023/11/12 7:11 PM
 */
public interface QLLinkedDeque<E> extends Deque<E> {

    /**
     * Returns if the element is at the front of the deque.
     *
     * @param e the linked element
     */
    boolean isFirst(E e);

    /**
     * Returns if the element is at the back of the deque.
     *
     * @param e the linked element
     */
    boolean isLast(E e);

    /**
     * Moves the element to the front of the deque so that it becomes the first element.
     *
     * @param e the linked element
     */
    void moveToFront(E e);

    /**
     * Moves the element to the back of the deque so that it becomes the last element.
     *
     * @param e the linked element
     */
    void moveToBack(E e);

    /**
     * Retrieves the previous element or <tt>null</tt> if either the element is unlinked or the first
     * element on the deque.
     */
    E getPrevious(E e);

    /** Sets the previous element or <tt>null</tt> if there is no link. */
    void setPrevious(E e, E prev);

    /**
     * Retrieves the next element or <tt>null</tt> if either the element is unlinked or the last
     * element on the deque.
     */
    E getNext(E e);

    /** Sets the next element or <tt>null</tt> if there is no link. */
    void setNext(E e, E next);

    @Override PeekingIterator<E> iterator();

    @Override
    PeekingIterator<E> descendingIterator();

    interface PeekingIterator<E> extends Iterator<E> {

        /** Returns the next element in the iteration, without advancing the iteration. */
        E peek();

        /** Returns an iterator that returns the first iteration followed by the second iteration. */
        static <E> PeekingIterator<E> concat(PeekingIterator<E> first, PeekingIterator<E> second) {
            return new PeekingIterator<E>() {
                @Override public boolean hasNext() {
                    return first.hasNext() || second.hasNext();
                }
                @Override public E next() {
                    if (first.hasNext()) {
                        return first.next();
                    } else if (second.hasNext()) {
                        return second.next();
                    }
                    throw new NoSuchElementException();
                }
                @Override public E peek() {
                    return first.hasNext() ? first.peek() : second.peek();
                }
            };
        }

        /** Returns an iterator that selects the greater element from the backing iterators. */
        static <E> PeekingIterator<E> comparing(PeekingIterator<E> first, PeekingIterator<E> second, Comparator<E> comparator) {
            return new PeekingIterator<E>() {
                @Override public boolean hasNext() {
                    return first.hasNext() || second.hasNext();
                }
                @Override public E next() {
                    if (!first.hasNext()) {
                        return second.next();
                    } else if (!second.hasNext()) {
                        return first.next();
                    }
                    E o1 = first.peek();
                    E o2 = second.peek();
                    boolean greaterOrEqual = (comparator.compare(o1, o2) >= 0);
                    return greaterOrEqual ? first.next() : second.next();
                }
                @Override public E peek() {
                    if (!first.hasNext()) {
                        return second.peek();
                    } else if (!second.hasNext()) {
                        return first.peek();
                    }
                    E o1 = first.peek();
                    E o2 = second.peek();
                    boolean greaterOrEqual = (comparator.compare(o1, o2) >= 0);
                    return greaterOrEqual ? first.peek() : second.peek();
                }
            };
        }
    }
}
