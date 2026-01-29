package com.sphas.project03.controller;

import com.sphas.project03.common.BizException;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller 基类：统一取 userId/role，并提供 requireAdmin 校验
 */
public abstract class BaseController {

    protected Long getUserId(HttpServletRequest request) {
        Object v = request.getAttribute("userId");
        if (v == null) return null;
        return Long.valueOf(v.toString());
    }

    protected String getRole(HttpServletRequest request) {
        Object v = request.getAttribute("role");
        return v == null ? null : v.toString();
    }

    /**
     * 只允许管理员访问（role=ADMIN）
     */
    protected void requireAdmin(HttpServletRequest request) {
        String role = getRole(request);
        if (!"ADMIN".equals(role)) {
            throw new BizException("无权限：仅管理员可操作");
        }
    }
}
