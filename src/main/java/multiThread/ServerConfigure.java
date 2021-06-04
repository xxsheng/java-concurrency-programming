package multiThread;


import static multiThread.ServerConfigureTest2.*;

public class ServerConfigure implements ServerConfigureMXBean {

    private int port;

    private String host;

    private int maxThread;

    private int minThread;

    public ServerConfigure(int port, String host, int maxThread, int minThread) {
        this.port = port;
        this.host = host;
        this.maxThread = maxThread;
        this.minThread = minThread;
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getHost() {
        return this.host;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getMaxThread() {
        return this.maxThread;
    }

    @Override
    public void setMaxThread(int maxThread) {
        this.maxThread = maxThread;
    }

    @Override
    public int getMinThread() {
        return this.minThread;
    }

    @Override
    public void setMinThread(int minThread) {
        this.minThread = minThread;
    }

    @Override
    public User1 getUser1() {
        return null;
    }

    @Override
    public void setUser1(User1 user) {

    }
}
