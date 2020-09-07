package com.huan.auth.common.shiro;

import com.alibaba.fastjson.JSONObject;
import com.huan.auth.common.constant.SystemConstants;
import com.huan.auth.service.UserInfoService;
import org.apache.shiro.web.servlet.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * 自定义权限控制filter
 * @author swhuan
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Autowired
    private UserInfoService userInfoService;

    @Value("${auth.exclude.urls}")
    private String excludeUrl;
    @Value("${web.service.validate-login}")
    private Boolean validateLogin;


    @Override
    protected void doFilterInternal(ServletRequest servletRequest, ServletResponse servletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        String guid = servletRequest.getParameter("guid");
        if(null==guid||"".equals(guid)) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("-1", ""));
        }else {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(guid, ""));
        }
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (!validateLogin){
            //如果无需登录校验，直接放行
            filterChain.doFilter(servletRequest, response);
            return;
        }
        String method = request.getMethod();
        if ("get".equalsIgnoreCase(method)){
            //get请求放行
            filterChain.doFilter(servletRequest, response);
            return;
        }
        String currentURL = request.getServletPath();
        if (isExclude(excludeUrl, currentURL)) {
            filterChain.doFilter(servletRequest, response);
            return;
        }
        String token = request.getParameter("token");
        boolean loginUser = userInfoService.isLoginUser(guid, token);
        if (loginUser){
            filterChain.doFilter(servletRequest, response);
            return;
        }else {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("code", SystemConstants.CODE_408);
            jsonObject.put("msg",SystemConstants.MSG_408);
            jsonObject.put("data","");
            response.reset();
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "*");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            PrintWriter writer = null;
            try {
                writer = response.getWriter();
                writer.write(jsonObject.toJSONString());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            }
        }

    }

    /**
     * 判断是否是放行url
     * @param excludeUrl
     * @param url
     * @return
     */
    private boolean isExclude(String excludeUrl, String url) {
        String[] interceptUrls = null;
        if ((null != excludeUrl) && (excludeUrl.trim().length() > 0)) {
            interceptUrls = excludeUrl.split(",");
        }
        if (null != interceptUrls) {
            for (String s : interceptUrls) {
                if (isSameUrl(url, s)) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 对比当前url是否包含在放行url中
     * 特殊处理带参数的url eg:user/{id}/someMethod，这种url在配置放行时用*代替参数，然后对*之前的url进行比对,*后面的url一律放过
     * @param url
     * @param excludedUrl
     * @return
     */
    private boolean isSameUrl(String url, String excludedUrl) {
        if (url == null || excludedUrl == null) {
            return false;
        }
        String[] urlArr= url.split("/");
        String[] excludedUrlArr = excludedUrl.split("/");
        if (urlArr.length != excludedUrlArr.length) {
            return false;
        }
        for (int i = 0; i < excludedUrlArr.length; i++) {
            if ("*".equals(excludedUrlArr[i])){
                return true;
            }
            if(excludedUrlArr[i].startsWith("{")&&excludedUrlArr[i].endsWith("}")){
                continue;
            }
            if (!excludedUrlArr[i].equals(urlArr[i])) {
                return false;
            }
        }
        return true;
    }

}
