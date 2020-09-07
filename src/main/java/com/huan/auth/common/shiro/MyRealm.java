package com.huan.auth.common.shiro;

import com.huan.auth.bean.SysUserInfo;
import com.huan.auth.service.UserInfoService;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 自定义Realm（身份校验与权限控制核心类）
 * 认证最终是交给Realm执行的
 *
 * @author swhuan
 */
public class MyRealm extends AuthorizingRealm {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 身份验证
     * 验证用户输入的账号和密码是否正确
     * 此方法在调用login会回调
     * Authentication 是用来验证用户身份
     *
     * @throws AuthenticationException
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        System.out.println("身份验证-->MyRealm.doGetAuthenticationInfo()");
        MyUsernamePasswordToken myToken = (MyUsernamePasswordToken) token;
        // 1. 获取用户输入的账号
        String userName = (String) myToken.getPrincipal();
        // 2. 通过username从数据库中查找，获取userInfo对象
        SysUserInfo sysUserInfo = userInfoService.getByUserName(userName);
        System.out.println("----->>sysUserInfo=" + sysUserInfo);
        // 判断是否有userInfo
        if (sysUserInfo == null) {
            return null;
        }
        // 3. 将库里查出的用户信息封装到SimpleAuthenticationInfo
        SimpleAuthenticationInfo simpleAuthenticationInfo = new SimpleAuthenticationInfo(
                //用户对象
                sysUserInfo,
                //密码
                sysUserInfo.getPassword(),
                //credentialsSalt=guid+salt
                ByteSource.Util.bytes(sysUserInfo.getCredentialsSalt()),
                //realm name
                getName()
        );
        return simpleAuthenticationInfo;
    }

    /**
     * 权限控制
     * 此方法在调用hasRole,hasPermission 的时候才会进行回调
     * Authorization 是授权访问控制，用于对用户进行的操作授权，证明该用户是否允许进行当前操作，如访问某个链接，某个资源文件等。
     *
     * @param principals
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {

        System.out.println("权限配置-->MyRealm.doGetAuthorizationInfo()");
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        System.out.println("权限配置-->principals.getPrimaryPrincipal()：" + principals.getPrimaryPrincipal());
        SysUserInfo sysUserInfo = (SysUserInfo) principals.getPrimaryPrincipal();
//        try {
//            for(SysRole role: sysUserInfo.getRoleList()){
//                authorizationInfo.addRole(role.getRole());
//                for(SysPermission p:role.getPermissions()){
//                    authorizationInfo.addStringPermission(p.getPermission());
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return authorizationInfo;
    }
}
