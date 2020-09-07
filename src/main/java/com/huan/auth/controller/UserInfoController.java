package com.huan.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.huan.auth.bean.SysUserInfo;
import com.huan.auth.common.constant.SystemConstants;
import com.huan.auth.common.shiro.MyUsernamePasswordToken;
import com.huan.auth.common.utils.ResultEntity;
import com.huan.auth.common.utils.StringUtils;
import com.huan.auth.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户相关
 *
 * @author swhuan
 */
@RestController
@Slf4j
@RequestMapping("/user")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserInfoController {
    final private UserInfoService userInfoService;
    public static String[] chars = new String[] { "a", "b", "c", "d", "e", "f",
            "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s",
            "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
            "W", "X", "Y", "Z" };
    /**
     * 随机数生成器
     */
    private static RandomNumberGenerator randomNumberGenerator = new SecureRandomNumberGenerator();

    /**
     * 指定散列算法为md5
     */
    private String algorithmName = "MD5";

    /**
     * 散列迭代次数
     */
    private final int hashIterations = 2;

    /**
     * 注册
     *
     * @param sysUserInfo
     * @return
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public Object register(@RequestBody @Validated SysUserInfo sysUserInfo) {
        try {
            //验证验证码 （暂且省略）
            SysUserInfo sysUserInCheckName = userInfoService.getByUserName(sysUserInfo.getUserName());
            if (sysUserInCheckName != null) {
                return ResponseEntity.ok().body(new ResultEntity(ResultEntity.ResultStatus.FAILURE, "", "用户名重复,请修改用户名"));
            }
            SysUserInfo sysUserInCheckPhone = userInfoService.getByPhone(sysUserInfo.getPhone());
            if (sysUserInCheckPhone != null) {
                return ResponseEntity.ok().body(new ResultEntity(ResultEntity.ResultStatus.FAILURE, "", "手机号已存在,请更换手机号"));
            }
            //生成guid
            sysUserInfo.setGuid(StringUtils.getUUID());
            //生成salt
            log.info("register==>nickName={},phone={}", sysUserInfo.getUserName(), sysUserInfo.getPhone());
            sysUserInfo.setSalt(randomNumberGenerator.nextBytes().toHex());
            //生成加密密码
            String newPassword =
                    new SimpleHash(algorithmName, sysUserInfo.getPassword(),
                            ByteSource.Util.bytes(sysUserInfo.getCredentialsSalt()), hashIterations).toHex();
            sysUserInfo.setPassword(newPassword);
            sysUserInfo.setCreateTime(System.currentTimeMillis());
            sysUserInfo.setIsDeleted(0);
            userInfoService.addUserInfo(sysUserInfo);
            return ResponseEntity.ok().body(new ResultEntity(ResultEntity.ResultStatus.OK, "", "注册成功"));
        } catch (Exception e) {
            log.error("register is failed", e);
            return ResponseEntity.ok().body(new ResultEntity(ResultEntity.ResultStatus.FAILURE, "", "注册失败"));
        }
    }

    /**
     * 账号（用户名或手机号）密码登录
     * @param userMsg
     * @return
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public JSONObject login(@RequestBody JSONObject userMsg) {
        JSONObject jsonObject = new JSONObject();
        JSONObject data = new JSONObject();
        Subject subject = SecurityUtils.getSubject();
        try {
            String account = userMsg.getString("account");
            String password = userMsg.getString("password");
            SysUserInfo sysUserInfo;
            //先查手机号,有的话就手机号密码登陆
            sysUserInfo = userInfoService.getByPhone(account);
            if (null == sysUserInfo) {
                //用用户名查找
                sysUserInfo = userInfoService.getByUserName(account);
            }
            if (null == sysUserInfo) {
                jsonObject.put("code", SystemConstants.CODE_FAILURE);
                jsonObject.put("msg", "该用户不存在");
                data.put("type", "username");
                jsonObject.put("data", data);
                log.info(jsonObject.toString());
                return jsonObject;
            }
            MyUsernamePasswordToken token = new MyUsernamePasswordToken(sysUserInfo.getUserName(), password);
            subject.login(token);
            //每次登录更新token
            String sessionId = (String) subject.getSession().getId();
            String token1 = userInfoService.updateUserToken(sysUserInfo.getGuid(), sessionId);
            JSONObject userInfo = JSON.parseObject(JSON.toJSONString(sysUserInfo));
            userInfo.put("token",token1);
            userInfo.remove("salt");
            userInfo.remove("password");
            userInfo.remove("credentialsSalt");

            jsonObject.put("code", SystemConstants.CODE_SUCCESS);
            jsonObject.put("msg", "登录成功");
            jsonObject.put("data", userInfo);

        } catch (IncorrectCredentialsException e) {
            jsonObject.put("code", SystemConstants.CODE_FAILURE);
            jsonObject.put("msg", "密码错误");
            data.put("type", "password");
            jsonObject.put("data", data);
            log.error(jsonObject.toString(),e);
        } catch (LockedAccountException e) {
            jsonObject.put("code", SystemConstants.CODE_FAILURE);
            jsonObject.put("msg", "登录失败，该用户已被冻结");
            data.put("type", "username");
            jsonObject.put("data", data);
            log.error(jsonObject.toString(),e);
        } catch (AuthenticationException e) {
            jsonObject.put("code", SystemConstants.CODE_FAILURE);
            jsonObject.put("msg", "该用户不存在");
            data.put("type", "username");
            jsonObject.put("data", data);
            log.error(jsonObject.toString(),e);
        } catch (Exception e) {
            jsonObject.put("code", SystemConstants.CODE_ERROR);
            jsonObject.put("msg", "登陆失败");
            jsonObject.put("data", data);
            log.error("login is failed", e);
        }
        log.info(jsonObject.toString());
        return jsonObject;
    }


    /**
     * 验证guid,token是否有效
     * @param guid
     * @param token
     * @return
     */
    @RequestMapping(value = "/validateGuidAndToken", method = {RequestMethod.GET, RequestMethod.POST})
    public JSONObject validateGuidAndToken(String guid, String token) {
        log.info("validateGuidAndToken guid:{},token:{}", guid, token);
        JSONObject json = new JSONObject();
        JSONObject data = new JSONObject();
        int code = SystemConstants.CODE_SUCCESS;
        String msg = SystemConstants.MSG_200;
        boolean valid = false;
        if (org.apache.commons.lang3.StringUtils.isNoneBlank(guid, token)) {
            SysUserInfo byGuid = userInfoService.getByGuid(guid);
            if (null==byGuid) {
                code = SystemConstants.CODE_409;
                msg = SystemConstants.MSG_409;
            } else {
                valid = userInfoService.isLoginUser(guid, token);
                log.info("check guid({}) token({}) valid info result:{}", guid, token, valid);
                if (!valid) {
                    code = SystemConstants.CODE_408;
                    msg = SystemConstants.MSG_408;
                } else {
                    //重置token失效时间
                    userInfoService.updateUserToken(guid, token);
                }
            }
        } else {
            code = SystemConstants.CODE_407;
            msg = "guid,token不能为空";
        }
        json.put("code", code);
        json.put("msg", msg);
        data.put("valid", valid);
        data.put("guid", guid);
        json.put("data", data);
        log.info("RETURN:{}", json.toString());
        return json;
    }

    /**
     * 注销
     *
     * @return
     */
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    @ResponseBody
    public JSONObject logout(@RequestBody Map<String, Object> map) {
        String guid = (String) map.get("guid");
        String token = (String) map.get("token");
        log.info("logout==>guid={},token={}", guid, token);
        JSONObject retJson = new JSONObject();
        if (org.apache.commons.lang3.StringUtils.isNoneBlank(guid, token)) {

            //验证用户是否登陆
            boolean loginUser = userInfoService.isLoginUser(guid, token);
            //未登录,直接返回退出成功
            if (!loginUser){
                retJson.put("code", SystemConstants.CODE_SUCCESS);
                retJson.put("msg", SystemConstants.MSG_200);
                return retJson;
            }
            Subject subject = SecurityUtils.getSubject();
            subject.logout();
            long result = userInfoService.logout(guid);
            if (result > 0) {
                retJson.put("code", SystemConstants.CODE_SUCCESS);
                retJson.put("msg", SystemConstants.MSG_200);
                retJson.put("data", new JSONObject());
            } else {
                retJson.put("code", SystemConstants.CODE_FAILURE);
                retJson.put("msg", "guid与token不匹配！");
                retJson.put("data", new JSONObject());
            }
        } else {
            retJson.put("key", SystemConstants.CODE_FAILURE);
            retJson.put("msg", "guid,token不能为空！");
            retJson.put("data", new JSONObject());
        }
        return retJson;
    }

    /**
     * 修改密码
     *
     * @return
     */
    @RequestMapping(value = "/updatePassword", method = RequestMethod.POST)
    @ResponseBody
    public Object updatePassword(@RequestBody SysUserInfo sysUserInfo) {
        log.info("updatePassword==>phone={}", sysUserInfo.getPhone());
        SysUserInfo userInfo = userInfoService.getByPhone(sysUserInfo.getPhone());
        if (userInfo == null) {
            return ResponseEntity.ok().body(new ResultEntity(ResultEntity.ResultStatus.FAILURE, "", "用户不存在"));
        }
        //生成salt
        userInfo.setSalt(randomNumberGenerator.nextBytes().toHex());
        //生成加密密码
        String newPassword =
                new SimpleHash(algorithmName, sysUserInfo.getPassword(),
                        ByteSource.Util.bytes(userInfo.getCredentialsSalt()), hashIterations).toHex();
        userInfo.setPassword(newPassword);
        userInfoService.addUserInfo(userInfo);
        return ResponseEntity.ok().body(new ResultEntity(ResultEntity.ResultStatus.OK, "", "密码更新成功"));
    }


}
