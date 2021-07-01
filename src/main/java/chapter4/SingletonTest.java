package chapter4;

public class SingletonTest {
    private SingletonTest() {

    }
    private final static SingletonTest instance = new SingletonTest();

    private SingletonTest getInstance() {
        return instance;
    }
}


class SingletonTest1 {
    private static class Singleton {
        private static final Singleton instance = new Singleton();
    }

    public static Singleton getInstance() {
        return Singleton.instance;
    }
}
