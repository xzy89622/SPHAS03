package com.sphas.project03.common;

/**
 * 业务异常：用来给前端返回友好的错误提示
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message); // 保存异常信息
    }
}

