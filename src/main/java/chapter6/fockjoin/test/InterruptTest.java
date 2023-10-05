package chapter6.fockjoin.test;

import java.util.concurrent.TimeUnit;

public class InterruptTest {

    public static void main(String[] args) throws InterruptedException {

        Thread thread = new Thread(() -> {
            try {


                System.out.println("thread interrupt :" + Thread.currentThread().isInterrupted());
                TimeUnit.SECONDS.sleep(4);
            } catch (InterruptedException e) {
                System.out.println("捕捉到异常");
                System.out.println("thread interrupt :" + Thread.currentThread().isInterrupted());
            }
        });
        System.out.println(thread.isInterrupted());
        thread.start();


        TimeUnit.SECONDS.sleep(1);
        thread.interrupt();
    }
}
