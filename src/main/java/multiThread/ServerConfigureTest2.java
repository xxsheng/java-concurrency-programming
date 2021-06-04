package multiThread;

import java.beans.ConstructorProperties;

public class ServerConfigureTest2 implements ServerConfigureMXBean {

    private int port;

    private String host;

    private int maxThread;

    private int minThread;

    private User1 user1;



    public ServerConfigureTest2(int port, String host, int maxThread, int minThread, User1 user1) {
        this.port = port;
        this.host = host;
        this.maxThread = maxThread;
        this.minThread = minThread;
        this.user1 = user1;
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
        return this.user1;
    }

    @Override
    public void setUser1(User1 user1) {
        this.user1 = user1;
    }


    public static class User1 {
        private String userName;

        public String getUserName() {
            return userName;
        }

//        public void setUserName(String userName) {
//            this.userName = userName;
//        }

        public String getPassword() {
            return password;
        }

//        public void setPassword(String password) {
//            this.password = password;
//        }

        private String password;

        @ConstructorProperties({"userName", "password"})
        public User1(String userName, String password) {
            this.userName = userName;
            this.password = password;
        }
    }


}

