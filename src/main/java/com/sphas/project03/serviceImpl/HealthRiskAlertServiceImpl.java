package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.HealthRiskAlert;
import com.sphas.project03.mapper.HealthRiskAlertMapper;
import com.sphas.project03.service.HealthRiskAlertService;
import org.springframework.stereotype.Service;

@Service
public class HealthRiskAlertServiceImpl extends ServiceImpl<HealthRiskAlertMapper, HealthRiskAlert>
        implements HealthRiskAlertService {}
