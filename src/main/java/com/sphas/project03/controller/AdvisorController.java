package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.AdvisorAdviceSendDTO;
import com.sphas.project03.entity.HealthRiskAlert;
import com.sphas.project03.entity.SysMessage;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.mapper.SysUserMapper;
import com.sphas.project03.service.HealthRiskAlertService;
import com.sphas.project03.service.SysMessageService;
import com.sphas.project03.utils.PrivacyUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 健康顾问专属接口
 */
@RestController
@RequestMapping("/api/advisor")
@Validated
public class AdvisorController extends BaseController {

    private final HealthRiskAlertService alertService;
    private final SysMessageService sysMessageService;
    private final SysUserMapper sysUserMapper;

    public AdvisorController(HealthRiskAlertService alertService,
                             SysMessageService sysMessageService,
                             SysUserMapper sysUserMapper) {
        this.alertService = alertService;
        this.sysMessageService = sysMessageService;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 查看最近高风险用户
     * 每个用户只保留最近一条高风险记录
     */
    @GetMapping("/high-risk-users")
    public R<List<Map<String, Object>>> highRiskUsers(@RequestParam(defaultValue = "10") Integer limit,
                                                      HttpServletRequest request) {
        requireAdminOrAdvisor(request);

        int n = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);

        List<HealthRiskAlert> rawList = alertService.list(
                new LambdaQueryWrapper<HealthRiskAlert>()
                        .eq(HealthRiskAlert::getRiskLevel, "HIGH")
                        .orderByDesc(HealthRiskAlert::getCreateTime)
                        .last("limit " + (n * 5))
        );

        Map<Long, Map<String, Object>> userLatestMap = new LinkedHashMap<>();
        for (HealthRiskAlert alert : rawList) {
            if (alert == null || alert.getUserId() == null) {
                continue;
            }
            if (userLatestMap.containsKey(alert.getUserId())) {
                continue;
            }

            SysUser user = sysUserMapper.selectById(alert.getUserId());
            if (user == null) {
                continue;
            }

            String reasonsJson = PrivacyUtil.decrypt(alert.getReasonsJson());
            List<String> reasons = parseReasons(reasonsJson);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", user.getId());
            item.put("username", user.getUsername());
            item.put("nickname", emptyToDefault(user.getNickname(), "未设置昵称"));
            item.put("phone", maskPhone(user.getPhone()));
            item.put("gender", user.getGender());
            item.put("age", user.getAge());
            item.put("riskAlertId", alert.getId());
            item.put("riskLevel", alert.getRiskLevel());
            item.put("riskScore", alert.getRiskScore());
            item.put("advice", alert.getAdvice());
            item.put("aiSummary", alert.getAiSummary());
            item.put("reasons", reasons);
            item.put("createTime", alert.getCreateTime());

            userLatestMap.put(alert.getUserId(), item);

            if (userLatestMap.size() >= n) {
                break;
            }
        }

        return R.ok(new ArrayList<>(userLatestMap.values()));
    }

    /**
     * 最近已发送建议记录
     * 说明：
     * 1. 先从站内消息里找 ADVICE 类型
     * 2. 再补用户信息和对应风险预警信息
     * 3. 这里先按消息记录维度展示，便于后台快速回看
     */
    @GetMapping("/advice-records")
    public R<List<Map<String, Object>>> adviceRecords(@RequestParam(defaultValue = "20") Integer limit,
                                                      HttpServletRequest request) {
        requireAdminOrAdvisor(request);

        int n = (limit == null || limit <= 0) ? 20 : Math.min(limit, 100);

        List<SysMessage> msgList = sysMessageService.list(
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getType, "ADVICE")
                        .orderByDesc(SysMessage::getCreateTime)
                        .last("limit " + n)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysMessage msg : msgList) {
            if (msg == null) {
                continue;
            }

            SysUser user = msg.getUserId() == null ? null : sysUserMapper.selectById(msg.getUserId());
            HealthRiskAlert alert = msg.getBizId() == null ? null : alertService.getById(msg.getBizId());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("messageId", msg.getId());
            item.put("userId", msg.getUserId());
            item.put("username", user == null ? "-" : user.getUsername());
            item.put("nickname", user == null ? "-" : emptyToDefault(user.getNickname(), "未设置昵称"));
            item.put("phone", user == null ? "-" : maskPhone(user.getPhone()));
            item.put("title", msg.getTitle());
            item.put("content", msg.getContent());
            item.put("isRead", msg.getIsRead());
            item.put("readTime", msg.getReadTime());
            item.put("createTime", msg.getCreateTime());
            item.put("riskAlertId", msg.getBizId());

            if (alert != null) {
                item.put("riskLevel", alert.getRiskLevel());
                item.put("riskScore", alert.getRiskScore());
                item.put("alertCreateTime", alert.getCreateTime());

                String reasonsJson = PrivacyUtil.decrypt(alert.getReasonsJson());
                item.put("reasons", parseReasons(reasonsJson));
            } else {
                item.put("riskLevel", null);
                item.put("riskScore", null);
                item.put("alertCreateTime", null);
                item.put("reasons", Collections.emptyList());
            }

            result.add(item);
        }

        return R.ok(result);
    }

    /**
     * 顾问发送健康建议
     * 这里先走站内消息
     */
    @PostMapping("/send-advice")
    public R<Boolean> sendAdvice(@RequestBody @Valid AdvisorAdviceSendDTO dto,
                                 HttpServletRequest request) {
        requireAdminOrAdvisor(request);

        SysUser user = sysUserMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new BizException("目标用户不存在");
        }
        if (!"USER".equals(user.getRole())) {
            throw new BizException("只能给普通用户发送健康建议");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("目标用户已被禁用");
        }

        if (dto.getRiskAlertId() != null) {
            HealthRiskAlert alert = alertService.getById(dto.getRiskAlertId());
            if (alert == null) {
                throw new BizException("关联的风险预警记录不存在");
            }
            if (!dto.getUserId().equals(alert.getUserId())) {
                throw new BizException("风险预警记录和目标用户不匹配");
            }
        }

        SysMessage msg = new SysMessage();
        msg.setUserId(dto.getUserId());
        msg.setType("ADVICE");
        msg.setTitle(dto.getTitle());
        msg.setContent(dto.getContent());
        msg.setBizId(dto.getRiskAlertId());
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());

        sysMessageService.save(msg);
        return R.ok(true);
    }

    private List<String> parseReasons(String reasonsJson) {
        if (reasonsJson == null || reasonsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String raw = reasonsJson.trim();
        if (raw.startsWith("[") && raw.endsWith("]")) {
            raw = raw.substring(1, raw.length() - 1);
        }

        if (raw.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] arr = raw.split(",");
        List<String> list = new ArrayList<>();
        for (String s : arr) {
            String x = s == null ? "" : s.trim();
            x = x.replace("\"", "");
            if (!x.isEmpty()) {
                list.add(x);
            }
        }
        return list;
    }

    private String emptyToDefault(String s, String def) {
        if (s == null || s.trim().isEmpty()) {
            return def;
        }
        return s.trim();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "-";
        }
        String p = phone.trim();
        if (p.length() != 11) {
            return p;
        }
        return p.substring(0, 3) + "****" + p.substring(7);
    }
}