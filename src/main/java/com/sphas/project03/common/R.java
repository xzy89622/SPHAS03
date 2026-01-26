package com.sphas.project03.common;

/**
 * 统一返回结果
 * @param <T> 返回数据类型
 */
public class R<T> {

    private int code;    // 状态码：0成功，1失败
    private String msg;  // 提示信息
    private T data;      // 数据

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 0;
        r.msg = "ok";
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.code = 1;
        r.msg = msg;
        r.data = null;
        return r;
    }

    // ====== getter / setter ======

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

