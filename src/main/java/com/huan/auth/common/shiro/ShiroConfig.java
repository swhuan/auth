package com.huan.auth.common.shiro;

import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.crazycake.shiro.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.servlet.Filter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shiro 配置
 *
 * @author swhuan
 */
@Configuration
public class ShiroConfig {
    protected static final int timeout = 600000;
    //指定散列算法为md5
    private final String algorithmName = "MD5";
    //散列迭代次数
    private final int hashIterations = 2;

    @Value("${spring.redis.nodes}")
    private String host;
    @Value("${spring.redis.password}")
    private String auth;

    /**
     * ShiroFilterFactoryBean 处理拦截资源文件问题
     * Filter Chain定义说明
     * 1、一个URL可以配置多个Filter，使用逗号分隔
     * 2、当设置多个过滤器时，全部验证通过，才视为通过
     * 3、部分过滤器可指定参数，如perms，roles
     */
    @Bean
    public ShiroFilterFactoryBean shirFilter(SecurityManager securityManager) {

        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        //设置 SecurityManager
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        Map<String, Filter> mapFilter = new LinkedHashMap<>();
        //配置自定义访问拦截器
        mapFilter.put("authFilter", authFilter());
        shiroFilterFactoryBean.setFilters(mapFilter);

        //配置过滤器，注意配置顺序，不能颠倒
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
        // 配置不会被拦截的链接 顺序判断
        filterChainDefinitionMap.put("/login", "anon");
        filterChainDefinitionMap.put("/**", "anon");

        //配置shiro默认登录界面地址，前后端分离中登录界面跳转应由前端路由控制，后台仅返回json数据
        shiroFilterFactoryBean.setLoginUrl("/unauth");
        // 登录成功后要跳转的链接
        //shiroFilterFactoryBean.setSuccessUrl("/index");
        //未授权界面;
        //shiroFilterFactoryBean.setUnauthorizedUrl("/403");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        return shiroFilterFactoryBean;
    }

    /**
     *安全管理器
     * @return
     */
    @Bean
    public SecurityManager securityManager() {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        //设置realm，将myRealm注入到securityManager中
        securityManager.setRealm(myRealm());
        // 自定义session管理 使用redis
        securityManager.setSessionManager(sessionManager());
        // 自定义缓存管理 使用redis
        securityManager.setCacheManager(cacheManager());

        //注入记住我管理器;
        securityManager.setRememberMeManager(rememberMeManager());
        return securityManager;
    }

    /**
     * 注入自定义realm（账号密码校验，权限等）
     * 后面将自定义的Realm注入到SecurityManager中
     *
     * @return
     */
    @Bean
    public MyRealm myRealm() {
        MyRealm myRealm = new MyRealm();
        myRealm.setCredentialsMatcher(hashedCredentialsMatcher());
        return myRealm;
    }

    /**
     * 注入自定义sessionManager
     *
     * @return
     */
    @Bean
    public SessionManager sessionManager() {
        MySessionManager mySessionManager = new MySessionManager();
        mySessionManager.setSessionDAO(redisSessionDAO());
        return mySessionManager;
    }

    /**
     * 注入RedisSessionDAO shiro sessionDao层的实现
     * 使用的是shiro-redis开源插件
     */
    @Bean
    public RedisSessionDAO redisSessionDAO() {
        RedisSessionDAO redisSessionDAO = new RedisSessionDAO();
        redisSessionDAO.setRedisManager(redisManager());
        return redisSessionDAO;
    }

    /**
     * 配置shiro redisManager
     * 使用的是shiro-redis开源插件
     *
     * @return
     */
    public IRedisManager redisManager() {
        String[] split = host.split(",");
        if (split.length == 1) {
            //单机
            RedisManager redisManager = new RedisManager();
            redisManager.setHost(host);
            redisManager.setTimeout(timeout);
            return redisManager;
        } else if (split.length > 1) {
            //集群
            RedisClusterManager redisClusterManager = new RedisClusterManager();
            redisClusterManager.setHost(host);
            redisClusterManager.setTimeout(timeout);
            redisClusterManager.setPassword(auth);
            return redisClusterManager;
        } else {
            return null;
        }
    }


    /**
     * 注入凭证匹配器
     * 加密算法
     * （密码校验交给Shiro的SimpleAuthenticationInfo处理
     * 所以需要修改下doGetAuthenticationInfo中的代码
     * ）
     *
     * @return
     */
    @Bean
    public HashedCredentialsMatcher hashedCredentialsMatcher() {
        MyHashedCredentialsMatcher hashedCredentialsMatcher = new MyHashedCredentialsMatcher();
        //散列算法:这里使用MD5算法;
        hashedCredentialsMatcher.setHashAlgorithmName(algorithmName);
        //散列的次数，比如散列两次，相当于 md5(md5(""));
        hashedCredentialsMatcher.setHashIterations(hashIterations);

        hashedCredentialsMatcher.setStoredCredentialsHexEncoded(true);
        return hashedCredentialsMatcher;
    }




    /**
     * cacheManager 缓存 redis实现
     * 使用的是shiro-redis开源插件
     *
     * @return
     */
    @Bean(name = "redisManager")
    public RedisCacheManager cacheManager() {
        RedisCacheManager redisCacheManager = new RedisCacheManager();
        redisCacheManager.setRedisManager(redisManager());
        redisCacheManager.setPrincipalIdFieldName("guid");
        return redisCacheManager;
    }

    /**
     * 注入记住密码cookie对象
     *
     * @return
     */
    @Bean
    public SimpleCookie rememberMeCookie() {
        //这个参数是cookie的名称，对应前端的checkbox 的name = rememberMe
        SimpleCookie simpleCookie = new SimpleCookie("rememberMe");
        //记住我cookie生效时间30天 ,单位秒
        simpleCookie.setMaxAge(259200);
        return simpleCookie;
    }

    /**
     * 注入记住密码cookie管理对象
     *
     * @return
     */
    @Bean
    public CookieRememberMeManager rememberMeManager() {
        CookieRememberMeManager cookieRememberMeManager = new CookieRememberMeManager();
        cookieRememberMeManager.setCookie(rememberMeCookie());
        return cookieRememberMeManager;
    }

    /**
     * 注入自定义鉴权filter
     *
     * @return
     */
    @Bean
    public AuthFilter authFilter() {
        return new AuthFilter();
    }
}
