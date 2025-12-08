package com.ma.grecorde.entity.dto;

import lombok.Data;

/**
 * 注册请求参数
 */
@Data
public class RegisterBody {
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 确认密码
     */
    private String confirmPassword;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号码
     */
    private String phone;
}