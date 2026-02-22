package com.sphas.project03.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.controller.vo.SocialCommentVO;
import com.sphas.project03.entity.SocialComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SocialCommentMapper extends BaseMapper<SocialComment> {

    /**
     * 评论分页（联表：social_comment + sys_user）
     */
    @Select({
            "<script>",
            "SELECT ",
            "  c.id, c.post_id AS postId, c.user_id AS userId, c.content, c.create_time AS createTime,",
            "  u.nickname AS nickname, u.avatar AS avatar ",
            "FROM social_comment c ",
            "LEFT JOIN sys_user u ON u.id = c.user_id ",
            "WHERE c.post_id = #{postId} ",
            "ORDER BY c.create_time ASC",
            "</script>"
    })
    IPage<SocialCommentVO> selectCommentPageV2(Page<SocialCommentVO> page,
                                               @Param("postId") Long postId);
}