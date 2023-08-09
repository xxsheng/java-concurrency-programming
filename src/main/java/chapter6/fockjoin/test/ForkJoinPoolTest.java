package chapter6.fockjoin.test;

import org.junit.Test;

import java.util.concurrent.ForkJoinPool;

public class ForkJoinPoolTest {
    @Test
    public void test1() {
        ForkJoinPool.commonPool().shutdown();
    }
}
