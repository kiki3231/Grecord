package com.ma.grecorde.filter;

import com.ma.grecorde.entity.User;
import com.ma.grecorde.service.UserService;
import com.ma.grecorde.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserService userService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 从请求头中获取token
        String token = request.getHeader("Authorization");
        
        // 验证token
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            
            // 检查token是否有效
            if (jwtUtil.validateToken(token)) {
                // 从token中获取用户名
                String username = jwtUtil.getUsernameFromToken(token);
                
                // 根据用户名查询用户
                User user = userService.selectUserByUsername(username);
                
                if (user != null) {
                    // 创建Authentication对象
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user.getUsername(), null, null);
                    
                    // 将Authentication对象存入SecurityContextHolder
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        
        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }
}