package com.ma.grecode.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置规则：前端访问 /avatar/** 时，映射到static/avatar目录
        registry.addResourceHandler("/avatar/**")
                .addResourceLocations("classpath:/static/avatar/");

        // 游戏封面：前端 <img src="/covers/sxxx.jpg"> -> classpath:/static/covers/
        registry.addResourceHandler("/covers/**")
                .addResourceLocations("classpath:/static/covers/");
    }
}