package com.ma.grecode.entity.dto;

import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginBody {
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
}