package com.sphas.project03.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sphas.project03.entity.HealthRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 健康记录Mapper
 */
@Mapper
public interface HealthRecordMapper extends BaseMapper<HealthRecord> {
}
