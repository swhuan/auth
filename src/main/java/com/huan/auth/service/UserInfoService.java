package com.huan.auth.service;

import com.huan.auth.bean.SysUserInfo;

/**
 * 用户相关
 * @author swhuan
 */
public interface UserInfoService {

    /**
     * 通过手机号获取用户
     * @param phone
     * @return
     */
    SysUserInfo getByPhone(String phone);

    /**
     * 通过用户名获取用户
     * @param username
     * @return
     */
    SysUserInfo getByUserName(String username);

    /**
     * 通过guid获取用户
     * @param guid
     * @return
     */
    SysUserInfo getByGuid(String guid);

    /**
     * 保存用户信息
     * @param sysUserInfo
     */
    void addUserInfo(SysUserInfo sysUserInfo);

    /**
     * 判断是否为有效的登录用户
     * @param guid
     * @param token
     * @return
     */
    boolean isLoginUser(String guid, String token);

    /**
     * 退出/注销登录
     */
    long logout(String guid);

    /**
     * 重置 token 失效时间
     * @param guid
     * @param token
     */
    String updateUserToken(String guid, String token);



    /**
     * 根据token得到guid;
     *
     * @param token
     * @return
     */
    String getGuid(String token);

}
