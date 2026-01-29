package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.DietPlan;
import com.sphas.project03.mapper.DietPlanMapper;
import com.sphas.project03.service.DietPlanService;
import org.springframework.stereotype.Service;

@Service
public class DietPlanServiceImpl extends ServiceImpl<DietPlanMapper, DietPlan> implements DietPlanService {}

