package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 健康顾问专属接口
 * 这里把高风险用户、建议记录、顾问摘要统一收口
 */
@RestController
@RequestMapping("/api/advisor")
@Validated
public class AdvisorController extends BaseController {

    private final HealthRiskAlertService alertService;
    private final SysMessageService sysMessageService;
    private final SysUserMapper sysUserMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdvisorController(HealthRiskAlertService alertService,
                             SysMessageService sysMessageService,
                             SysUserMapper sysUserMapper) {
        this.alertService = alertService;
        this.sysMessageService = sysMessageService;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 顾问首页摘要
     * 这个接口后面做顾问工作台会比较方便
     */
    @GetMapping("/dashboard-summary")
    public R<Map<String, Object>> dashboardSummary(HttpServletRequest request) {
        requireAdminOrAdvisor(request);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recent7Days = now.minusDays(7);
        LocalDateTime recent30Days = now.minusDays(30);

        List<HealthRiskAlert> highList = alertService.list(
                new LambdaQueryWrapper<HealthRiskAlert>()
                        .eq(HealthRiskAlert::getRiskLevel, "HIGH")
                        .orderByDesc(HealthRiskAlert::getCreateTime)
                        .last("limit 500")
        );

        Set<Long> highUserIds = new LinkedHashSet<>();
        LocalDateTime latestHighTime = null;

        for (HealthRiskAlert alert : highList) {
            if (alert == null || alert.getUserId() == null) {
                continue;
            }
            highUserIds.add(alert.getUserId());

            if (latestHighTime == null) {
                latestHighTime = alert.getCreateTime();
            }
        }

        List<SysMessage> adviceList = sysMessageService.list(
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getType, "ADVICE")
                        .orderByDesc(SysMessage::getCreateTime)
                        .last("limit 500")
        );

        int adviceCount7d = 0;
        int unreadAdviceCount = 0;
        Set<Long> advisedUserIds = new HashSet<>();

        for (SysMessage msg : adviceList) {
            if (msg == null) {
                continue;
            }

            if (msg.getCreateTime() != null && !msg.getCreateTime().isBefore(recent7Days)) {
                adviceCount7d++;
            }
            if (msg.getIsRead() != null && msg.getIsRead() == 0) {
                unreadAdviceCount++;
            }
            if (msg.getUserId() != null) {
                advisedUserIds.add(msg.getUserId());
            }
        }

        int unadvisedHighRiskUserCount = 0;
        for (Long userId : highUserIds) {
            if (!advisedUserIds.contains(userId)) {
                unadvisedHighRiskUserCount++;
            }
        }

        List<HealthRiskAlert> recent30HighList = alertService.list(
                new LambdaQueryWrapper<HealthRiskAlert>()
                        .eq(HealthRiskAlert::getRiskLevel, "HIGH")
                        .ge(HealthRiskAlert::getCreateTime, recent30Days)
                        .orderByDesc(HealthRiskAlert::getCreateTime)
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("highRiskUserCount", highUserIds.size());
        result.put("unadvisedHighRiskUserCount", unadvisedHighRiskUserCount);
        result.put("adviceCount7d", adviceCount7d);
        result.put("unreadAdviceCount", unreadAdviceCount);
        result.put("highRiskCount30d", recent30HighList.size());
        result.put("latestHighTime", latestHighTime);

        return R.ok(result);
    }

    /**
     * 查看最近高风险用户
     * 每个用户只保留最近一条高风险记录
     * keyword 支持搜：用户名 / 昵称 / 手机号
     */
    @GetMapping("/high-risk-users")
    public R<List<Map<String, Object>>> highRiskUsers(@RequestParam(defaultValue = "10") Integer limit,
                                                      @RequestParam(required = false) String keyword,
                                                      HttpServletRequest request) {
        requireAdminOrAdvisor(request);

        int n = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        String kw = keyword == null ? "" : keyword.trim();

        List<HealthRiskAlert> rawList = alertService.list(
                new LambdaQueryWrapper<HealthRiskAlert>()
                        .eq(HealthRiskAlert::getRiskLevel, "HIGH")
                        .orderByDesc(HealthRiskAlert::getCreateTime)
                        .last("limit " + (n * 8))
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

            if (!matchKeyword(user, kw)) {
                continue;
            }

            String reasonsJson = PrivacyUtil.decrypt(alert.getReasonsJson());
            List<String> reasons = parseReasons(reasonsJson);
            Map<String, Object> prediction = parsePrediction(alert.getAiPredictionJson());

            SysMessage latestAdvice = sysMessageService.getOne(
                    new LambdaQueryWrapper<SysMessage>()
                            .eq(SysMessage::getUserId, alert.getUserId())
                            .eq(SysMessage::getType, "ADVICE")
                            .orderByDesc(SysMessage::getCreateTime)
                            .last("limit 1"),
                    false
            );

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

            item.put("predictionModel", prediction.get("model"));
            item.put("predictionLevel", prediction.get("predictedLevel"));
            item.put("predictionRiskScore", prediction.get("predictedRiskScore"));
            item.put("predictionWeightKg", prediction.get("predictedWeightKg"));
            item.put("predictionTrend", prediction.get("trend"));
            item.put("predictionConfidence", prediction.get("confidence"));
            item.put("predictionBasis", prediction.get("basis"));
            item.put("predictionSuggestion", prediction.get("suggestion"));

            item.put("hasAdvice", latestAdvice != null);
            if (latestAdvice != null) {
                item.put("latestAdviceMessageId", latestAdvice.getId());
                item.put("latestAdviceTitle", latestAdvice.getTitle());
                item.put("latestAdviceCreateTime", latestAdvice.getCreateTime());
                item.put("latestAdviceIsRead", latestAdvice.getIsRead());
            } else {
                item.put("latestAdviceMessageId", null);
                item.put("latestAdviceTitle", null);
                item.put("latestAdviceCreateTime", null);
                item.put("latestAdviceIsRead", null);
            }

            userLatestMap.put(alert.getUserId(), item);

            if (userLatestMap.size() >= n) {
                break;
            }
        }

        return R.ok(new ArrayList<>(userLatestMap.values()));
    }

    /**
     * 最近已发送建议记录
     * keyword 支持搜：用户名 / 昵称 / 手机号 / 建议标题
     */
    @GetMapping("/advice-records")
    public R<List<Map<String, Object>>> adviceRecords(@RequestParam(defaultValue = "20") Integer limit,
                                                      @RequestParam(required = false) String keyword,
                                                      HttpServletRequest request) {
        requireAdminOrAdvisor(request);

        int n = (limit == null || limit <= 0) ? 20 : Math.min(limit, 100);
        String kw = keyword == null ? "" : keyword.trim();

        List<SysMessage> msgList = sysMessageService.list(
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getType, "ADVICE")
                        .orderByDesc(SysMessage::getCreateTime)
                        .last("limit " + (n * 3))
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysMessage msg : msgList) {
            if (msg == null) {
                continue;
            }

            SysUser user = msg.getUserId() == null ? null : sysUserMapper.selectById(msg.getUserId());
            if (!matchKeyword(user, kw) && !containsIgnoreCase(msg.getTitle(), kw)) {
                continue;
            }

            HealthRiskAlert alert = msg.getBizId() == null ? null : alertService.getById(msg.getBizId());
            Map<String, Object> prediction = parsePrediction(alert == null ? null : alert.getAiPredictionJson());

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
                item.put("aiSummary", alert.getAiSummary());

                String reasonsJson = PrivacyUtil.decrypt(alert.getReasonsJson());
                item.put("reasons", parseReasons(reasonsJson));

                item.put("predictionModel", prediction.get("model"));
                item.put("predictionLevel", prediction.get("predictedLevel"));
                item.put("predictionRiskScore", prediction.get("predictedRiskScore"));
                item.put("predictionWeightKg", prediction.get("predictedWeightKg"));
                item.put("predictionTrend", prediction.get("trend"));
                item.put("predictionConfidence", prediction.get("confidence"));
                item.put("predictionBasis", prediction.get("basis"));
            } else {
                item.put("riskLevel", null);
                item.put("riskScore", null);
                item.put("alertCreateTime", null);
                item.put("aiSummary", null);
                item.put("reasons", Collections.emptyList());
                item.put("predictionModel", null);
                item.put("predictionLevel", null);
                item.put("predictionRiskScore", null);
                item.put("predictionWeightKg", null);
                item.put("predictionTrend", null);
                item.put("predictionConfidence", null);
                item.put("predictionBasis", Collections.emptyList());
            }

            result.add(item);

            if (result.size() >= n) {
                break;
            }
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
        msg.setTitle(dto.getTitle().trim());
        msg.setContent(dto.getContent().trim());
        msg.setBizId(dto.getRiskAlertId());
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());

