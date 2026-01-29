package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.BmiStandard;
import com.sphas.project03.mapper.BmiStandardMapper;
import com.sphas.project03.service.BmiStandardService;
import org.springframework.stereotype.Service;

/**
 * BMI 标准 ServiceImpl
 */
@Service
public class BmiStandardServiceImpl extends ServiceImpl<BmiStandardMapper, BmiStandard>
        implements BmiStandardService {
}
