package chapter4;

import chapter4.utils.SleepUtils;

import java.util.concurrent.TimeUnit;

public class Interrupted {
    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new SleepRunner(), "SleepThread");
        thread.setDaemon(true);

        Thread busyThread = new Thread(new BusyRunner(), "BusyThread");
        busyThread.setDaemon(true);

        thread.start();
        busyThread.start();
        // 休眠五秒，让sleepThread 和 BusyThread充分运行
        TimeUnit.SECONDS.sleep(5);
        try {
            thread.interrupt();
        } catch (Exception e) {
            System.out.println("test");
            e.printStackTrace();
        }
        busyThread.interrupt();
        System.out.println("sleep thread interrupted is " + thread.isInterrupted());
        System.out.println("BusyThread interrupted is " + busyThread.isInterrupted());

        SleepUtils.second(2);
    }

    static class SleepRunner implements Runnable {

        @Override
        public void run() {
            while (true) {
                SleepUtils.second(10);
            }
        }
    }

    static class BusyRunner implements Runnable {

        @Override
        public void run() {
            while (true) {

            }
        }
    }
}
