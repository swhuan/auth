package com.huan.auth.service.impl;

import com.huan.auth.bean.SysUserInfo;
import com.huan.auth.common.constant.RedisKeyConstants;
import com.huan.auth.common.utils.JedisUtil;
import com.huan.auth.dao.SysUserInfoRepository;
import com.huan.auth.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisCommands;


/**
 * 用户相关
 * @author swhuan
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserInfoServiceImpl implements UserInfoService {

    final private SysUserInfoRepository sysUserInfoRepository;

    /**
     * 通过手机号获取用户
     * @param phone
     * @return
     */
    @Override
    public SysUserInfo getByPhone(String phone) {
        return sysUserInfoRepository.findByPhoneAndIsDeleted(phone,0);
    }

    /**
     * 通过用户名获取用户
     * @param username
     * @return
     */
    @Override
    public SysUserInfo getByUserName(String username) {
        return sysUserInfoRepository.findByUserNameAndIsDeleted(username,0);
    }

    @Override
    public SysUserInfo getByGuid(String guid) {
        return sysUserInfoRepository.findByGuid(guid);
    }

    @Override
    public void addUserInfo(SysUserInfo sysUserInfo) {
        sysUserInfoRepository.save(sysUserInfo);
    }

    /**
     * 退出/注销登录
     *
     * @param guid
     */
    @Override
    public long logout(String guid) {
        long ret = -1;
        log.info("logout:guid={}", guid);
        JedisCommands jedis = null;
        String tokenInDb = "";
        try {
            jedis = JedisUtil.getJedisCommands();
            if (jedis != null) {
                String key = JedisUtil.getKey(RedisKeyConstants.TOKEN_PREFIX + guid);
                tokenInDb = jedis.get(key);
                if(null == tokenInDb){
                    return 1;
                }
                ret = jedis.del(key);
            }
            if (jedis != null) {
                String key = JedisUtil.getKey(RedisKeyConstants.SHRIO_PREFIX + tokenInDb);
                jedis.del(key);
            }
            if (jedis != null) {
                String key = JedisUtil.getKey(tokenInDb);
                jedis.del(key);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }finally {
            if (null!=jedis){
                JedisUtil.closeJedis(jedis);
            }
        }
        return ret;
    }

    @Override
    public String updateUserToken(String guid, String token) {
        JedisCommands jedis = null;

        try {
            token = JedisUtil.updateUserToken(guid, token);
            jedis = JedisUtil.getJedisCommands();
            if (jedis != null) {
                String key = JedisUtil.getKey(RedisKeyConstants.TOKEN_PREFIX + guid);
                jedis.setex(key, 86400 * 30, token);
                log.info("update user:{} token:{} done!", guid, token);
            }
            if (jedis != null) {
                String key = JedisUtil.getKey(RedisKeyConstants.SHRIO_PREFIX+ token);
                jedis.expire(key, 86400 * 7);
                log.info("update user:{} shrioToken:{} done!", guid, token);
            }
            if (jedis != null) {
                String key = JedisUtil.getKey(token);
                jedis.setex(key, 86400 * 7, guid);
                log.info("update user:{} token:{} done!", guid, token);
            }
            return token;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "";
        }finally {
            if (null!=jedis){
                JedisUtil.closeJedis(jedis);
            }
        }
    }


    /**
     * 判断是否为有效登录用户
     * @param guid
     * @param token
     * @return
     */
    @Override
    public boolean isLoginUser(String guid, String token) {
        boolean isLogin = false;
        String tokenInDb = null;
        if (StringUtils.isNoneBlank(guid, token)) {
            tokenInDb = queryUserToken(guid);
            if (token.equals(tokenInDb)) {
                isLogin = true;
            }
        }
        log.info("isLoginUser:guid={},token={},tokenInDb={},isLogin={},", guid, token, tokenInDb, isLogin);
        return isLogin;
    }

    /**
     * @param guid
     * @return
     */
    public String queryUserToken(String guid) {
        String token = "";
        if (StringUtils.isNoneBlank(guid)) {
            JedisCommands jedis = null;
            try {
                jedis = JedisUtil.getJedisCommands();
                if (jedis != null) {
                    String key = JedisUtil.getKey(RedisKeyConstants.TOKEN_PREFIX + guid);
                    token = jedis.get(key);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }finally {
                if (null!=jedis){
                    JedisUtil.closeJedis(jedis);
                }
            }
        }
        log.info("queryUserToken:guid={},token={}", guid, token);
        return token;
    }

    @Override
    public String getGuid(String token) {
        String guid = null;
        if (StringUtils.isNoneBlank(token)) {
            JedisCommands jedis = null;
            try {
                jedis = JedisUtil.getJedisCommands();
                if (jedis != null) {
                    guid = jedis.get(JedisUtil.getKey(token));
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }finally {
                if (null!=jedis){
                    JedisUtil.closeJedis(jedis);
                }
            }
            log.info("getGuid:guid={},token={}", guid, token);
        }
        return guid;
    }

}
