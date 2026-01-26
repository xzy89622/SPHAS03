package com.sphas.project03.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：把异常转成统一返回
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        return R.fail(e.getMessage()); // 业务异常直接返回原因
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleAny(Exception e) {
        e.printStackTrace(); // 控制台打印真实原因
        return R.fail("系统繁忙，请稍后再试");
    }
}
