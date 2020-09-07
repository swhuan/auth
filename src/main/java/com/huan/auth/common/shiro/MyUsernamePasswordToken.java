package com.huan.auth.common.shiro;

import org.apache.shiro.authc.UsernamePasswordToken;

/**
 * 用户信息封装类
 * @author swhuan
 */
public class MyUsernamePasswordToken extends UsernamePasswordToken {

    private String loginType;

    public MyUsernamePasswordToken() {
        super();
    }

    /**
     * 账号密码登录
     */
    public MyUsernamePasswordToken(String userName, String password, String loginType, boolean rememberMe, String host) {
        super(userName, password, rememberMe, host);
        this.loginType = loginType;
    }

    /**
     * 免密登录
     */
    public MyUsernamePasswordToken(String userName) {
        super(userName, "", false, null);
        this.loginType = LoginType.NO_PASSWORD.getCode();
    }

    public MyUsernamePasswordToken(String userName, String password) {
        super(userName, password, false, null);
        this.loginType = LoginType.PASSWORD.getCode();
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }
}