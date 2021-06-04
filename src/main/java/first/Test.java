package first;

public class Test {
    static String a = "222";

    static {
        System.out.println("test");
    }

    static class ABC {
         String b = "434";
        ABC() {
            System.out.println("43434");
        }
    }

    public static void main(String[] args) {
        System.out.println(new ABC().b);
    }
}
