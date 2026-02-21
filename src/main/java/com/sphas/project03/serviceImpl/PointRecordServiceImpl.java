package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.PointRecord;
import com.sphas.project03.mapper.PointRecordMapper;
import com.sphas.project03.service.PointRecordService;
import org.springframework.stereotype.Service;

/**
 * 积分流水Service实现
 */
@Service
public class PointRecordServiceImpl extends ServiceImpl<PointRecordMapper, PointRecord> implements PointRecordService {
}
