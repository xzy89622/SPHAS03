package com.sphas.project03.service;

import java.util.Map;

/**
 * Python模型客户端
 */
public interface AiPythonClient {

    /**
     * 调用Python预测接口，返回Map；失败返回null
     */
    Map<String, Object> predict(Map<String, Object> features);
}