package com.alibaba.qlexpress4.cache;


import java.util.AbstractCollection;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * @Author TaoKan
 * @Date 2023/11/12 7:14 PM
 */
public abstract class QLAbstractLinkedDeque<E> extends AbstractCollection<E> implements QLLinkedDeque<E> {

    E first;
    E last;
    int modCount;

    void linkFirst(final E e) {
        final E f = first;
        first = e;

        if (f == null) {
            last = e;
        } else {
            setPrevious(f, e);
            setNext(e, f);
        }
        modCount++;
    }

    /**
     * Links the element to the back of the deque so that it becomes the last element.
     *
     * @param e the unlinked element
     */
    void linkLast(final E e) {
        final E l = last;
        last = e;

        if (l == null) {
            first = e;
        } else {
            setNext(l, e);
            setPrevious(e, l);
        }
        modCount++;
    }

    /** Unlinks the non-null first element. */
    @SuppressWarnings("NullAway")
    E unlinkFirst() {
        final E f = first;
        final E next = getNext(f);
        setNext(f, null);

        first = next;
        if (next == null) {
            last = null;
        } else {
            setPrevious(next, null);
        }
        modCount++;
        return f;
    }

    /** Unlinks the non-null last element. */
    @SuppressWarnings("NullAway")
    E unlinkLast() {
        final E l = last;
        final E prev = getPrevious(l);
        setPrevious(l, null);
        last = prev;
        if (prev == null) {
            first = null;
        } else {
            setNext(prev, null);
        }
        modCount++;
        return l;
    }

    /** Unlinks the non-null element. */
    void unlink(E e) {
        final E prev = getPrevious(e);
        final E next = getNext(e);

        if (prev == null) {
            first = next;
        } else {
            setNext(prev, next);
            setPrevious(e, null);
        }

        if (next == null) {
            last = prev;
        } else {
            setPrevious(next, prev);
            setNext(e, null);
        }
        modCount++;
    }

    @Override
    public boolean isEmpty() {
        return (first == null);
    }

    void checkNotEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Beware that, unlike in most collections, this method is <em>NOT</em> a constant-time operation.
     */
    @Override
    public int size() {
        int size = 0;
        for (E e = first; e != null; e = getNext(e)) {
            size++;
        }
        return size;
    }

    @Override
    @SuppressWarnings("PMD.AvoidReassigningLoopVariables")
    public void clear() {
        E e = first;
        while (e != null) {
            E next = getNext(e);
            setPrevious(e, null);
            setNext(e, null);
            e = next;
        }
        first = last = null;
        modCount++;
    }

    @Override
    public abstract boolean contains(Object o);

    @Override
    public boolean isFirst(E e) {
        return (e != null) && (e == first);
    }

    @Override
    public boolean isLast(E e) {
        return (e != null) && (e == last);
    }

    @Override
    public void moveToFront(E e) {
        if (e != first) {
            unlink(e);
            linkFirst(e);
        }
    }

    @Override
    public void moveToBack(E e) {
        if (e != last) {
            unlink(e);
            linkLast(e);
        }
    }

    @Override
    public E peek() {
        return peekFirst();
    }

    @Override
    public E peekFirst() {
        return first;
    }

    @Override
    public E peekLast() {
        return last;
    }

    @Override
    @SuppressWarnings("NullAway")
    public E getFirst() {
        checkNotEmpty();
        return peekFirst();
    }

    @Override
    @SuppressWarnings("NullAway")
    public E getLast() {
        checkNotEmpty();
        return peekLast();
    }

    @Override
    public E element() {
        return getFirst();
    }

    @Override
    public boolean offer(E e) {
        return offerLast(e);
    }

    @Override
    public boolean offerFirst(E e) {
        if (contains(e)) {
            return false;
        }
        linkFirst(e);
        return true;
    }

    @Override
    public boolean offerLast(E e) {
        if (contains(e)) {
            return false;
        }
        linkLast(e);
        return true;
    }

    @Override
    public boolean add(E e) {
        return offerLast(e);
    }

    @Override
    public void addFirst(E e) {
        if (!offerFirst(e)) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void addLast(E e) {
        if (!offerLast(e)) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public E poll() {
        return pollFirst();
    }

    @Override
    public E pollFirst() {
        return isEmpty() ? null : unlinkFirst();
    }

    @Override
    public E pollLast() {
        return isEmpty() ? null : unlinkLast();
    }

    @Override
    public E remove() {
        return removeFirst();
    }

    @Override
    @SuppressWarnings("NullAway")
    public E removeFirst() {
        checkNotEmpty();
        return pollFirst();
    }

    @Override
    public abstract boolean remove(Object o);

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    @Override
    @SuppressWarnings("NullAway")
    public E removeLast() {
        checkNotEmpty();
        return pollLast();
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object o : c) {
            modified |= remove(o);
        }
        return modified;
    }

    @Override
    public void push(E e) {
        addFirst(e);
    }

    @Override
    public E pop() {
        return removeFirst();
    }

    @Override
    public PeekingIterator<E> iterator() {
        return new QLAbstractLinkedIterator(first) {
            @Override E computeNext() {
                return getNext((E) cursor);
            }
        };
    }

    @Override
    public PeekingIterator<E> descendingIterator() {
        return new QLAbstractLinkedIterator(last) {
            @Override E computeNext() {
                return getPrevious((E) cursor);
            }
        };
    }

    public abstract class QLAbstractLinkedIterator<E> implements PeekingIterator<E> {
        E previous;
        E cursor;

        int expectedModCount;

        /**
         * Creates an iterator that can can traverse the deque.
         *
         * @param start the initial element to begin traversal from
         */
        QLAbstractLinkedIterator(E start) {
            expectedModCount = modCount;
            cursor = start;
        }

        @Override
        public boolean hasNext() {
            checkForComodification();
            return (cursor != null);
        }

        @Override
        public E peek() {
            return cursor;
        }

        @Override
        @SuppressWarnings("NullAway")
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            previous = cursor;
            cursor = computeNext();
            return previous;
        }

        /** Retrieves the next element to traverse to or <tt>null</tt> if there are no more elements. */
        abstract E computeNext();

        @Override
        public void remove() {
            if (previous == null) {
                throw new IllegalStateException();
            }
            checkForComodification();

            QLAbstractLinkedDeque.this.remove(previous);
            expectedModCount = modCount;
            previous = null;
        }

        /**
         * If the expected modCount value that the iterator believes that the backing deque should have
         * is violated then the iterator has detected concurrent modification.
         */
        void checkForComodification() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
