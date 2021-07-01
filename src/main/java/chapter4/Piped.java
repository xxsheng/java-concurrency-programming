package chapter4;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;

public class Piped {
    public static void main(String[] args) throws IOException {
        PipedWriter out = new PipedWriter();
        PipedReader in = new PipedReader();
        // 将输出流和输入流进行链接，否则在使用时会抛出IoException
        out.connect(in);
        Thread thread = new Thread(new Print(in), "PrintThread");
        thread.start();
        int receive = 0;

        while ((receive = System.in.read()) != -1) {
            out.write(receive);
        }

    }

    static class Print implements Runnable {

        private PipedReader in;

        public Print(PipedReader in) {
            this.in = in;
        }

        @Override
        public void run() {
            int receive = 0;
            while (true) {
                try {
                    if (((receive = in.read()) != -1)) {
                        System.out.println((char) receive);
                    }
                } catch (IOException e) {


                }

            }
        }
    }
}
