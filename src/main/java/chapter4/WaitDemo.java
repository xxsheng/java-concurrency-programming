package chapter4;

public class WaitDemo {
    Object lock = new Object();


    public void test1() throws InterruptedException {
        synchronized (lock) {
            System.out.println("to wait");
            lock.wait(10000);
            System.out.println("wakeUp");
        }
    }

    public void test2() {
        synchronized (lock) {
            System.out.println("get lock");
            lock.notifyAll();
            System.out.println("release lock");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        WaitDemo waitDemo = new WaitDemo();
        new Thread(() -> {
            try {
                waitDemo.test1();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            waitDemo.test2();
        }).start();
    }
}
