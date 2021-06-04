package multiThread;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class TestMain {

    public static void main(String[] args) throws MalformedObjectNameException, MBeanRegistrationException, InstanceAlreadyExistsException, NotCompliantMBeanException, InterruptedException {
//        testServerConfigureMBean();

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        for (ThreadInfo threadInfo : threadInfos) {
            System.out.println(threadInfo.getThreadId() + threadInfo.getThreadName());
        }
    }

    private static void testServerConfigureMBean() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, InterruptedException {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        ServerConfigureTest2 serverConfigure = new ServerConfigureTest2(8080, "china.com", 100, 20, new ServerConfigureTest2.User1("222", "2323"));
        ObjectName objectName = new ObjectName("multiThread:type=ServerConfigure");
        platformMBeanServer.registerMBean(serverConfigure, objectName);
        System.out.println("wait-----");
        Thread.sleep(Long.MAX_VALUE);
    }
}
