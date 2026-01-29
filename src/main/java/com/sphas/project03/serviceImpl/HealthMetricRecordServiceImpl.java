package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.HealthMetricRecord;
import com.sphas.project03.mapper.HealthMetricRecordMapper;
import com.sphas.project03.service.HealthMetricRecordService;
import org.springframework.stereotype.Service;

@Service
public class HealthMetricRecordServiceImpl
        extends ServiceImpl<HealthMetricRecordMapper, HealthMetricRecord>
        implements HealthMetricRecordService {
}