        sysMessageService.save(msg);
        return R.ok(true);
    }

    private boolean matchKeyword(SysUser user, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        if (user == null) {
            return false;
        }

        String kw = keyword.trim();
        return containsIgnoreCase(user.getUsername(), kw)
                || containsIgnoreCase(user.getNickname(), kw)
                || containsIgnoreCase(user.getPhone(), kw);
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        if (text == null) {
            return false;
        }
        return text.toLowerCase().contains(keyword.trim().toLowerCase());
    }

    private Map<String, Object> parsePrediction(String aiPredictionJson) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", null);
        result.put("predictedLevel", null);
        result.put("predictedRiskScore", null);
        result.put("predictedWeightKg", null);
        result.put("trend", null);
        result.put("confidence", null);
        result.put("basis", Collections.emptyList());
        result.put("suggestion", null);

        if (aiPredictionJson == null || aiPredictionJson.trim().isEmpty()) {
            return result;
        }

        try {
            Map<String, Object> raw = objectMapper.readValue(
                    aiPredictionJson,
                    new TypeReference<Map<String, Object>>() {}
            );

            result.put("model", raw.get("model"));
            result.put("predictedLevel", raw.get("predictedLevel"));
            result.put("predictedRiskScore", raw.get("predictedRiskScore"));
            result.put("predictedWeightKg", normalizeNumber(raw.get("predictedWeightKg")));
            result.put("trend", raw.get("trend"));
            result.put("confidence", normalizeNumber(raw.get("confidence")));
            result.put("basis", raw.get("basis") instanceof List ? raw.get("basis") : Collections.emptyList());
            result.put("suggestion", raw.get("suggestion"));
            return result;
        } catch (Exception e) {
            return result;
        }
    }

    private Object normalizeNumber(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return value;
        }
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