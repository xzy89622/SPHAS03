package com.sphas.project03.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sphas.project03.entity.WeeklyReportRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报告表 Mapper
 */
@Mapper
public interface WeeklyReportMapper extends BaseMapper<WeeklyReportRecord> {
}