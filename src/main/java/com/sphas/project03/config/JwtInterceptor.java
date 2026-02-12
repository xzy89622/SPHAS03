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

        // ✅ 0) 放行预检 OPTIONS（浏览器可能会先发这个）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // ✅ 1) 放行登录/注册接口（否则永远拿不到 token）
        String uri = request.getRequestURI(); // 例如 /api/auth/login
        if (uri != null && uri.startsWith("/api/auth/")) {
            return true;
        }

        // ✅ 2) 其他接口才校验 token
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BizException("未登录或 token 缺失");
        }

        String token = auth.substring(7);
        Claims claims = JwtUtil.parseToken(token, secret);

        request.setAttribute("userId", claims.getSubject());
        request.setAttribute("role", claims.get("role"));

        return true;
    }

}

