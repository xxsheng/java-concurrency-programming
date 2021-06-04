package multiThread;


import multiThread.ServerConfigureTest2.User1;

public interface ServerConfigureMXBean {

    public int getPort();

    public void setPort(int port);

    public String getHost();

    public void setHost(String host);

    public int getMaxThread();

    public void setMaxThread(int maxThread);

    public int getMinThread();

    public void setMinThread(int minThread);

    public User1 getUser1();

    public void setUser1(User1 user1);

}
