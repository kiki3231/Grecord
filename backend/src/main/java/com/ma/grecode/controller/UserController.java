package com.ma.grecode.controller;

import com.ma.grecode.entity.User;
import com.ma.grecode.service.UserService;
import com.ma.grecode.utils.AjaxResult;
import com.ma.grecode.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Tag(name = "用户模块")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Value("${avatar.upload.path:/upload/avatar}")
    private String avatarUploadPath;

    @Value("${avatar.access.path:/avatar}")
    private String avatarAccessPath;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public AjaxResult<?> getUserInfo() {
        User user = SecurityUtils.getCurrentUser();
        User freshUser = userService.selectUserByUsername(user.getUsername());
        if (freshUser == null) {
            return AjaxResult.error(404, "用户不存在");
        }
        freshUser.setPassword(null);
        if (freshUser.getAvatar() != null && freshUser.getAvatar().length() < 5) {
            freshUser.setAvatar("");
        }
        return AjaxResult.success("获取用户信息成功", freshUser);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/info")
    public AjaxResult<?> updateUserInfo(@RequestBody User user) {
        User currentUser = SecurityUtils.getCurrentUser();
        User dbUser = userService.selectUserByUsername(currentUser.getUsername());
        if (dbUser == null) {
            return AjaxResult.error(404, "用户不存在");
        }
        dbUser.setNickname(user.getNickname());
        dbUser.setEmail(user.getEmail());
        dbUser.setPhone(user.getPhone());
        userService.updateById(dbUser);
        return AjaxResult.success("更新用户信息成功");
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public AjaxResult<?> uploadAvatar(@RequestParam("avatar") MultipartFile avatarFile) {
        try {
            User currentUser = SecurityUtils.getCurrentUser();
            User dbUser = userService.selectUserByUsername(currentUser.getUsername());
            if (dbUser == null) {
                return AjaxResult.error(404, "用户不存在");
            }

            if (avatarFile.isEmpty()) {
                return AjaxResult.error(400, "头像文件不能为空");
            }

            String contentType = avatarFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return AjaxResult.error(400, "只能上传图片类型文件");
            }

            List<String> allowedContentTypes = Arrays.asList(
                "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
            );
            if (!allowedContentTypes.contains(contentType)) {
                return AjaxResult.error(400, "只支持JPEG、PNG、GIF、WebP、BMP格式");
            }

            if (avatarFile.getSize() > 10 * 1024 * 1024) {
                return AjaxResult.error(400, "头像文件大小不能超过10MB");
            }

            String originalFilename = avatarFile.getOriginalFilename();
            String suffix = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String fileName = "user_" + dbUser.getId() + "_avatar" + suffix;

            File uploadDir = new File(avatarUploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            if (dbUser.getAvatar() != null && !dbUser.getAvatar().isEmpty()) {
                String oldFileName = dbUser.getAvatar().substring(dbUser.getAvatar().lastIndexOf("/") + 1);
                File oldFile = new File(uploadDir, oldFileName);
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }

            avatarFile.transferTo(new File(uploadDir, fileName));

            String avatarUrl = avatarAccessPath + "/" + fileName;
            dbUser.setAvatar(avatarUrl);
            userService.updateById(dbUser);

            return AjaxResult.success("头像上传成功", avatarUrl);
        } catch (IOException e) {
            return AjaxResult.error(500, "头像上传失败：" + e.getMessage());
        }
    }
}
