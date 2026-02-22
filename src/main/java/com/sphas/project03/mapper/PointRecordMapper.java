package com.sphas.project03.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sphas.project03.entity.PointRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 积分流水Mapper
 */
@Mapper
public interface PointRecordMapper extends BaseMapper<PointRecord> {

    /**
     * 汇总每个用户的总积分
     */
    @Select("SELECT user_id, SUM(points) AS total_points " +
            "FROM point_record GROUP BY user_id")
    List<Map<String, Object>> sumPointsGroupByUser();
}