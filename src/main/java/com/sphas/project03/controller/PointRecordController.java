package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.PointRecord;
import com.sphas.project03.service.PointRecordService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 积分流水（积分变动记录）
 */
@RestController
@RequestMapping("/api/points/record")
public class PointRecordController extends BaseController {

    private final PointRecordService pointRecordService;
    private final StringRedisTemplate stringRedisTemplate;

    public PointRecordController(PointRecordService pointRecordService,
                                 StringRedisTemplate stringRedisTemplate) {
        this.pointRecordService = pointRecordService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 我的积分流水分页
     * GET /api/points/record/my/page?pageNum=1&pageSize=10
     */
    @GetMapping("/my/page")
    public R<Page<PointRecord>> myPage(@RequestParam(defaultValue = "1") long pageNum,
                                       @RequestParam(defaultValue = "10") long pageSize,
                                       HttpServletRequest request) {

        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        Page<PointRecord> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<PointRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(PointRecord::getUserId, userId);
        qw.orderByDesc(PointRecord::getCreateTime);

        return R.ok(pointRecordService.page(page, qw));
    }

    /**
     * 我的总积分（从Redis总榜取，没有就返回0）
     * GET /api/points/record/my/total
     */
    @GetMapping("/my/total")
    public R<Integer> myTotal(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return R.fail("未登录或 token 缺失");

        try {
            Double score = stringRedisTemplate.opsForZSet()
                    .score("project03:points:total", String.valueOf(userId));
            return R.ok(score == null ? 0 : score.intValue());
        } catch (Exception e) {
            return R.ok(0);
        }
    }

    /**
     * 管理端：某用户积分流水分页（用于后台排查）
     * GET /api/points/record/admin/page?userId=xx
     */
    @GetMapping("/admin/page")
    public R<Page<PointRecord>> adminPage(@RequestParam Long userId,
                                          @RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          HttpServletRequest request) {

        requireAdmin(request);
        if (userId == null) throw new BizException("userId不能为空");

        Page<PointRecord> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<PointRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(PointRecord::getUserId, userId);
        qw.orderByDesc(PointRecord::getCreateTime);

        return R.ok(pointRecordService.page(page, qw));
    }
}