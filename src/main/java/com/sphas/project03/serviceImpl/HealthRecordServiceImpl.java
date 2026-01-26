package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.controller.dto.HealthRecordAddDTO;
import com.sphas.project03.entity.HealthRecord;
import com.sphas.project03.mapper.HealthRecordMapper;
import com.sphas.project03.service.HealthRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 健康记录业务
 */
@Service
public class HealthRecordServiceImpl implements HealthRecordService {

    private final HealthRecordMapper healthRecordMapper;

    public HealthRecordServiceImpl(HealthRecordMapper healthRecordMapper) {
        this.healthRecordMapper = healthRecordMapper;
    }

    @Override
    public Long upsert(Long userId, HealthRecordAddDTO dto) {
        LocalDate date = LocalDate.parse(dto.getRecordDate()); // 解析日期

        // 查当天是否已有记录
        HealthRecord exist = healthRecordMapper.selectOne(
                new LambdaQueryWrapper<HealthRecord>()
                        .eq(HealthRecord::getUserId, userId)
                        .eq(HealthRecord::getRecordDate, date)
        );

        if (exist == null) {
            // 新增
            HealthRecord r = new HealthRecord();
            r.setUserId(userId);
            r.setRecordDate(date);
            fill(r, dto);
            healthRecordMapper.insert(r);
            return r.getId();
        } else {
            // 更新
            fill(exist, dto);
            healthRecordMapper.updateById(exist);
            return exist.getId();
        }
    }

    @Override
    public List<HealthRecord> listLatest(Long userId, int limit) {
        if (limit <= 0 || limit > 100) {
            throw new BizException("limit范围建议 1~100");
        }
        return healthRecordMapper.selectList(
                new LambdaQueryWrapper<HealthRecord>()
                        .eq(HealthRecord::getUserId, userId)
                        .orderByDesc(HealthRecord::getRecordDate)
                        .last("LIMIT " + limit) // 简单限制条数
        );
    }

    @Override
    public List<HealthRecord> listByDateRange(Long userId, String from, String to) {
        LocalDate fromD = LocalDate.parse(from);
        LocalDate toD = LocalDate.parse(to);

        return healthRecordMapper.selectList(
                new LambdaQueryWrapper<HealthRecord>()
                        .eq(HealthRecord::getUserId, userId)
                        .ge(HealthRecord::getRecordDate, fromD)
                        .le(HealthRecord::getRecordDate, toD)
                        .orderByAsc(HealthRecord::getRecordDate)
        );
    }

    private void fill(HealthRecord r, HealthRecordAddDTO dto) {
        // 把 DTO 的字段写入实体
        r.setHeightCm(dto.getHeightCm());
        r.setWeightKg(dto.getWeightKg());
        r.setSystolic(dto.getSystolic());
        r.setDiastolic(dto.getDiastolic());
        r.setHeartRate(dto.getHeartRate());
        r.setSteps(dto.getSteps());
        r.setSleepHours(dto.getSleepHours());
        r.setRemark(dto.getRemark());
    }
}

