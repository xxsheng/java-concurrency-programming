package chapter6;


import org.junit.Before;
import org.junit.Test;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class LinkedTransferQueueTest {

    LinkedTransferQueue<String> queue;

    @Before
    public void before() {
        queue = new LinkedTransferQueue<>();
    }
    @Test
    public void test1() {
//        LinkedTransferQueue queue = new LinkedTransferQueue<String>();
        // 不需要等待，如果没有线程等待获取元素，则直接返回false
        queue.tryTransfer("322");
    }

    @Test
    public void testTryTransferTimeOut() throws InterruptedException {
        queue.tryTransfer("222", 30, TimeUnit.SECONDS);
    }

    @Test
    public void testAdd()  {
        boolean add = queue.add("222");
    }

    @Test
    public void testOffer() {
        boolean offer = queue.offer("222");
        assert offer;
    }

    @Test
    public void testOfferTimed() {
        boolean offer = queue.offer("222", 20, TimeUnit.SECONDS);
        assert offer;
    }

    @Test
    public void testTransfer() throws InterruptedException {
        System.out.println(queue.tryTransfer("22"));
    }

    @Test
    public void test2() {
        new Thread(() -> {
            System.out.println("---");
            queue.add("222");
        }).start();

        new Thread(() -> {
            try {
                System.out.println("--");
                System.out.println(queue.take());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                System.out.println("--");
                System.out.println(queue.take());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                System.out.println("--");
                System.out.println(queue.take());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
                System.out.println("--");
                System.out.println(queue.add("22"));
        }).start();

        System.out.println("--");
    }

    @Test
    public void test3() {

        new Thread(() -> {
            System.out.println("---");
            queue.add("222");
        }).start();

        new Thread(() -> {
                System.out.println("--");
                System.out.println(queue.add("333"));

        }).start();


        System.out.println("--");
    }

    @Test
    public void testTryAppend() {
        new Thread(() -> {
            System.out.println("----");
            queue.add("2221");
        }).start();

        new Thread(() -> {
            System.out.println("----");
            queue.add("2223");
        }).start();

        new Thread(() -> {
            System.out.println("----");
            queue.add("2224");
        }).start();

        new Thread(() -> {
            System.out.println("----");
            queue.add("2225");
        }).start();

        new Thread(() -> {
            System.out.println("----");
            queue.add("2226");
        }).start();

        new Thread(() -> {
            System.out.println("----");
            queue.add("2226");
        }).start();

        new Thread(() -> {
            System.out.println("----");
            try {
                queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            System.out.println("----");
            try {
                queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            System.out.println("----");
            try {
                queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            System.out.println("----");
            try {
                queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println("----");

    }

    @Test
    public void testUnSplice() throws InterruptedException {
        queue.tryTransfer("22", 30, TimeUnit.SECONDS);
    }

    @Test
    public void testIsEmpty() throws InterruptedException {
        new Thread(() -> {
            try {
                queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        TimeUnit.SECONDS.sleep(10);

        System.out.println(queue.isEmpty());
    }


}
