public class VolatileTest {
    private volatile VolatileTest volatileTest = new VolatileTest();

    public static void main(String[] args) {
        System.out.println("------");
        System.out.println("------");

    }
    public VolatileTest()
    {
        int sum = 0;

        // 1_000_000 is F4240 in hex
        for (int i = 0 ; i < 1_000_000; i++)
        {
            sum = this.add(sum, 99); // 63 hex
        }

        System.out.println("Sum:" + sum);
    }
    public synchronized int add(int a, int b)
    {
        return a + b;
    }


}