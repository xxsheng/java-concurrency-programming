package chapter6;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class SynchronousQueueTest {
    public static void main(String[] args) throws InterruptedException {
        SynchronousQueue<String> queue = new SynchronousQueue<>(false);



        new Thread(() -> {
            try {
                System.out.println("--thread1");
//                String take = queue.take();
                queue.put("222");
//                System.out.println(take);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

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
                System.out.println("--thread1");
//                String take = queue.take();
                queue.take();
//                queue.put("2222");
//                System.out.println(take);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();



    }
}
