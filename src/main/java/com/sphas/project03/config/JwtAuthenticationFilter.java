package com.sphas.project03.config;

import com.sphas.project03.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 这里用 Spring Security 的过滤器把 JWT 解析进上下文
 * 这样权限这块就不只是靠 MVC 拦截器了
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 这些接口或资源直接放行
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || match(uri, "/")
                || match(uri, "/index.html")
                || match(uri, "/favicon.ico")
                || match(uri, "/assets/**")
                || match(uri, "/upload/**")
                || match(uri, "/api/auth/**")
                || match(uri, "/swagger-ui.html")
                || match(uri, "/swagger-ui/**")
                || match(uri, "/v3/api-docs/**")
                || match(uri, "/doc.html")
                || match(uri, "/ping");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");

        // 非公开接口没带 token，直接返回 401
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeUnauthorized(response, "未登录或 token 缺失");
            return;
        }

        String token = auth.substring(7);

        // 这里先做一下基础格式判断
        if (token.trim().isEmpty() || token.split("\\.").length != 3) {
            writeUnauthorized(response, "token格式不正确，请重新登录");
            return;
        }

        try {
            Claims claims = JwtUtil.parseToken(token, secret);

            String userId = claims.getSubject();
            String role = String.valueOf(claims.get("role"));

            // 兼容你原来 Controller / Interceptor 的写法
            request.setAttribute("userId", userId);
            request.setAttribute("role", role);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (role != null && !role.trim().isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            writeUnauthorized(response, "token无效或已过期，请重新登录");
        }
    }

    private boolean match(String uri, String pattern) {
        return pathMatcher.match(pattern, uri);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":401,\"msg\":\"" + message + "\",\"data\":null}");
    }
}