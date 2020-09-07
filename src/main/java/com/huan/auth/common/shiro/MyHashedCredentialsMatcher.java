package com.huan.auth.common.shiro;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;

/**
 * 密码匹配器
 * @author swhuan
 */
public class MyHashedCredentialsMatcher extends HashedCredentialsMatcher {


    /**
     * 重写验证方法，实现无秘登录
     * @param token
     * @param info
     * @return
     */
    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        MyUsernamePasswordToken mupt = (MyUsernamePasswordToken)token;
        if (mupt.getLoginType().equals(LoginType.NO_PASSWORD.getCode())) {
            return true;
        }
        if (mupt.getLoginType().equals(LoginType.PASSWORD.getCode())) {
            return super.doCredentialsMatch(token, info);
        } else {
            return false;
        }
    }
}
