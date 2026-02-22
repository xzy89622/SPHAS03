package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.SocialLike;
import com.sphas.project03.mapper.SocialLikeMapper;
import com.sphas.project03.service.SocialLikeService;
import org.springframework.stereotype.Service;

/**
 * 点赞Service实现
 */
@Service
public class SocialLikeServiceImpl extends ServiceImpl<SocialLikeMapper, SocialLike> implements SocialLikeService {
}