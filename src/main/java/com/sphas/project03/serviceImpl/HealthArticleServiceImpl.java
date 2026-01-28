package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.HealthArticle;
import com.sphas.project03.mapper.HealthArticleMapper;
import com.sphas.project03.service.HealthArticleService;
import org.springframework.stereotype.Service;

/**
 * 健康科普文章 Service 实现
 */
@Service
public class HealthArticleServiceImpl
        extends ServiceImpl<HealthArticleMapper, HealthArticle>
        implements HealthArticleService {
}

