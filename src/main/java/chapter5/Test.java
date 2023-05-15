package chapter5;

public class Test {

    public static void main(String[] args) {
        int i = (1 << 16) - 1;
        System.out.println(Integer.toBinaryString(i));
        System.out.println("---");
    }
}
