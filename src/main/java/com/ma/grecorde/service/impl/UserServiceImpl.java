package com.ma.grecorde.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ma.grecorde.entity.User;
import com.ma.grecorde.mapper.UserMapper;
import com.ma.grecorde.entity.dto.LoginBody;
import com.ma.grecorde.entity.dto.RegisterBody;
import com.ma.grecorde.service.UserService;
import com.ma.grecorde.utils.AjaxResult;
import com.ma.grecorde.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户Service实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public AjaxResult<?> register(RegisterBody registerBody) {
        try {
            // 验证参数
            Assert.notNull(registerBody.getUsername(), "用户名不能为空");
            Assert.notNull(registerBody.getPassword(), "密码不能为空");
            Assert.isTrue(registerBody.getPassword().equals(registerBody.getConfirmPassword()), "两次输入的密码不一致");
            
            // 检查用户名是否已存在
            User existingUser = userMapper.selectUserByUsername(registerBody.getUsername());
            Assert.isNull(existingUser, "用户名已存在");
            
            // 创建新用户
            User user = new User();
            user.setUsername(registerBody.getUsername());
            user.setPassword(passwordEncoder.encode(registerBody.getPassword()));
            user.setNickname(registerBody.getNickname() != null ? registerBody.getNickname() : registerBody.getUsername());
            user.setEmail(registerBody.getEmail());
            user.setPhone(registerBody.getPhone());
            user.setStatus(0); // 正常状态
            user.setIsDelete(0); // 未删除
            user.setCreateTime(new Date());
            user.setUpdateTime(new Date());
            
            // 保存用户
            userMapper.insert(user);
            
            return AjaxResult.success("注册成功");
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error("注册失败：" + e.getMessage());
        }
    }
    
    @Override
    public AjaxResult<?> login(LoginBody loginBody) {
        try {
            // 验证参数
            Assert.notNull(loginBody.getUsername(), "用户名不能为空");
            Assert.notNull(loginBody.getPassword(), "密码不能为空");
            
            // 根据用户名查询用户
            User user = userMapper.selectUserByUsername(loginBody.getUsername());
            Assert.notNull(user, "用户名或密码错误");
            
            // 验证密码
            Assert.isTrue(passwordEncoder.matches(loginBody.getPassword(), user.getPassword()), "用户名或密码错误");
            
            // 检查用户状态
            Assert.isTrue(user.getStatus() == 0, "用户已禁用");
            Assert.isTrue(user.getIsDelete() == 0, "用户已删除");
            
            // 生成JWT令牌
            String token = jwtUtil.generateToken(user.getUsername());
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);
            
            return AjaxResult.success("登录成功", result);
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error("登录失败：" + e.getMessage());
        }
    }
    
    @Override
    public User selectUserByUsername(String username) {
        return userMapper.selectUserByUsername(username);
    }
}

