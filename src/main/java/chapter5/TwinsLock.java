package chapter5;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class TwinsLock implements Lock {

    final Sync sync = new Sync(2);
    @Override
    public void lock() {
        sync.acquireShared(1);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        sync.releaseShared(1);
    }

    @Override
    public Condition newCondition() {
        return null;
    }

    static class Sync extends AbstractQueuedSynchronizer {

        public Sync(int count) {
            if (count > 2) {
                throw new IllegalArgumentException("无效的参数");
            }
            setState(count);
        }

        @Override
        protected int tryAcquireShared(int arg) {
            for (;;) {
                int state = getState();
                int newState = state - arg;
                if (newState < 0 || compareAndSetState(state, newState)) {
                    return newState;
                }
            }

        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            for (;;) {
                int state = getState();
                int newState = state + arg;
                if (compareAndSetState(state, newState)) {
                    return true;
                }
            }
        }
    }
}
