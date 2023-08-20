package chapter6.fockjoin;

import java.util.concurrent.*;

public class ExecutorTest {

    public static void main(String[] args) {
        ThreadPoolExecutor executorService = new ThreadPoolExecutor(15, 50,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());

        new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                Object o = new Object();
                executorService.execute(new MyRunnable(i));
            }
        }).start();

        new Thread(() -> {
            while (true) {
                System.out.println();
                System.out.println("监控开始");
                System.out.print("活跃线程：" + executorService.getActiveCount());
                System.out.print("核心线程：" + executorService.getCorePoolSize());
                System.out.print("完成任务数量：" + executorService.getCompletedTaskCount());
                System.out.print("最大线程池：" + executorService.getMaximumPoolSize());
                System.out.print("最大达到线程池：" + executorService.getLargestPoolSize());
                System.out.print("线程队列数量：" + executorService.getTaskCount());
                BlockingQueue<Runnable> queue = executorService.getQueue();
                for (Runnable runnable : queue) {
                    if (runnable instanceof MyRunnable) {
                        System.out.println("当前队列排序：" + ((MyRunnable) runnable).getI());
                    }
                }
            }
        }).start();

    }

    public static class MyRunnable implements Runnable {

        private int i;
        public MyRunnable(int i) {
            this.i = i;
        }

        public int getI() {
            return i;
        }

        @Override
        public void run() {
            try {
                TimeUnit.SECONDS.sleep(5);
                System.out.println(Thread.currentThread().getName());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
