package chapter6;


import org.junit.Before;
import org.junit.Test;


import java.util.concurrent.LinkedTransferQueue;
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
        queue.transfer("22");
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
}
