package com.ma.grecode.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ma.grecode.entity.User;
import com.ma.grecode.entity.dto.RegisterBody;
import com.ma.grecode.entity.dto.LoginBody;
import com.ma.grecode.utils.AjaxResult;

/**
 * 用户Service接口
 */
public interface UserService extends IService<User> {
    
    /**
     * 用户注册
     * @param registerBody 注册信息
     * @return 注册结果
     */
    AjaxResult<?> register(RegisterBody registerBody);
    
    /**
     * 用户登录
     * @param loginBody 登录信息
     * @return 登录结果，包含token
     */
    AjaxResult<?> login(LoginBody loginBody);
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User selectUserByUsername(String username);
}
