import java.util.HashMap;
import java.util.Map;

public class SimpleTest1
{
    public volatile Map<String, String> map = new HashMap<>();

    public SimpleTest1()
    {
        int sum = 0;

        // 1_000_000 is F4240 in hex
        for (int i = 0 ; i < 1_000_000; i++)
        {
            sum = this.add(sum, 99); // 63 hex
        }

        System.out.println("Sum:" + sum);
    }

    public int add(int a, int b)
    {
        return a + b;
    }

    public static void main(String[] args)
    {
        new SimpleTest1();
    }
}
