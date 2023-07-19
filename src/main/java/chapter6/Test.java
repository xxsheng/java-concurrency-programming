package chapter6;

import java.io.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class Test {
    @org.junit.Test
    public void test1() throws IOException {
        ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
//        queue.add("22");
//        queue.add("33");
        byte[] buff = new byte[9046];
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(new File("/Users/xiaxiuqiang/Documents/xxq_document/03_document/1.txt")));
        objectOutputStream.writeObject(queue);
    }

    @org.junit.Test
    public void test2() throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("/Users/xiaxiuqiang/Documents/xxq_document/03_document/1.txt"));
        Object o = objectInputStream.readObject();
        System.out.println(o);
    }

    @org.junit.Test
    public void test3() throws InterruptedException {
        SynchronousQueue<String> strings = new SynchronousQueue<>();
//        System.out.println(strings.take());
        strings.put("ppp" );
        System.out.println("---");
    }
}
