package com.huan.auth.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查
 * @author swhuan
 */
@RestController
@RequestMapping("/check")
public class CheckHealthController {

    /**
     * 健康检查
     * @return
     */
    @GetMapping("/checkHealth")
    public  String  checkHealth(){
        return "ok";
    }

}
