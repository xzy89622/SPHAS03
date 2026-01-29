package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.SportPlan;
import com.sphas.project03.mapper.SportPlanMapper;
import com.sphas.project03.service.SportPlanService;
import org.springframework.stereotype.Service;

@Service
public class SportPlanServiceImpl extends ServiceImpl<SportPlanMapper, SportPlan> implements SportPlanService {}

