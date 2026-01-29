package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.Notice;
import com.sphas.project03.mapper.NoticeMapper;
import com.sphas.project03.service.NoticeService;
import org.springframework.stereotype.Service;

/**
 * 公告 ServiceImpl
 */
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {
}
