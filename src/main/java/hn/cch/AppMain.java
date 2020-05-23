package hn.cch;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class AppMain {

    private static Logger logger = LoggerFactory.getLogger(AppMain.class);

    public static final String USER_DIR = System.getProperty("user.dir");

    public static final String ETC_DIR = USER_DIR + File.separator + "etc";

    public static final String LOG_PATH = ETC_DIR + File.separator + "log.properties";

    public static final String RSM_PATH = ETC_DIR + File.separator + "rsm.properties";

    public static final String redis_sentinel_host = "redis.sentinel.host";
    public static final String redis_sentinel_port = "redis.sentinel.port";
    public static final String redis_master_name = "redis.master.name";
    public static final String redis_master_auth = "redis.master.auth";


    public static void main(String[] args) {
        // 日志配置文件
        PropertyConfigurator.configure(LOG_PATH);

        // redis sentinel 配置文件
        Properties properties = new Properties();
        File file = new File(RSM_PATH);
        try {
            properties.load(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("properties load error : " + e.getMessage());
        }


        //连接池配置
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();


        // 以|作为分隔符
        String[] hosts = properties.getProperty(redis_sentinel_host).split("\\|");
        String[] ports = properties.getProperty(redis_sentinel_port).split("\\|");

        if (hosts.length != ports.length) {//配置文件校验
            logger.error("length hosts != ports");
            return;
        }

        //哨兵模式集群部署
        Set<String> sentinels = new HashSet<>();
        for (int i = 0; i < hosts.length; i++) {
            sentinels.add(hosts[i] + ":" + ports[i]);
        }

        //主机和密码
        String name = properties.getProperty(redis_master_name);
        String auth = properties.getProperty(redis_master_auth);

        //使用Java版哨兵模式集群部署的Redis的连接池
        JedisSentinelPool jedisSentinelPool =
                new JedisSentinelPool(name, sentinels, jedisPoolConfig, auth);

        //获取客户端
        Jedis jedis = jedisSentinelPool.getResource();

        logger.info(jedis.set(redis_sentinel_host, redis_sentinel_port));
        logger.info(jedis.set(redis_master_name, redis_master_auth));

        logger.info(redis_sentinel_host + ":" + jedis.get(redis_sentinel_host));
        logger.info(redis_master_name + ":" + jedis.get(redis_master_name));

        //释放客户端
        jedis.close();

    }

}
