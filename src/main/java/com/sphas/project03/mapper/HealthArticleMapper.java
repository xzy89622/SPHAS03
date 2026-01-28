package com.sphas.project03.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sphas.project03.entity.HealthArticle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 健康科普文章 Mapper
 */
@Mapper
public interface HealthArticleMapper extends BaseMapper<HealthArticle> {
}
