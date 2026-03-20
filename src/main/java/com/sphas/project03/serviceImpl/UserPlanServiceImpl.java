package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.UserPlan;
import com.sphas.project03.mapper.UserPlanMapper;
import com.sphas.project03.service.UserPlanService;
import org.springframework.stereotype.Service;

@Service
public class UserPlanServiceImpl extends ServiceImpl<UserPlanMapper, UserPlan> implements UserPlanService {
}