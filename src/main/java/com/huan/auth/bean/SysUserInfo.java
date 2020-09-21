package com.huan.auth.bean;

import lombok.Data;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * 用户表实体
 * @author swhuan
 */
@Entity
@Data
public class SysUserInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 用户id
     */
    @Id
    private String guid;

    /**
     * 用户名（唯一）
     */
    private String userName;

    /**
     * 手机号（唯一）
     */
    private String phone = "";
    /**
     * 密码
     */
    private String password;
    /**
     * 密码盐
     */
    private String salt;

    /**
     * 注册时间
     */
    private Long createTime;


    /**
     * 邮箱
     */
    private String email = "";

    /**
     * 是否删除
     */
    private int isDeleted = 0;

    /**
     * 验证的密码盐
     * @return
     */
    public String getCredentialsSalt(){
        return this.userName+this.salt;
    }

}
