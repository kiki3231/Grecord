package com.ma.grecode.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.ma.grecode.filter.JwtAuthenticationTokenFilter;

/**
 * Spring Security配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    /**
     * 配置密码编码器
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 配置安全规则
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
            // 禁用CSRF
            .csrf().disable()
            // 不使用Session
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // 配置请求授权规则
            .authorizeRequests()
            // 允许匿名访问的接口
            .antMatchers("/auth/login", "/auth/register").permitAll()
            // 允许访问头像资源
            .antMatchers("/avatar/**").permitAll()
            // Swagger文档
            .antMatchers("/swagger-ui.html", "/swagger-resources/**", "/v2/api-docs").permitAll()
            // 其他接口需要认证
            .anyRequest().authenticated();
        
        // 添加JWT认证过滤器
        httpSecurity.addFilterBefore(jwtAuthenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return httpSecurity.build();
    }
    
    /**
     * 创建JWT认证过滤器
     */
    @Bean
    public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter() {
        return new JwtAuthenticationTokenFilter();
    }
}