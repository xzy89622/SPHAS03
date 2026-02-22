package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.UserBadge;
import com.sphas.project03.mapper.UserBadgeMapper;
import com.sphas.project03.service.UserBadgeService;
import org.springframework.stereotype.Service;

@Service
public class UserBadgeServiceImpl extends ServiceImpl<UserBadgeMapper, UserBadge> implements UserBadgeService {
}