package chapter4.connection;

import java.sql.Connection;
import java.util.LinkedList;

public class ConnectionPool {

    private LinkedList<Connection> pool = new LinkedList<>();

    public ConnectionPool(int initalSize) {
        if (initalSize > 0) {
            for (int i = 0; i < initalSize; i++) {
                pool.addLast(ConnectionDriver.createConnection());
            }
        }
    }

    public void releaseConnection(Connection connection) {
        if (connection != null) {
            synchronized (pool) {
                // 链接释放后需要进行通知，这样其他消费者能够感知到连接池中已经归还了一个链接
                pool.addLast(connection);
                pool.notifyAll();
            }
        }
    }

    public Connection fetchConnection(long mills) throws InterruptedException {
        synchronized (pool) {
            if (mills < 0) {
                while (pool.isEmpty()) {
                    pool.wait();
                }
                return pool.removeFirst();
            } else {
                long future = System.currentTimeMillis() + mills;
                long remaining = mills;
                while (remaining > 0 && pool.isEmpty()) {
                    pool.wait(remaining);
                    remaining = future - System.currentTimeMillis();
                }
                Connection result = null;
                if (!pool.isEmpty()) {
                    return pool.removeFirst();
                } else  {
                    return result;
                }
            }
        }
    }
}
