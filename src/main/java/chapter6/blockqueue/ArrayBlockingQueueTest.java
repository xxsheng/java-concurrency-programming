package chapter6.blockqueue;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ArrayBlockingQueueTest<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable  {

    int count, removeIndex, addIndex;

    Object[] items;

    ReentrantLock lock;
    Condition notEmpty;
    Condition notFull;

    public ArrayBlockingQueueTest(int capacity, boolean fair) {
        this.items = new Object[capacity];
        this.lock = new ReentrantLock(fair);
        this.notEmpty = lock.newCondition();
        this.notFull = lock.newCondition();
    }

    @Override
    public Iterator<E> iterator() {
        return null;
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public void put(E e) throws InterruptedException {
        lock.lock();
        if (count == items.length) {
            notFull.await();
        }
        items[addIndex++] = e;
        count++;
        notEmpty.signal();
        lock.unlock();
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public E take() throws InterruptedException {
        return null;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public int remainingCapacity() {
        return 0;
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
        return false;
    }

    @Override
    public E poll() {
        return null;
    }

    @Override
    public E peek() {
        return null;
    }
}
