package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.FeedbackReply;
import com.sphas.project03.mapper.FeedbackReplyMapper;
import com.sphas.project03.service.FeedbackReplyService;
import org.springframework.stereotype.Service;

@Service
public class FeedbackReplyServiceImpl extends ServiceImpl<FeedbackReplyMapper, FeedbackReply>
        implements FeedbackReplyService {
}

