import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;


public class DbCfg {
    final List<String> hosts;
    String user;
    String password;
    private  final AtomicInteger index = new AtomicInteger(0);
    private Driver driver = null;
    private URLClassLoader loader;
    private final Properties info;
    public void setDriver(String jarfile) {
        try {
            if(jarfile == null || jarfile.isEmpty()) {
                initDriver();
                return;
            }
            //there maybe some connection not closed, if do loader.close(), will cause ClassNotFoundException
            //during call cmd .logoff
            loader = new URLClassLoader(new URL[]{new URL("file:" + jarfile)},Thread.currentThread().getContextClassLoader().getParent());
            Class<?> clazz;
            try{
                clazz = loader.loadClass("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                clazz = loader.loadClass("com.mysql.jdbc.Driver");
            }
            driver = (Driver) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.out.println("can not load driver " + e.getMessage());
        }
    }
    public Connection getConnection() {
        if(hosts.isEmpty()) {
            return null;
        }
        int i = index.addAndGet(1);
        String host = hosts.get(i % hosts.size());
        String url = "jdbc:mysql://" + host + "?characterEncoding=utf8&useSSL=false&useServerPrepStmts=true&cachePrepStmts=true&prepStmtCacheSqlLimit=10000000&useConfigs=maxPerformance&rewriteBatchedStatements=true&defaultfetchsize=-2147483648&allowMultiQueries=true&allowLoadLocalInfile=true";

        info.setProperty("password",password);
        Connection conn;
        Instant instant1 = Instant.now();
        Instant instant2;
        try {
            conn = driver.connect(url,info);
            instant2 = Instant.now();
            System.out.println("driver: " + conn.getMetaData().getDriverVersion());
            System.out.println(Utils.getElapsedTime(instant1,instant2) + ", logon to " + host + " successfully!");
        } catch (SQLException e) {
            instant2 = Instant.now();
            System.out.println(Utils.getElapsedTime(instant1,instant2) + ", logon to " + host + " failed, " + e.getMessage());
            throw new RuntimeException(e);
        }
        return conn;
    }
    public void setHost(String host) {
        hosts.clear();
        Collections.addAll(hosts, host.split(","));
    }
    public void setUser(String auser) {
        user = auser;
        info.setProperty("user",user);
    }
    public void setPassword(String password) {
        this.password = password;
    }
    private void initDriver() {
        try {
            driver = new com.mysql.cj.jdbc.Driver();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public DbCfg() {
        hosts = new ArrayList<>();
        info = new Properties();
        initDriver();
    }
}
