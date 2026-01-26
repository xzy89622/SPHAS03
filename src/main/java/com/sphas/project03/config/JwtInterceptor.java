package com.sphas.project03.config;

import com.sphas.project03.common.BizException;
import com.sphas.project03.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 拦截器：校验 token
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BizException("未登录或 token 缺失");
        }

        String token = auth.substring(7);
        Claims claims = JwtUtil.parseToken(token, secret);

        // 把用户信息放进 request，controller 可以直接取
        request.setAttribute("userId", claims.getSubject());
        request.setAttribute("role", claims.get("role"));

        return true; // 放行
    }
}

