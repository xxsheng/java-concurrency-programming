package chapter6;

import org.junit.Test;

import java.util.Map;

public class TestConcurrentHashMap {

    @Test
    public void testRehash() {

        Map<MyObject, String> test = new ConcurrentHashMap<MyObject, String>();
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        test.put(new MyObject(), "123");
        System.out.println("---s");
    }

    private class MyObject {
        @Override
        public int hashCode() {
            return 123;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }
    }
}
