package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.UserPlanCheckin;
import com.sphas.project03.mapper.UserPlanCheckinMapper;
import com.sphas.project03.service.UserPlanCheckinService;
import org.springframework.stereotype.Service;

@Service
public class UserPlanCheckinServiceImpl extends ServiceImpl<UserPlanCheckinMapper, UserPlanCheckin> implements UserPlanCheckinService {
}