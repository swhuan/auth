package com.huan.auth.common.shiro;

/**
 * 登录类型
 * @author swhuan
 */
public enum LoginType {
    /**
     * 密码登录
     */
    PASSWORD("password"),
    /**
     * 免密登录
     */
    NO_PASSWORD("no_password");

    /**
     * 状态值
     */
    private String code;

    private LoginType(String code) {
        this.code = code;
    }
    public String getCode () {
        return code;
    }
}