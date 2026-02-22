package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.Badge;
import com.sphas.project03.mapper.BadgeMapper;
import com.sphas.project03.service.BadgeService;
import org.springframework.stereotype.Service;

@Service
public class BadgeServiceImpl extends ServiceImpl<BadgeMapper, Badge> implements BadgeService {
}