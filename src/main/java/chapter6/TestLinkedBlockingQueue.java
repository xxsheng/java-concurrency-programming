package chapter6;

import chapter6.blockqueue.LinkedBlockingQueue;
import org.junit.Test;

public class TestLinkedBlockingQueue {

    @Test
    public void test1() throws InterruptedException {
        LinkedBlockingQueue<String> strings = new LinkedBlockingQueue<>();

        strings.offer("222");
        strings.offer("333");
        String take = strings.take();
    }
}
