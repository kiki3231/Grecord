package com.ma.grecode.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ma.grecode.constant.HttpStatus;
import com.ma.grecode.entity.User;
import com.ma.grecode.mapper.UserMapper;
import com.ma.grecode.entity.dto.LoginBody;
import com.ma.grecode.entity.dto.RegisterBody;
import com.ma.grecode.service.UserService;
import com.ma.grecode.utils.AjaxResult;
import com.ma.grecode.utils.JwtUtil;
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
            
            // 手机号格式验证（可选）
            if (registerBody.getPhone() != null && !registerBody.getPhone().isEmpty()) {
                // 标准中国手机号正则：1开头，第二位3-9，后面9位数字，共11位
                String phoneRegex = "^1[3-9]\\d{9}$";
                if (!registerBody.getPhone().matches(phoneRegex)) {
                    return AjaxResult.error(HttpStatus.BAD_REQUEST, "手机号格式不正确，请输入11位有效的手机号码");
                }
            }
            
            // 检查用户名是否已存在
            User existingUser = userMapper.selectUserByUsername(registerBody.getUsername());
            if (existingUser != null) {
                return AjaxResult.error(HttpStatus.CONFLICT, "注册失败：该用户名已被注册，请尝试其他用户名");
            }
            
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
            
            return AjaxResult.success("注册成功！欢迎加入我们");
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(HttpStatus.BAD_REQUEST, "注册失败：" + e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error(HttpStatus.ERROR, "注册失败：系统内部错误，请稍后重试");
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
            if (user == null) {
                return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录失败：未找到该用户名，请检查输入是否正确");
            }
            
            // 验证密码
            if (!passwordEncoder.matches(loginBody.getPassword(), user.getPassword())) {
                return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录失败：密码错误，请重新输入");
            }
            
            // 检查用户状态
            if (user.getIsDelete() != 0) {
                return AjaxResult.error(HttpStatus.FORBIDDEN, "登录失败：该账号已被删除");
            }
            
            if (user.getStatus() != 0) {
                return AjaxResult.error(HttpStatus.FORBIDDEN, "登录失败：该账号已被禁用，请联系管理员");
            }
            
            // 生成JWT令牌
            String token = jwtUtil.generateToken(user.getUsername());
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);
            
            return AjaxResult.success("登录成功", result);
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(HttpStatus.BAD_REQUEST, "登录失败：" + e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error(HttpStatus.ERROR, "登录失败：系统内部错误，请稍后重试");
        }
    }
    
    @Override
    public User selectUserByUsername(String username) {
        return userMapper.selectUserByUsername(username);
    }
}

