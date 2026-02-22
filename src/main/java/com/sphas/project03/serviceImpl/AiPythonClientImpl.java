package com.sphas.project03.serviceImpl;

import com.sphas.project03.service.AiPythonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Python模型调用实现
 */
@Service
public class AiPythonClientImpl implements AiPythonClient {

    private static final Logger log = LoggerFactory.getLogger(AiPythonClientImpl.class);

    private final RestTemplate restTemplate;

    @Value("${ai.python.baseUrl:http://127.0.0.1:5001}")
    private String baseUrl;

    public AiPythonClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> predict(Map<String, Object> features) {
        try {
            String url = baseUrl + "/predict";
            Object res = restTemplate.postForObject(url, features, Object.class);
            if (res instanceof Map) return (Map<String, Object>) res;
            return null;
        } catch (Exception e) {
            log.warn("Python模型不可用，走降级预测，err={}", e.getMessage());
            return null;
        }
    }
}