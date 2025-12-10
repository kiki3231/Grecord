package com.ma.grecode.controller;

import com.ma.grecode.entity.User;
import com.ma.grecode.service.UserService;
import com.ma.grecode.utils.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取当前登录用户信息
     * @return 用户信息
     */
    @GetMapping("/info")
    public AjaxResult<?> getUserInfo() {
        try {
            // 从SecurityContext获取当前登录用户名
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // 查询用户信息
            User user = userService.selectUserByUsername(username);
            if (user == null) {
                return AjaxResult.error(404, "用户不存在");
            }
            
            // 隐藏敏感信息
            user.setPassword(null);
            
            return AjaxResult.success("获取用户信息成功", user);
        } catch (Exception e) {
            return AjaxResult.error(500, "获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新用户信息
     * @param user 更新的用户信息
     * @return 更新结果
     */
    @PutMapping("/info")
    public AjaxResult<?> updateUserInfo(@RequestBody User user) {
        try {
            // 从SecurityContext获取当前登录用户名
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // 查询当前用户信息
            User currentUser = userService.selectUserByUsername(username);
            if (currentUser == null) {
                return AjaxResult.error(404, "用户不存在");
            }
            
            // 更新用户信息（只允许更新部分字段）
            currentUser.setNickname(user.getNickname());
            currentUser.setEmail(user.getEmail());
            currentUser.setPhone(user.getPhone());
            // 可以根据需要添加其他允许更新的字段
            
            // 保存更新
            userService.updateById(currentUser);
            
            return AjaxResult.success("更新用户信息成功");
        } catch (Exception e) {
            return AjaxResult.error(500, "更新用户信息失败: " + e.getMessage());
        }
    }
}