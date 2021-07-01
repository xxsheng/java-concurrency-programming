package chapter4;

public class Join {
    public static void main(String[] args) {

        Thread mainThread = Thread.currentThread();
        for (int i = 0; i <10; i++) {
            Thread thread = new Thread(new Domino(mainThread), String.valueOf(i));
            thread.start();
            mainThread = thread;
        }

        System.out.println("main thread terminate");
    }

    static class Domino implements Runnable {

        private Thread thread;

        public Domino(Thread thread) {
            this.thread = thread;
        }

        @Override
        public void run() {
            try {
                thread.join();
            } catch (InterruptedException e) {


            }

            System.out.println(Thread.currentThread().getName() + " terminate");
        }
    }
}
