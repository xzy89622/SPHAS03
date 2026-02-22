package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.SocialComment;
import com.sphas.project03.mapper.SocialCommentMapper;
import com.sphas.project03.service.SocialCommentService;
import org.springframework.stereotype.Service;

/**
 * 评论Service实现
 */
@Service
public class SocialCommentServiceImpl extends ServiceImpl<SocialCommentMapper, SocialComment> implements SocialCommentService {
}