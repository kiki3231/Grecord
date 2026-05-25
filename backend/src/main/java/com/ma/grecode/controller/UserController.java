package com.ma.grecode.controller;

import com.ma.grecode.entity.User;
import com.ma.grecode.service.UserService;
import com.ma.grecode.utils.AjaxResult;
import com.ma.grecode.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Tag(name = "用户模块")
@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/pjpeg", "image/png",
            "image/gif", "image/webp", "image/bmp"
    );

    @Autowired
    private UserService userService;

    @Value("${avatar.upload.path}")
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
    public AjaxResult<?> uploadAvatar(@RequestParam(value = "avatar", required = false) MultipartFile avatarFile) {
        if (avatarFile == null) {
            return AjaxResult.error(400, "请选择头像文件");
        }
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
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                return AjaxResult.error(400, "只能上传图片类型文件");
            }
            String normalizedType = contentType.toLowerCase(Locale.ROOT);
            if (!ALLOWED_CONTENT_TYPES.contains(normalizedType)) {
                log.warn("头像 Content-Type 未在白名单: {}", contentType);
                return AjaxResult.error(400, "只支持 JPEG、PNG、GIF、WebP、BMP 格式");
            }

            if (avatarFile.getSize() > 10 * 1024 * 1024) {
                return AjaxResult.error(400, "头像文件大小不能超过10MB");
            }

            String suffix = resolveImageSuffix(avatarFile.getOriginalFilename(), normalizedType);
            String fileName = "user_" + dbUser.getId() + "_avatar" + suffix;

            Path uploadDir = Paths.get(avatarUploadPath).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            String oldFileName = extractAvatarFileName(dbUser.getAvatar());
            if (oldFileName != null) {
                Path oldPath = uploadDir.resolve(oldFileName);
                try {
                    Files.deleteIfExists(oldPath);
                } catch (IOException e) {
                    log.warn("删除旧头像失败: {}", oldPath, e);
                }
            }

            Path targetPath = uploadDir.resolve(fileName);
            avatarFile.transferTo(targetPath.toFile());

            String accessPrefix = avatarAccessPath.startsWith("/") ? avatarAccessPath : "/" + avatarAccessPath;
            String avatarUrl = accessPrefix + "/" + fileName;
            dbUser.setAvatar(avatarUrl);
            userService.updateById(dbUser);

            log.info("头像上传成功 userId={} path={}", dbUser.getId(), targetPath);
            return AjaxResult.success("头像上传成功", avatarUrl);
        } catch (IOException e) {
            log.error("头像上传 IO 失败", e);
            return AjaxResult.error(500, "头像上传失败：" + e.getMessage());
        }
    }

    private static String extractAvatarFileName(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return null;
        }
        String path = avatarUrl.split("\\?")[0].trim();
        int slash = path.lastIndexOf('/');
        if (slash < 0 || slash >= path.length() - 1) {
            return null;
        }
        return path.substring(slash + 1);
    }

    private static String resolveImageSuffix(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp)")) {
                return ext.equals(".jpeg") ? ".jpg" : ext;
            }
        }
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/bmp" -> ".bmp";
            default -> ".jpg";
        };
    }
}
