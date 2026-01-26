package com.sphas.project03.service;

import com.sphas.project03.controller.dto.HealthRecordAddDTO;
import com.sphas.project03.entity.HealthRecord;

import java.util.List;

public interface HealthRecordService {

    Long upsert(Long userId, HealthRecordAddDTO dto); // 新增或更新(按日期)

    List<HealthRecord> listLatest(Long userId, int limit); // 最近N条

    List<HealthRecord> listByDateRange(Long userId, String from, String to); // 按时间范围
}

