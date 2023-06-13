package chapter5;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TestCondition {

    public static void main(String[] args) {
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();

        new Thread(() -> {
            System.out.println("线程1执行");

            lock.lock();
            try {
                condition.await();
                System.out.println("线程1 wake up");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock.unlock();
        }).start();

        new Thread(() -> {
            System.out.println("线程2执行");

            lock.lock();
            try {
                condition.await();
                System.out.println("线程2 wake up");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock.unlock();
        }).start();


        new Thread(() -> {
            System.out.println("线程3执行");
            lock.lock();
            // 线程1和线程2重新入队
            condition.signalAll();
            // 重新唤起队列
            lock.unlock();
        }).start();
    }
}
