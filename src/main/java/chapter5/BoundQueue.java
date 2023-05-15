package chapter5;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BoundQueue<T> {
    private Object[] items;

    private int addCount, removeCount, count;

    private ReentrantLock reentrantLock = new ReentrantLock();
    private Condition condition1 = reentrantLock.newCondition();
    private Condition condition2 = reentrantLock.newCondition();

    public BoundQueue(int size) {
        this.items = new Object[size];
    }

    public void add(T t) throws InterruptedException {
        reentrantLock.lock();

        while (count == items.length) {
            condition1.await();
        }
        items[addCount] = t;
        if (++addCount == items.length) {
            addCount = 0;
        }
        count++;
        condition2.signal();

        reentrantLock.unlock();


    }

    public T remove() throws InterruptedException {
        reentrantLock.lock();
        while (count == 0) {
            condition2.await();
        }
        Object t = items[removeCount++];
        if (removeCount == items.length) {
            removeCount = 0;
        }

        count--;
        condition1.signal();
        reentrantLock.unlock();

        return (T) t;

    }
}
