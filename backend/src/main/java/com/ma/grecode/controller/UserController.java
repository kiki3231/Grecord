package com.ma.grecode.controller;

import com.ma.grecode.entity.User;
import com.ma.grecode.service.UserService;
import com.ma.grecode.utils.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    // 头像上传路径
    @Value("${avatar.upload.path:/upload/avatar}")
    private String avatarUploadPath;
    
    // 头像访问路径
    @Value("${avatar.access.path:/avatar}")
    private String avatarAccessPath;
    
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
    
    /**
     * 上传头像
     * @param avatarFile 头像文件
     * @return 上传结果
     */
    @PostMapping("/avatar")
    public AjaxResult<?> uploadAvatar(@RequestParam("avatar") MultipartFile avatarFile) {
        try {
            // 从SecurityContext获取当前登录用户名
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // 查询当前用户信息
            User currentUser = userService.selectUserByUsername(username);
            if (currentUser == null) {
                return AjaxResult.error(404, "用户不存在");
            }
            
            // 验证文件是否为空
            if (avatarFile.isEmpty()) {
                return AjaxResult.error(400, "上传失败：头像文件不能为空");
            }
            
            // 验证文件类型
            String contentType = avatarFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return AjaxResult.error(400, "上传失败：只能上传图片类型文件");
            }
            
            // 验证文件大小（限制10MB）
            long maxSize = 10 * 1024 * 1024;
            if (avatarFile.getSize() > maxSize) {
                return AjaxResult.error(400, "上传失败：头像文件大小不能超过10MB");
            }
            
            // 生成唯一文件名
            String originalFilename = avatarFile.getOriginalFilename();
            String suffix = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String fileName = UUID.randomUUID().toString() + suffix;
            
            // 创建上传目录
            File uploadDir = new File(avatarUploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // 保存文件
            File destFile = new File(uploadDir, fileName);
            avatarFile.transferTo(destFile);
            
            // 更新用户头像
            String avatarUrl = avatarAccessPath + "/" + fileName;
            currentUser.setAvatar(avatarUrl);
            userService.updateById(currentUser);
            
            // 返回上传结果
            return AjaxResult.success("头像上传成功", avatarUrl);
        } catch (IOException e) {
            return AjaxResult.error(500, "头像上传失败：文件保存异常 - " + e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error(500, "头像上传失败: " + e.getMessage());
        }
    }
}