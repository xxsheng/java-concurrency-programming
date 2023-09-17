package chapter6.fockjoin.test;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

    @Test
    public void test4() {
        System.out.println(Integer.toBinaryString(Integer.MIN_VALUE));
        System.out.println(Integer.toBinaryString(Integer.MIN_VALUE >>> 1));
    }

    @Test
    public void test5() {
        System.out.println(Integer.toBinaryString(49));
        System.out.println(Integer.toBinaryString(3));
        System.out.println(Integer.toBinaryString(Integer.MAX_VALUE));
        System.out.println(Long.toBinaryString(Long.MIN_VALUE >>> 32));

        System.out.println(Integer.toBinaryString(1 << 31));
        System.out.println(1 << 13);
    }

    @Test
    public void test6() {
        int n = 0x8000;
        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 1;
        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 2;
        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 4;
        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 8;
        System.out.println(Integer.toBinaryString(n));
        n |= n >>> 16;
        System.out.println(Integer.toBinaryString(n));
        n = (n + 1);
        System.out.println(Integer.toBinaryString(n));
        System.out.println(n);
    }

    @Test
    public void test7() {
        String test1 = "\u07ff";
        System.out.println(Integer.toHexString((int)test1.charAt(0)));
        byte[] bytes = test1.getBytes(StandardCharsets.UTF_8);
        System.out.println(Arrays.toString(bytes));

    }

    @Test
    public void test8() {
//        String word = "\uD835\uDD46";
//        System.out.println(word);
        System.out.println(1L<<47);

    }
}
