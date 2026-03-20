package com.sphas.project03.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.controller.vo.SocialPostVO;
import com.sphas.project03.entity.SocialPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SocialPostMapper extends BaseMapper<SocialPost> {

    /**
     * 社区帖子分页
     * 这里先不查 u.avatar 了
     * 因为当前 sys_user 表里没有这个字段，会直接把列表查挂
     */
    @Select({
            "<script>",
            "SELECT ",
            "  p.id, p.user_id AS userId, p.content, p.images_json AS imagesJson, p.status, p.create_time AS createTime,",
            "  u.nickname AS nickname, '' AS avatar,",
            "  (SELECT COUNT(1) FROM social_like sl WHERE sl.post_id = p.id) AS likeCount,",
            "  (SELECT COUNT(1) FROM social_comment sc WHERE sc.post_id = p.id) AS commentCount,",
            "  CASE WHEN #{userId} IS NULL THEN 0",
            "       WHEN EXISTS(SELECT 1 FROM social_like sl2 WHERE sl2.post_id = p.id AND sl2.user_id = #{userId}) THEN 1",
            "       ELSE 0 END AS likedByMe",
            "FROM social_post p ",
            "LEFT JOIN sys_user u ON u.id = p.user_id ",
            "WHERE p.status = 1 AND p.deleted_flag = 0 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (p.content LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%')) ",
            "</if>",
            "ORDER BY p.create_time DESC",
            "</script>"
    })
    IPage<SocialPostVO> selectPostPageV2(Page<SocialPostVO> page,
                                         @Param("userId") Long userId,
                                         @Param("keyword") String keyword);

}