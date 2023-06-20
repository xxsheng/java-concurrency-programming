package chapter6;

import org.junit.Test;

public class TestConcurrentLinkedQueue {

    @Test
    public void testAdd() throws NoSuchFieldException {
        ConcurrentLinkedQueue<String> strings = new ConcurrentLinkedQueue<>();
        strings.add("1234");
//        String peek = strings.peek();
        String poll = strings.poll();
        strings.add("12345");
        strings.add("12346");
        strings.remove("12345");

//        java.util.concurrent.ConcurrentLinkedQueue<String> queue = new java.util.concurrent.ConcurrentLinkedQueue();
//        queue.add("123");
//        System.out.println("--");
    }

    @Test
             public void test() {
                 String tail = "";
                 String t = (tail = "oldTail");
                 tail = "newTail";
                 boolean isEqual = t != (t = tail); // <- 神奇吧
                 System.out.println("isEqual : " + isEqual); // isEqual : true
             }

    @Test
    public void testBugs() throws NoSuchFieldException {
        java.util.concurrent.ConcurrentLinkedQueue<String> queue = new java.util.concurrent.ConcurrentLinkedQueue<String>();
        String a="a";
        String b="b";
        queue.offer(a);
        for(int i=0;;i++){
//            if(i % 1024 == 0) {
//                System.out.println("i = "+i);
//            }
            queue.offer(b);
            queue.remove(b);

        }
    }

    @Test
    public void test2() {
        String a = "22";
        String b = "44";

        System.out.println(a == (a = b));
    }


}
