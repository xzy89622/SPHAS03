package com.sphas.project03.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sphas.project03.entity.Notice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告 Mapper
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
}
