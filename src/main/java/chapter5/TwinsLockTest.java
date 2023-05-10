package chapter5;

import chapter4.utils.SleepUtils;

public class TwinsLockTest {

    public static void main(String[] args) {
        TwinsLock twinsLock = new TwinsLock();

        class Work extends Thread {
            @Override
            public void run() {
                SleepUtils.second(1);
                twinsLock.lock();
                System.out.println(Thread.currentThread().getName());
                SleepUtils.second(1);
                twinsLock.unlock();
            }
        }

        for (int i = 0; i<10; i++) {
            Work work = new Work();
            work.start();
        }

        for (int i =0; i<10; i++) {
            SleepUtils.second(1);
            System.out.println();
        }
    }
}
