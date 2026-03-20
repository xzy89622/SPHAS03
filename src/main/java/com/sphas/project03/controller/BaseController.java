package com.sphas.project03.controller;

import com.sphas.project03.common.BizException;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller 基类：统一取 userId/role，并提供角色校验
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
     * 只允许管理员访问
     */
    protected void requireAdmin(HttpServletRequest request) {
        String role = getRole(request);
        if (!"ADMIN".equals(role)) {
            throw new BizException("无权限：仅管理员可操作");
        }
    }

    /**
     * 只允许 AI 健康顾问访问
     */
    protected void requireAiAdvisor(HttpServletRequest request) {
        String role = getRole(request);
        if (!"AI_ADVISOR".equals(role)) {
            throw new BizException("无权限：仅AI健康顾问可操作");
        }
    }

    /**
     * 管理员和 AI 健康顾问都能访问
     * 这里主要是方便管理员联调顾问接口
     */
    protected void requireAdminOrAdvisor(HttpServletRequest request) {
        String role = getRole(request);
        if (!"ADMIN".equals(role) && !"AI_ADVISOR".equals(role)) {
            throw new BizException("无权限：仅管理员或AI健康顾问可操作");
        }
    }
}