package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.SocialPost;
import com.sphas.project03.mapper.SocialPostMapper;
import com.sphas.project03.service.SocialPostService;
import org.springframework.stereotype.Service;

/**
 * 帖子Service实现
 */
@Service
public class SocialPostServiceImpl extends ServiceImpl<SocialPostMapper, SocialPost> implements SocialPostService {
}