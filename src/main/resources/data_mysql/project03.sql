/*
 Navicat Premium Data Transfer

 Source Server         : sphas03
 Source Server Type    : MySQL
 Source Server Version : 50735 (5.7.35)
 Source Host           : localhost:3306
 Source Schema         : project03

 Target Server Type    : MySQL
 Target Server Version : 50735 (5.7.35)
 File Encoding         : 65001

 Date: 08/02/2026 16:02:22
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for bmi_standard
-- ----------------------------
DROP TABLE IF EXISTS `bmi_standard`;
CREATE TABLE `bmi_standard`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `min_value` decimal(5, 2) NOT NULL,
  `max_value` decimal(5, 2) NOT NULL,
  `level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `advice` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` int(11) NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bmi_standard
-- ----------------------------
INSERT INTO `bmi_standard` VALUES (1, 0.00, 18.50, '偏瘦', '建议适当增加蛋白质与力量训练', 1, '2026-01-29 16:42:08', '2026-01-29 16:42:08');
INSERT INTO `bmi_standard` VALUES (2, 18.50, 24.00, '正常', '保持规律饮食与运动', 1, '2026-01-29 16:42:25', '2026-01-29 16:42:25');
INSERT INTO `bmi_standard` VALUES (3, 24.00, 28.00, '超重', '建议减少高热量摄入，增加有氧运动', 1, '2026-01-29 16:42:42', '2026-01-29 16:42:42');
INSERT INTO `bmi_standard` VALUES (4, 28.00, 100.00, '肥胖', '建议制定减脂计划并持续监测指标', 1, '2026-01-29 16:42:55', '2026-01-29 16:42:55');

-- ----------------------------
-- Table structure for diet_plan
-- ----------------------------
DROP TABLE IF EXISTS `diet_plan`;
CREATE TABLE `diet_plan`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `bmi_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `tags` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` int(11) NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of diet_plan
-- ----------------------------
INSERT INTO `diet_plan` VALUES (1, '超重减脂饮食A', '超重', '减少油炸与含糖饮料；晚餐七分饱；每日蛋白质充足（鸡蛋/鱼/豆制品）；多蔬菜少精米面', '减脂,低糖', 1, '2026-01-29 17:17:32', '2026-01-29 17:17:32');
INSERT INTO `diet_plan` VALUES (2, '正常均衡饮食A', '正常', '三餐规律；碳水/蛋白/脂肪均衡；每日水果1-2份；少盐少油', '均衡,少油', 1, '2026-01-29 17:17:32', '2026-01-29 17:17:32');

-- ----------------------------
-- Table structure for feedback
-- ----------------------------
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '提交反馈的用户ID',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '反馈标题',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '反馈内容',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN处理中 / CLOSED已完成',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_feedback_user_time`(`user_id`, `create_time`) USING BTREE,
  INDEX `idx_feedback_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户反馈' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of feedback
-- ----------------------------
INSERT INTO `feedback` VALUES (1, 1, '页面闪退', '点击个人中心后白屏', 'OPEN', '2026-01-28 15:06:57', '2026-01-28 15:06:57');
INSERT INTO `feedback` VALUES (2, 1, '反馈标题', '反馈内容', 'OPEN', '2026-01-28 16:13:36', '2026-01-28 16:13:36');
INSERT INTO `feedback` VALUES (3, 1, '反馈标题', '反馈内容', 'OPEN', '2026-01-28 16:39:53', '2026-01-28 16:39:53');

-- ----------------------------
-- Table structure for feedback_reply
-- ----------------------------
DROP TABLE IF EXISTS `feedback_reply`;
CREATE TABLE `feedback_reply`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `feedback_id` bigint(20) NOT NULL COMMENT '关联的反馈ID',
  `sender_role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '发送者角色：ADMIN/USER',
  `sender_id` bigint(20) NOT NULL COMMENT '发送者ID（管理员或用户）',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '回复内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_feedback_id_create_time`(`feedback_id`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '反馈回复' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of feedback_reply
-- ----------------------------
INSERT INTO `feedback_reply` VALUES (1, 3, 'ADMIN', 1, '已收到反馈，我们会尽快处理。', '2026-01-28 17:23:43');

-- ----------------------------
-- Table structure for file_attachment
-- ----------------------------
DROP TABLE IF EXISTS `file_attachment`;
CREATE TABLE `file_attachment`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务类型：FEEDBACK/LOG等',
  `biz_id` bigint(20) NOT NULL COMMENT '业务ID：如feedback.id 或 health_log.id',
  `file_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '原始文件名',
  `file_url` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件访问URL',
  `file_size` bigint(20) NULL DEFAULT NULL COMMENT '文件大小（字节）',
  `mime_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件类型（image/png等）',
  `upload_user_id` bigint(20) NOT NULL COMMENT '上传人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_attach_biz`(`biz_type`, `biz_id`) USING BTREE,
  INDEX `idx_attach_user_time`(`upload_user_id`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通用附件' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_attachment
-- ----------------------------
INSERT INTO `file_attachment` VALUES (1, 'FEEDBACK', 2, 'feedback-upload', 'http://localhost:8080/upload/feedback/test.png', 0, 'image/*', 1, '2026-01-28 16:13:36');
INSERT INTO `file_attachment` VALUES (2, 'FEEDBACK', 3, 'feedback-upload', 'http://localhost:8080/upload/feedback/test.png', 0, 'image/*', 1, '2026-01-28 16:39:53');

-- ----------------------------
-- Table structure for health_article
-- ----------------------------
DROP TABLE IF EXISTS `health_article`;
CREATE TABLE `health_article`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文章标题',
  `summary` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文章摘要',
  `cover_url` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '封面图URL',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文章正文（可富文本/markdown）',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1-发布 0-下线',
  `author_id` bigint(20) NULL DEFAULT NULL COMMENT '作者ID（管理员）',
  `publish_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `views` int(11) NOT NULL DEFAULT 0 COMMENT '浏览量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_article_status_time`(`status`, `publish_time`) USING BTREE,
  INDEX `idx_article_title`(`title`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '健康科普文章' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of health_article
-- ----------------------------
INSERT INTO `health_article` VALUES (1, '健康饮食小常识', '每天吃什么更健康', 'https://example.com/a.jpg', '少油少盐，多吃蔬菜水果，坚持运动。', 1, NULL, '2026-01-28 14:48:10', 0, '2026-01-28 14:48:09', '2026-01-28 14:48:09');

-- ----------------------------
-- Table structure for health_metric_record
-- ----------------------------
DROP TABLE IF EXISTS `health_metric_record`;
CREATE TABLE `health_metric_record`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `record_time` datetime NOT NULL,
  `height_cm` decimal(5, 2) NULL DEFAULT NULL,
  `weight_kg` decimal(5, 2) NULL DEFAULT NULL,
  `bmi` decimal(5, 2) NULL DEFAULT NULL,
  `body_fat` decimal(5, 2) NULL DEFAULT NULL,
  `blood_sugar` decimal(5, 2) NULL DEFAULT NULL,
  `systolic` int(11) NULL DEFAULT NULL,
  `diastolic` int(11) NULL DEFAULT NULL,
  `sleep_hours` decimal(4, 2) NULL DEFAULT NULL,
  `steps` int(11) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_time`(`user_id`, `record_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of health_metric_record
-- ----------------------------
INSERT INTO `health_metric_record` VALUES (1, 1, '2026-01-29 10:00:00', 175.00, 80.00, 26.12, 24.50, 5.80, 132, 86, 6.50, 5200, '2026-01-29 16:07:35', '2026-01-29 16:07:35');
INSERT INTO `health_metric_record` VALUES (2, 1, '2026-01-29 18:00:00', 175.00, 79.00, 25.80, 24.00, 5.60, 128, 82, 7.00, 8000, '2026-01-29 16:08:20', '2026-01-29 16:08:20');
INSERT INTO `health_metric_record` VALUES (3, 1, '2026-01-30 08:00:00', 175.00, 95.00, 31.02, NULL, 7.20, 150, 95, 5.20, 2000, '2026-01-29 18:34:35', '2026-01-29 18:34:35');

-- ----------------------------
-- Table structure for health_record
-- ----------------------------
DROP TABLE IF EXISTS `health_record`;
CREATE TABLE `health_record`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `record_date` date NOT NULL,
  `height_cm` decimal(5, 2) NULL DEFAULT NULL,
  `weight_kg` decimal(5, 2) NULL DEFAULT NULL,
  `systolic` int(11) NULL DEFAULT NULL,
  `diastolic` int(11) NULL DEFAULT NULL,
  `heart_rate` int(11) NULL DEFAULT NULL,
  `steps` int(11) NULL DEFAULT NULL,
  `sleep_hours` decimal(4, 2) NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id`, `record_date`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of health_record
-- ----------------------------
INSERT INTO `health_record` VALUES (1, 1, '2026-01-26', 175.00, 70.50, 120, 80, 72, 8000, 7.50, '状态不错', '2026-01-26 16:22:58', '2026-01-26 16:22:58');

-- ----------------------------
-- Table structure for health_risk_alert
-- ----------------------------
DROP TABLE IF EXISTS `health_risk_alert`;
CREATE TABLE `health_risk_alert`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `risk_level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `risk_score` int(11) NOT NULL,
  `block_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模拟区块Hash（数据指纹）',
  `prev_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0000000000000000' COMMENT '上一区块Hash（链式结构）',
  `reasons_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '加密后的原因JSON',
  `advice` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `source_record_id` bigint(20) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `ai_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'AI风险解读',
  `ai_prediction_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'AI预测结果(JSON)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_time`(`user_id`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of health_risk_alert
-- ----------------------------
INSERT INTO `health_risk_alert` VALUES (1, 1, 'LOW', 18, NULL, '0000000000000000', '[\"BMI偏高（超重）\"]', '建议减少高糖高油饮食，保持每周≥3次有氧运动', 2, '2026-01-29 17:43:18', NULL, NULL);
INSERT INTO `health_risk_alert` VALUES (2, 1, 'LOW', 18, NULL, '0000000000000000', '[\"BMI偏高（超重）\"]', '建议减少高糖高油饮食，保持每周≥3次有氧运动', 2, '2026-01-29 17:44:16', NULL, NULL);
INSERT INTO `health_risk_alert` VALUES (3, 1, 'HIGH', 100, NULL, '0000000000000000', '[\"BMI偏高（肥胖）\",\"血压偏高（≥140/90）\",\"血糖偏高（≥7.0）\",\"BMI较上次上升\",\"收缩压较上次上升\",\"舒张压较上次上升\",\"血糖较上次上升\",\"睡眠不足（<6小时）\",\"日均步数偏低（<5000）\"]', '建议控制总热量摄入，增加有氧+力量训练，持续监测体重变化；建议减少高盐饮食，规律作息；如长期偏高建议就医评估；建议控制精制碳水摄入，餐后适度运动；如持续偏高建议就医；建议保持规律作息，逐步提高到6.5-8小时睡眠；建议每天增加步行量，逐步达到≥6000-8000步', 3, '2026-01-29 18:35:05', NULL, NULL);
INSERT INTO `health_risk_alert` VALUES (4, 1, 'HIGH', 100, NULL, '0000000000000000', '[\"BMI偏高（肥胖）\",\"血压偏高（≥140/90）\",\"血糖偏高（≥7.0）\",\"BMI较上次上升\",\"收缩压较上次上升\",\"舒张压较上次上升\",\"血糖较上次上升\",\"睡眠不足（<6小时）\",\"日均步数偏低（<5000）\"]', '建议控制总热量摄入，增加有氧+力量训练，持续监测体重变化；建议减少高盐饮食，规律作息；如长期偏高建议就医评估；建议控制精制碳水摄入，餐后适度运动；如持续偏高建议就医；建议保持规律作息，逐步提高到6.5-8小时睡眠；建议每天增加步行量，逐步达到≥6000-8000步', 3, '2026-01-29 18:37:48', NULL, NULL);
INSERT INTO `health_risk_alert` VALUES (5, 1, 'HIGH', 100, NULL, '0000000000000000', '[\"BMI偏高（肥胖）\",\"血压偏高（≥140/90）\",\"血糖偏高（≥7.0）\",\"BMI较上次上升\",\"收缩压较上次上升\",\"舒张压较上次上升\",\"血糖较上次上升\",\"睡眠不足（<6小时）\",\"日均步数偏低（<5000）\"]', '建议控制总热量摄入，增加有氧+力量训练，持续监测体重变化；建议减少高盐饮食，规律作息；如长期偏高建议就医评估；建议控制精制碳水摄入，餐后适度运动；如持续偏高建议就医；建议保持规律作息，逐步提高到6.5-8小时睡眠；建议每天增加步行量，逐步达到≥6000-8000步', 3, '2026-01-29 19:14:12', NULL, NULL);

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告内容（可富文本）',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1-发布 0-下线',
  `publisher_id` bigint(20) NULL DEFAULT NULL COMMENT '发布人用户ID（管理员）',
  `publish_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notice_status_time`(`status`, `publish_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统公告' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notice
-- ----------------------------
INSERT INTO `notice` VALUES (1, '系统维护通知', '本周六凌晨 1:00-2:00 系统维护，请提前保存数据。', 0, NULL, NULL, '2026-01-29 15:23:44', '2026-01-29 15:48:04');
INSERT INTO `notice` VALUES (2, '【健康风险预警】检测到较高风险，请及时关注', '触发原因：BMI偏高（肥胖）、血压偏高（≥140/90）、血糖偏高（≥7.0）、BMI较上次上升、收缩压较上次上升、舒张压较上次上升、血糖较上次上升、睡眠不足（<6小时）、日均步数偏低（<5000）。\n建议：建议控制总热量摄入，增加有氧+力量训练，持续监测体重变化；建议减少高盐饮食，规律作息；如长期偏高建议就医评估；建议控制精制碳水摄入，餐后适度运动；如持续偏高建议就医；建议保持规律作息，逐步提高到6.5-8小时睡眠；建议每天增加步行量，逐步达到≥6000-8000步。\n（系统自动生成）', 1, NULL, NULL, '2026-01-29 18:35:05', '2026-01-29 18:35:05');

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '接收通知的用户ID',
  `type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知类型：INACTIVE/RISK/SYSTEM等',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '内容（扩容以容纳风险建议）',
  `is_read` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notice_user_read_time`(`user_id`, `is_read`, `create_time`) USING BTREE,
  INDEX `idx_notice_user_time`(`user_id`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '站内通知' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notification
-- ----------------------------

-- ----------------------------
-- Table structure for question_bank
-- ----------------------------
DROP TABLE IF EXISTS `question_bank`;
CREATE TABLE `question_bank`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dimension` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `question` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `options_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` int(11) NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of question_bank
-- ----------------------------
INSERT INTO `question_bank` VALUES (1, 'SPORT', '你一周运动几次？', '[{\"label\":\"0次\",\"score\":0},{\"label\":\"1-2次\",\"score\":1},{\"label\":\"3-4次\",\"score\":2},{\"label\":\"5次及以上\",\"score\":3}]', 1, '2026-01-29 16:46:33', '2026-01-29 16:46:33');

-- ----------------------------
-- Table structure for sport_plan
-- ----------------------------
DROP TABLE IF EXISTS `sport_plan`;
CREATE TABLE `sport_plan`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `bmi_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `intensity` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` int(11) NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sport_plan
-- ----------------------------
INSERT INTO `sport_plan` VALUES (1, '超重有氧入门', '超重', '快走30分钟（每周4次）+ 拉伸10分钟；逐步提升到慢跑', 'LOW', 1, '2026-01-29 17:17:32', '2026-01-29 17:17:32');
INSERT INTO `sport_plan` VALUES (2, '超重有氧进阶', '超重', '慢跑/椭圆机40分钟（每周4次）+ 核心训练15分钟', 'MID', 1, '2026-01-29 17:17:32', '2026-01-29 17:17:32');
INSERT INTO `sport_plan` VALUES (3, '正常保持运动', '正常', '有氧30分钟（每周3次）+ 力量训练20分钟（每周2次）', 'MID', 1, '2026-01-29 17:17:32', '2026-01-29 17:17:32');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'USER',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT 1,
  `last_login_time` datetime NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'test01', '$2a$10$AQ/2bk6DzHJI0YEXIM1s4u391b.CbgU6TOCgRbqdrUK.J.Bw9rT1C', 'USER', '小明', NULL, 1, '2026-01-29 15:33:47', '2026-01-26 15:58:45', '2026-01-26 15:58:45');
INSERT INTO `sys_user` VALUES (2, 'admin01', '$2a$10$mOFffAiWHj1T2HSoo8W7CuAaPuH4D5RkJdUD2BwAYjzPXCZFLS0Si', 'USER', NULL, NULL, 1, '2026-01-28 18:44:08', '2026-01-28 18:42:32', '2026-01-28 18:42:32');
INSERT INTO `sys_user` VALUES (3, 'admin', '$2a$10$yxevWha1ncDej.D6ReAfBeXNZGkPKFTG/5Q.ESbQ5QHYa0.bbBTQS', 'ADMIN', '超级管理员', NULL, 1, '2026-01-30 21:19:23', '2026-01-29 14:53:15', '2026-01-29 14:53:15');
INSERT INTO `sys_user` VALUES (4, 'admin02', '$2a$10$pCJpuvzsg.MOUDMdXOEMRuYJXL8Wdjr7qt8g6tEMA5tp1AiSb.Aq.', 'ADMIN', '管理员2号', NULL, 1, '2026-01-29 15:47:16', '2026-01-29 15:01:16', '2026-01-29 15:01:16');

-- ----------------------------
-- Table structure for user_recommendation
-- ----------------------------
DROP TABLE IF EXISTS `user_recommendation`;
CREATE TABLE `user_recommendation`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `bmi` decimal(5, 2) NOT NULL,
  `bmi_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `scores_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `diet_plan_id` bigint(20) NULL DEFAULT NULL,
  `sport_plan_id` bigint(20) NULL DEFAULT NULL,
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_time`(`user_id`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_recommendation
-- ----------------------------
INSERT INTO `user_recommendation` VALUES (1, 1, 25.80, '超重', '{\"SPORT\":3,\"DIET\":2,\"SLEEP\":1,\"STRESS\":1}', 1, 2, '基于你当前BMI(25.80，超重)与运动得分(3)生成今日方案', '2026-01-29 17:18:50');

-- ----------------------------
-- Table structure for weekly_report
-- ----------------------------
DROP TABLE IF EXISTS `weekly_report`;
CREATE TABLE `weekly_report`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `week_start` date NOT NULL,
  `week_end` date NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `metrics_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `table_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_week_range`(`week_start`, `week_end`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of weekly_report
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
