package com.sphas.project03.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sphas.project03.entity.SocialLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞Mapper
 */
@Mapper
public interface SocialLikeMapper extends BaseMapper<SocialLike> {
}