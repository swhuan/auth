package com.huan.auth.dao;

import com.huan.auth.bean.SysUserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * 用户信息dao
 * @author suhuan
 */
@Repository
public interface SysUserInfoRepository extends JpaRepository<SysUserInfo,String> {


    /**
     * 根据手机号查询用户
     * @param phone
     * @param isDeleted
     * @return
     */
    SysUserInfo findByPhoneAndIsDeleted(String phone, Integer isDeleted);

    /**
     * 根据用户名
     * @param userName
     * @param isDeleted
     * @return
     */
    SysUserInfo findByUserNameAndIsDeleted(String userName,Integer isDeleted);

    /**
     * 根据用户id查用户
     * @param guid
     * @return
     */
    SysUserInfo findByGuid(String guid);

}
