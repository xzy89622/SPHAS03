package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.Badge;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.entity.UserBadge;
import com.sphas.project03.mapper.BadgeMapper;
import com.sphas.project03.mapper.UserBadgeMapper;
import com.sphas.project03.service.SysUserService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端：勋章管理 / 用户勋章查看
 */
@RestController
@RequestMapping("/api/badge/admin")
public class BadgeAdminController extends BaseController {

    private final BadgeMapper badgeMapper;
    private final UserBadgeMapper userBadgeMapper;
    private final SysUserService sysUserService;

    public BadgeAdminController(BadgeMapper badgeMapper,
                                UserBadgeMapper userBadgeMapper,
                                SysUserService sysUserService) {
        this.badgeMapper = badgeMapper;
        this.userBadgeMapper = userBadgeMapper;
        this.sysUserService = sysUserService;
    }

    /**
     * 勋章库分页
     */
    @GetMapping("/page")
    public R<Page<Badge>> page(@RequestParam(defaultValue = "1") long pageNum,
                               @RequestParam(defaultValue = "10") long pageSize,
                               @RequestParam(required = false) String keyword,
                               HttpServletRequest request) {
        requireAdmin(request);

        LambdaQueryWrapper<Badge> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Badge::getCode, keyword.trim())
                    .or()
                    .like(Badge::getName, keyword.trim())
                    .or()
                    .like(Badge::getDescription, keyword.trim()));
        }
        qw.orderByDesc(Badge::getId);

        Page<Badge> page = badgeMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return R.ok(page);
    }

    /**
     * 保存勋章
     */
    @PostMapping("/save")
    public R<Long> save(@RequestBody Badge badge, HttpServletRequest request) {
        requireAdmin(request);

        if (!StringUtils.hasText(badge.getCode())) {
            throw new BizException("勋章编码不能为空");
        }
        if (!StringUtils.hasText(badge.getName())) {
            throw new BizException("勋章名称不能为空");
        }

        if (badge.getId() == null) {
            Badge exist = badgeMapper.selectOne(
                    new LambdaQueryWrapper<Badge>()
                            .eq(Badge::getCode, badge.getCode().trim())
                            .last("limit 1")
            );
            if (exist != null) {
                throw new BizException("勋章编码已存在");
            }

            badge.setCode(badge.getCode().trim());
            badge.setName(badge.getName().trim());
            if (badge.getDescription() != null) {
                badge.setDescription(badge.getDescription().trim());
            }
            if (badge.getIcon() != null) {
                badge.setIcon(badge.getIcon().trim());
            }
            badge.setCreateTime(LocalDateTime.now());

            badgeMapper.insert(badge);
            return R.ok(badge.getId());
        }

        Badge db = badgeMapper.selectById(badge.getId());
        if (db == null) {
            throw new BizException("勋章不存在");
        }

        Badge sameCode = badgeMapper.selectOne(
                new LambdaQueryWrapper<Badge>()
                        .eq(Badge::getCode, badge.getCode().trim())
                        .ne(Badge::getId, badge.getId())
                        .last("limit 1")
        );
        if (sameCode != null) {
            throw new BizException("勋章编码已存在");
        }

        db.setCode(badge.getCode().trim());
        db.setName(badge.getName().trim());
        db.setDescription(badge.getDescription() == null ? null : badge.getDescription().trim());
        db.setIcon(badge.getIcon() == null ? null : badge.getIcon().trim());

        badgeMapper.updateById(db);
        return R.ok(db.getId());
    }

    /**
     * 删除勋章
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);

        Badge db = badgeMapper.selectById(id);
        if (db == null) {
            throw new BizException("勋章不存在");
        }

        long bindCount = userBadgeMapper.selectCount(
                new LambdaQueryWrapper<UserBadge>().eq(UserBadge::getBadgeCode, db.getCode())
        );
        if (bindCount > 0) {
            throw new BizException("该勋章已发放给用户，不能直接删除");
        }

        return R.ok(badgeMapper.deleteById(id) > 0);
    }

    /**
     * 用户勋章发放记录分页
     */
    @GetMapping("/user-page")
    public R<Page<Map<String, Object>>> userPage(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String badgeCode,
                                                 HttpServletRequest request) {
        requireAdmin(request);

        Page<UserBadge> rawPage = userBadgeMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UserBadge>()
                        .orderByDesc(UserBadge::getCreateTime)
                        .orderByDesc(UserBadge::getId)
        );

        List<Map<String, Object>> records = rawPage.getRecords().stream()
                .map(this::buildUserBadgeRow)
                .filter(row -> matchKeyword(row, keyword) && matchBadgeCode(row, badgeCode))
                .collect(Collectors.toList());

        Page<Map<String, Object>> res = new Page<>(pageNum, pageSize);
        res.setCurrent(rawPage.getCurrent());
        res.setSize(rawPage.getSize());
        res.setTotal(rawPage.getTotal());
        res.setRecords(records);
        return R.ok(res);
    }

    private boolean matchKeyword(Map<String, Object> row, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String kw = keyword.trim();
        String username = String.valueOf(row.getOrDefault("username", ""));
        String nickname = String.valueOf(row.getOrDefault("nickname", ""));
        String badgeCode = String.valueOf(row.getOrDefault("badgeCode", ""));
        String badgeName = String.valueOf(row.getOrDefault("badgeName", ""));
        return username.contains(kw)
                || nickname.contains(kw)
                || badgeCode.contains(kw)
                || badgeName.contains(kw);
    }

    private boolean matchBadgeCode(Map<String, Object> row, String badgeCode) {
        if (!StringUtils.hasText(badgeCode)) {
            return true;
        }
        return badgeCode.trim().equals(String.valueOf(row.get("badgeCode")));
    }

    private Map<String, Object> buildUserBadgeRow(UserBadge userBadge) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", userBadge.getId());
        row.put("userId", userBadge.getUserId());
        row.put("badgeCode", userBadge.getBadgeCode());
        row.put("createTime", userBadge.getCreateTime());

        SysUser user = userBadge.getUserId() == null ? null : sysUserService.getById(userBadge.getUserId());
        Badge badge = badgeMapper.selectOne(
                new LambdaQueryWrapper<Badge>()
                        .eq(Badge::getCode, userBadge.getBadgeCode())
                        .last("limit 1")
        );

        row.put("username", user == null ? "" : user.getUsername());
        row.put("nickname", user == null ? "" : user.getNickname());
        row.put("phone", user == null ? "" : user.getPhone());

        row.put("badgeName", badge == null ? "" : badge.getName());
        row.put("badgeDescription", badge == null ? "" : badge.getDescription());
        row.put("badgeIcon", badge == null ? "" : badge.getIcon());

        return row;
    }
}