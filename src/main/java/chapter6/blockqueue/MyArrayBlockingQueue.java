package chapter6.blockqueue;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class MyArrayBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable  {

    int count, removeIndex, addIndex;

    Object[] items;

    ReentrantLock lock;
    Condition notEmpty;
    Condition notFull;

    public MyArrayBlockingQueue(int capacity, boolean fair) {
        this.items = new Object[capacity];
        this.lock = new ReentrantLock(fair);
        this.notEmpty = lock.newCondition();
        this.notFull = lock.newCondition();
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public void put(E e) throws InterruptedException {
        Objects.requireNonNull(e);
        lock.lockInterruptibly();
        try {
            if (count == items.length) {
                notFull.await();
            }
            enqueue(e);
        } finally {
            lock.unlock();
        }
    }

    private void enqueue(E e) {
        items[addIndex++] = e;
        if (addIndex == items.length) addIndex = 0;
        count++;
        notEmpty.signal();
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(e);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            long nanos = unit.toNanos(timeout);
            while (count == items.length) {
                if (nanos <= 0) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(e);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0) {
                notEmpty.await();
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    private E dequeue() {
        E e = (E)items[removeIndex];
        items[removeIndex] = null;
        if (++removeIndex == items.length) removeIndex = 0;
        count--;
        notFull.signal();
        return e;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            long nanos = unit.toNanos(timeout);
            while (count == 0) {
                if (nanos <= 0 ){
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return items.length - count;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return 0;
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        return 0;
    }

    @Override
    public boolean offer(E e) {
        Objects.requireNonNull(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count == items.length) {
                return false;
            }
            enqueue(e);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count == 0) {
                return null;
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return itemAt(removeIndex);
        } finally {
            lock.unlock();
        }
    }

    private E itemAt(int removeIndex) {
        return (E)items[removeIndex];
    }

    private class Itr implements Iterator<E> {
        int remaining;
        int nextIndex;
        E nextItem;
        int lastIndex;
        E lastItem;

        public Itr() {
            final ReentrantLock lock = MyArrayBlockingQueue.this.lock;
            lock.lock();
            try {

                if ((this.remaining = count) > 0) {

                    this.nextIndex = removeIndex;
                    this.nextItem = itemAt(nextIndex);
                }
                this.lastIndex = -1;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean hasNext() {
            return remaining>0;
        }

        @Override
        public E next() {
            final ReentrantLock lock = MyArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if (remaining <= 0) {
                    throw new NoSuchElementException();
                }
                lastIndex = nextIndex;
                E e = itemAt(nextIndex);
                if (e == null) {
                    nextItem = e;
                    lastItem = null;
                } else {
                    lastItem = e;
                }
                while ((--remaining) > 0 && (nextItem = itemAt(nextIndex = (++nextIndex == items.length ? 0 : nextIndex))) == null)
                    ;
                return e;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void remove() {
            final ReentrantLock lock = MyArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                int i = lastIndex;
                if (i == -1) {
                    throw new IllegalStateException();
                }
                lastIndex = -1;
                E x = lastItem;
                lastItem = null;
                if (x != null && x == itemAt(i)) {
                    boolean removeHead = (i == removeIndex);
                    removeAt(i);
                    if (!removeHead) {
                        nextIndex = dec(nextIndex);
                    }
                }
                items[i] = null;

            } finally {
                lock.unlock();
            }

        }

        private void removeAt(int i) {
            final Object[] items = MyArrayBlockingQueue.this.items;
            if (i == removeIndex) {
                items[removeIndex] = null;
                removeIndex = inc(removeIndex);
            } else {

                for (;;) {
                    int nexti = inc(i);
                    if (nexti != addIndex) {
                        items[i] = itemAt(nexti);
                        i = nexti;
                    } else {
                        items[i] = null;
                        addIndex = i;
                        break;
                    }
                }
            }
            count--;
            notFull.signal();
        }

        private int inc(int i) {
            return ++i == items.length ? 0 : i;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Iterator.super.forEachRemaining(action);
        }
    }

    private int dec(int i) {
        return (i == 0 ? items.length : i) -1;
    }
}
