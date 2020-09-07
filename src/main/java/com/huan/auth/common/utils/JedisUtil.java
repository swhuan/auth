package com.huan.auth.common.utils;

import com.huan.auth.common.constant.RedisKeyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.*;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;


/**
 * redis工具类,兼容单机集群,兼容有密码无密码
 * @author swhuan
 */
@Component
public class JedisUtil {

    protected static final Logger logger = LoggerFactory.getLogger(JedisUtil.class);
    protected static final int timeout = 2000;
    private static final Long RELEASE_SUCCESS = 1L;
    private static final String LOCK_SUCCESS = "OK";
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    protected static ReentrantLock lockPool = new ReentrantLock();
    private static JedisCluster jedisCluster = null;
    private static JedisPool jedisPool = null;

    /**
     * redis类型,单机是0,集群是1
     */
    private static Integer jedisType = null;

    protected static String host;
    protected static String auth = "123456";

    @Value("${spring.redis.nodes}")
    public void setHost(String host) {
        JedisUtil.host = host;
    }


    @Value("${spring.redis.password}")
    public void setAuth(String auth) {
        JedisUtil.auth = auth;
    }

    private static String projectBranch;

    @Value("${project.branch}")
    public void setProjectBranch(String projectBranch){
        this.projectBranch=projectBranch;
    }

    @PostConstruct
    public void init() {
        jedisInit();
    }


    /**
     * 把项目的前缀给拼上
     * @param key
     * @return
     */
    public static String getKey(String key){
        return projectBranch+":"+key;
    }

    /**
     * 初始化jedisPool
     */
    private static void jedisInit() {
        // 当前锁是否已经锁住?if锁住了，do nothing; else continue
        assert !lockPool.isHeldByCurrentThread();

        // 配置发生变化的时候自动载入
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            // 是否启用后进先出, 默认true
            config.setLifo(true);
            // 最大空闲连接数
            config.setMaxIdle(10);
            // 获取连接时的最大等待毫秒数(如果设置为阻塞时BlockWhenExhausted), 如果超时就抛异常,小于零:阻塞不确定的时间
            config.setMaxWaitMillis(2000);
            // 逐出连接的最小空闲时间 默认1800000毫秒(30分钟)
            config.setMinEvictableIdleTimeMillis(1800000);
            // 最小空闲连接数, 默认0
            config.setMinIdle(0);
            // 每次逐出检查时 逐出的最大数目 如果为负数就是 : 1/abs(n), 默认3
            config.setNumTestsPerEvictionRun(3);
            // 对象空闲多久后逐出, 当空闲时间>该值 且 空闲连接>最大空闲数 时直接逐出,不再根据MinEvictableIdleTimeMillis判断  (默认逐出策略)
            config.setSoftMinEvictableIdleTimeMillis(1800000);
            // 在获取连接的时候检查有效性, 默认false
            config.setTestOnBorrow(true);
            config.setMaxTotal(50000);
            // 在空闲时检查有效性, 默认false
            config.setTestWhileIdle(false);

            logger.info("build jedisPool...");
            String[] split = host.split(",");
            if (split.length==1){
                //单机的
                jedisType = 0;
                String[] split1 = host.split(":");
                if (null==auth||"".equals(auth)){
                    jedisPool = new JedisPool(config,split1[0],Integer.valueOf(split1[1]),timeout);
                }else {
                    jedisPool = new JedisPool(config,split1[0],Integer.valueOf(split1[1]),timeout,auth);
                }

            }else if (split.length>1){
                //集群
                jedisType = 1;
                HashSet<HostAndPort> hostAndPorts = new HashSet<>();
                for (String s:split){
                    String[] split1 = s.split(":");
                    HostAndPort hostAndPort = new HostAndPort(split1[0], Integer.valueOf(split1[1]));
                    hostAndPorts.add(hostAndPort);
                }
                if (null==auth||"".equals(auth)){
                    //无密码的
                    jedisCluster = new JedisCluster(hostAndPorts,timeout,timeout,5,config);
                }else {
                    jedisCluster = new JedisCluster(hostAndPorts,timeout,timeout,5,auth,config);
                }
            }
        } catch (Exception ex) {
            logger.error("redisPoolInit failed#", ex);
        }
    }

    /**
     * @return
     */
    public static JedisCommands getJedisCommands() {
        if (jedisType == null) {
            jedisInit();
            return getJedisCommands();
        }else {
            if (0==jedisType){
                return jedisPool.getResource();
            }else{
                return jedisCluster;
            }
        }
    }

    /**
     * 尝试获取分布式锁
     *
     * @param jedis      Redis客户端
     * @param lockKey    锁
     * @param requestId  请求标识
     * @param expireTime 超期时间
     * @return 是否获取成功
     */
    public static boolean tryGetDistributedLock(JedisCommands jedis, String lockKey, String requestId, int expireTime) {

        long startTime = System.currentTimeMillis();
        String result = jedis.set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
        long endTime = System.currentTimeMillis();
        logger.info("get user redis lock use {} ms", endTime - startTime);
        return LOCK_SUCCESS.equals(result);

    }

    /**
     * 释放分布式锁
     *
     * @param jedisCommands     Redis客户端
     * @param lockKey   锁
     * @param requestId 请求标识
     * @return 是否释放成功
     */
    public static boolean releaseDistributedLock(JedisCommands jedisCommands, String lockKey, String requestId) {
        long startTime = System.currentTimeMillis();

        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = new Object();
        try {
            if (jedisCommands instanceof Jedis){
                Jedis jedis = (Jedis)jedisCommands;
                result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            }else if (jedisCommands instanceof JedisCluster){
                JedisCluster jedisCluster = (JedisCluster) jedisCommands;
                result = jedisCluster.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            }

        } catch (Exception e) {
            logger.error("", e);
            return false;
        } finally {
            long endTime = System.currentTimeMillis();
            logger.info("release user redis lock use {} ms", endTime - startTime);
        }

        return RELEASE_SUCCESS.equals(result);
    }


    public static String updateUserToken(String guid, String token) {
        String tokenOld = "";
        if (0==jedisType){
            Jedis resource = jedisPool.getResource();
            tokenOld = resource.get(JedisUtil.getKey(RedisKeyConstants.TOKEN_PREFIX + guid));
            resource.close();
        }else {
            tokenOld = jedisCluster.get(JedisUtil.getKey(RedisKeyConstants.TOKEN_PREFIX + guid));
        }
        if (null==tokenOld||"".equals(tokenOld)){
            return token;
        }else {
            return tokenOld;
        }
    }


    public static void closeJedis(JedisCommands jedisCommands){
        if (0==jedisType){
            Jedis jedis = (Jedis) jedisCommands;
            jedis.close();
        }
    }

}
