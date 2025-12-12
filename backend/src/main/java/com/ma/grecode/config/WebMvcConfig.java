package com.ma.grecode.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // 从配置文件读取头像上传路径（项目内的static/avatar目录）
    @Value("${avatar.upload.path}")
    private String uploadPath;

    // 从配置文件读取头像访问前缀
    @Value("${avatar.access.path}")
    private String accessPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置规则：前端访问 accessPath/** 时，映射到 uploadPath 目录
        // 注意：因为你的文件存在项目内的static目录下，这里可以用 classpath: 开头
        registry.addResourceHandler(accessPath + "/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
