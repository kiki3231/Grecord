package com.ma.grecode.controller;

import com.ma.grecode.entity.dto.LoginBody;
import com.ma.grecode.entity.dto.RegisterBody;
import com.ma.grecode.service.UserService;
import com.ma.grecode.utils.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 用户注册
     * @param registerBody 注册信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public AjaxResult<?> register(@RequestBody RegisterBody registerBody) {
        return userService.register(registerBody);
    }
    
    /**
     * 用户登录
     * @param loginBody 登录信息
     * @return 登录结果，包含token
     */
    @PostMapping("/login")
    public AjaxResult<?> login(@RequestBody LoginBody loginBody) {
        return userService.login(loginBody);
    }
}