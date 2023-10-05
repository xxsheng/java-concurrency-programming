package chapter6.fockjoin;

import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.concurrent.*;

public class CountTask extends RecursiveTask<Integer> {
    private static final int THRESHOLD = 2;
    private int start;
    private int end;

    public CountTask(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    protected Integer compute() {
        int sum = 0;
        boolean canCompute = (end - start) <= THRESHOLD;
        if (canCompute) {
            for (int i = start; i <= end; i++) {
                sum = sum + i;
            }

        }
        else {
            int middle = (start + end) / 2;
            CountTask leftTask = new CountTask(start, middle);
            CountTask rightTask = new CountTask(middle + 1, end);
            leftTask.fork();
            rightTask.fork();
            int leftResult = leftTask.join();
            int rightResult = rightTask.join();

            sum = leftResult + rightResult;
        }

        return sum;
    }
    static volatile int a = 9;
    // Active counts
    private static final int  AC_SHIFT   = 48;
    private static final long AC_UNIT    = 0x0001L << AC_SHIFT;
    private static final long AC_MASK    = 0xffffL << AC_SHIFT;

    // Total counts
    private static final int  TC_SHIFT   = 32;
    private static final long TC_UNIT    = 0x0001L << TC_SHIFT;
    private static final long TC_MASK    = 0xffffL << TC_SHIFT;
    private static final long ADD_WORKER = 0x0001L << (TC_SHIFT + 15); // sign

    // Lower and upper word masks
    // 低32位全是1
    private static final long SP_MASK    = 0xffffffffL;
    // 高32位全是1，低32位全是0
    private static final long UC_MASK    = ~SP_MASK;
    public static void main(String[] args) throws ExecutionException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        ForkJoinPool forkJoinPool = new ForkJoinPool(0x7fff);
        CountTask task = new CountTask(1, 100);
        forkJoinPool.submit(task);
        System.out.println(task.get());

        System.out.println(Integer.MAX_VALUE);
        System.out.println(Integer.numberOfLeadingZeros(4));

        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        System.out.println(unsafe.arrayBaseOffset(ForkJoinPool[].class));
        System.out.println(31 - Integer.numberOfLeadingZeros(16));
//
////        System.out.println(unsafe.arrayBaseOffset(int[].class));
//        int longScale = unsafe.arrayIndexScale(Long[].class);
//        System.out.println(longScale);
//        System.out.println(Integer.numberOfLeadingZeros(longScale));
//        System.out.println(Integer.toBinaryString(longScale));
//        int objectScale = unsafe.arrayIndexScale(ForkJoinTask[].class);
//        System.out.println(objectScale);
//        System.out.println(2<<2);
//        System.out.println(1<<2);


        Object o1= new Object();
        Object o2= new Object();
        TestObject testObject = new TestObject(o1);
//        new Thread(() -> {
//
//            while (testObject.getAbc()==o1) {
//
////                System.out.println("test is 123");
//            }
////            System.out.println("test abc is" + testObject.getAbc());
//        }).start();

//        TimeUnit.SECONDS.sleep(5);
//        long abcFieldOffset = unsafe.objectFieldOffset(TestObject.class.getDeclaredField("abc"));
//        unsafe.compareAndSwapObject(testObject,abcFieldOffset,o1, o2);

//        for (int i = 0; i < 100; i++) {
//            int n = (i > 1) ? i - 1 : 1;
//            n |= n >>> 1; n |= n >>> 2;  n |= n >>> 4;
//            n |= n >>> 8; n |= n >>> 16; n = (n + 1) << 1;
//
//            System.out.println("n:" + n + "i:" + i);
//        }

        int count = -32767;
        System.out.println(Integer.toBinaryString(32767));
        System.out.println(Integer.toBinaryString(-32767));
        System.out.println(Long.toBinaryString(-4294967296l));
        System.out.println((short)-32767);

        long c = ((count << AC_SHIFT) & AC_MASK) | ((count << TC_SHIFT) & TC_MASK);
        System.out.println(c);

        long c1 =  (c + AC_UNIT);
        System.out.println(c1);
        System.out.println(Long.toBinaryString(c1));

        int t = (short)(c >>> TC_SHIFT);
        System.out.println(t);
    }

    static class TestObject {
        private Object abc;

        public TestObject(Object abc) {
            this.abc = abc;
        }

        public Object getAbc() {
            return abc;
        }

        public void setAbc(Object abc) {
            this.abc = abc;
        }
    }
}
