package chapter6.fockjoin.test;

import org.junit.Test;

import java.util.concurrent.ForkJoinPool;

public class ForkJoinPoolTest {
    @Test
    public void test1() {
        int test = 0x7fff;
        int test2 = -test;
        long a = (long)-test;
        System.out.println(a);
        System.out.println(Integer.toBinaryString(test));
        System.out.println(Integer.toBinaryString(test2));
        System.out.println(Long.toBinaryString(a));
        System.out.println(Long.toBinaryString(a + (test-1)));
        System.out.println(a + (test-1));
    }

    @Test
    public void test2() {
        long SP_MASK    = 0xffffffffL;
        System.out.println(Long.toBinaryString(~SP_MASK));
        System.out.println(~SP_MASK);
    }

    @Test
    public void test3() {
        int a = 1;
        int b = 2;

        System.out.println(a == (a = b));
        System.out.println(a);
    }
}
