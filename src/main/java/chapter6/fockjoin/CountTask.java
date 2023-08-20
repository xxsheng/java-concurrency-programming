package chapter6.fockjoin;

import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

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

    public static void main(String[] args) throws ExecutionException, InterruptedException, NoSuchFieldException, IllegalAccessException {
//        ForkJoinPool forkJoinPool = new ForkJoinPool(0x7fff);
//        CountTask task = new CountTask(1, 100);
//        forkJoinPool.submit(task);
//        System.out.println(task.get());
//
//        System.out.println(Integer.MAX_VALUE);
//        System.out.println(Integer.numberOfLeadingZeros(4));
        ForkJoinTask[] array = new ForkJoinTask[10];
        array[0] = new ForkJoinTask() {
            @Override
            public Object getRawResult() {
                return 0;
            }

            @Override
            protected void setRawResult(Object value) {

            }

            @Override
            protected boolean exec() {
                return false;
            }
        };
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);

//        System.out.println(unsafe.arrayBaseOffset(int[].class));
        System.out.println(unsafe.arrayIndexScale(Long[].class));
        System.out.println(unsafe.arrayIndexScale(ForkJoinTask[].class));
        System.out.println(2<<2);
        System.out.println(1<<2);
    }
}
