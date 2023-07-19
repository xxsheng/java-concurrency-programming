package chapter6.blockqueue;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class SynchronousQueueTest {
    public static void main(String[] args) throws InterruptedException {
        SynchronousQueue<String> queue = new SynchronousQueue<>(false);


        Thread thread = new Thread(() -> {
            try {
                System.out.println("--thread1");
//                String take = queue.take();
                queue.put("222");
//                System.out.println(take);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        Thread.sleep(1000);
        new Thread(() -> {
            try {
                System.out.println("--thread1");
//                String take = queue.take();
                queue.take();
//                queue.put("2222");
//                System.out.println(take);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(1000);
        new Thread(() -> {
            try {
                thread.interrupt();
                System.out.println("--thread1");
//                String take = queue.take();
                queue.put("2222");
//                queue.put("2222");
//                System.out.println(take);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();



    }
}
