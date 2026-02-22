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

 Date: 21/02/2026 21:05:23
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for badge
-- ----------------------------
DROP TABLE IF EXISTS `badge`;
CREATE TABLE `badge`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of badge
-- ----------------------------
INSERT INTO `badge` VALUES (1, 'FIRST_CHALLENGE', '初次挑战', '首次完成挑战获得', '', '2026-02-21 20:28:53');
INSERT INTO `badge` VALUES (2, 'POINTS_50', '积分达人', '总积分达到50获得', '', '2026-02-21 20:28:53');
INSERT INTO `badge` VALUES (3, 'POST_3', '社区活跃', '发布3条帖子获得', '', '2026-02-21 20:28:53');

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
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bmi_standard
-- ----------------------------
INSERT INTO `bmi_standard` VALUES (3, 24.00, 28.00, '超重', '建议减少高热量摄入，增加有氧运动', 1, '2026-01-29 16:42:42', '2026-01-29 16:42:42');
INSERT INTO `bmi_standard` VALUES (4, 28.00, 100.00, '肥胖', '建议制定减脂计划并持续监测指标', 1, '2026-01-29 16:42:55', '2026-01-29 16:42:55');
INSERT INTO `bmi_standard` VALUES (6, 11.00, 23.00, '正常', '哈哈哈哈', 1, '2026-02-13 21:15:05', '2026-02-13 21:15:05');

-- ----------------------------
-- Table structure for challenge
-- ----------------------------
DROP TABLE IF EXISTS `challenge`;
CREATE TABLE `challenge`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `target_value` int(11) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT 1,
  `reward_points` int(11) NOT NULL DEFAULT 50,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of challenge
-- ----------------------------
INSERT INTO `challenge` VALUES (1, '每日步数挑战', '每天步数达到目标，完成后获得积分奖励', 'STEP', 8000, '2026-02-19', '2026-03-05', 1, 50, '2026-02-19 22:47:00', '2026-02-19 22:47:00');
INSERT INTO `challenge` VALUES (2, '跑步里程挑战', '累计跑步里程达到目标', 'RUN', 20, '2026-02-19', '2026-03-21', 1, 80, '2026-02-19 22:47:00', '2026-02-19 22:47:00');
INSERT INTO `challenge` VALUES (3, '控糖饮食挑战', '坚持低糖饮食，完成后获得积分奖励', 'DIET', 14, '2026-02-19', '2026-03-05', 1, 60, '2026-02-19 22:47:00', '2026-02-19 22:47:00');

-- ----------------------------
-- Table structure for challenge_join
-- ----------------------------
DROP TABLE IF EXISTS `challenge_join`;
CREATE TABLE `challenge_join`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `challenge_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `progress_value` int(11) NOT NULL DEFAULT 0,
  `finished` tinyint(4) NOT NULL DEFAULT 0,
  `finish_time` datetime NULL DEFAULT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ch_user`(`challenge_id`, `user_id`) USING BTREE,
  INDEX `idx_ch`(`challenge_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of challenge_join
-- ----------------------------
INSERT INTO `challenge_join` VALUES (1, 1, 1, 8000, 1, '2026-02-19 22:49:18', '2026-02-19 22:48:18');

-- ----------------------------
-- Table structure for community_comment
-- ----------------------------
DROP TABLE IF EXISTS `community_comment`;
CREATE TABLE `community_comment`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `post_id` bigint(20) NOT NULL COMMENT '动态ID',
  `user_id` bigint(20) NOT NULL COMMENT '评论者ID',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '评论内容',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of community_comment
-- ----------------------------

-- ----------------------------
-- Table structure for community_post
-- ----------------------------
DROP TABLE IF EXISTS `community_post`;
CREATE TABLE `community_post`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '发布者ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '打卡文字内容',
  `image_urls` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图片链接(逗号分隔)',
  `like_count` int(11) NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` int(11) NULL DEFAULT 0 COMMENT '评论数',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '状态: 0待审核, 1正常, 2违规',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of community_post
-- ----------------------------

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
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户反馈' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of feedback
-- ----------------------------
INSERT INTO `feedback` VALUES (1, 1, '页面闪退', '点击个人中心后白屏', 'OPEN', '2026-01-28 15:06:57', '2026-01-28 15:06:57');
INSERT INTO `feedback` VALUES (2, 1, '反馈标题', '反馈内容', 'CLOSED', '2026-01-28 16:13:36', '2026-02-13 21:14:44');
INSERT INTO `feedback` VALUES (3, 1, '反馈标题', '反馈内容', 'CLOSED', '2026-01-28 16:39:53', '2026-02-13 19:39:12');
INSERT INTO `feedback` VALUES (4, 1, '有问题', '无', 'OPEN', '2026-02-18 19:59:11', '2026-02-18 19:59:11');

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
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '反馈回复' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of feedback_reply
-- ----------------------------
INSERT INTO `feedback_reply` VALUES (1, 3, 'ADMIN', 1, '已收到反馈，我们会尽快处理。', '2026-01-28 17:23:43');
INSERT INTO `feedback_reply` VALUES (2, 3, 'ADMIN', 5, '已标记为已处理（关闭反馈）', '2026-02-13 19:39:12');
INSERT INTO `feedback_reply` VALUES (3, 3, 'ADMIN', 5, '2222', '2026-02-13 21:14:37');
INSERT INTO `feedback_reply` VALUES (4, 2, 'ADMIN', 5, '已标记为已处理（关闭反馈）', '2026-02-13 21:14:44');

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
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通用附件' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_attachment
-- ----------------------------
INSERT INTO `file_attachment` VALUES (1, 'FEEDBACK', 2, 'feedback-upload', 'http://localhost:8080/upload/feedback/test.png', 0, 'image/*', 1, '2026-01-28 16:13:36');
INSERT INTO `file_attachment` VALUES (2, 'FEEDBACK', 3, 'feedback-upload', 'http://localhost:8080/upload/feedback/test.png', 0, 'image/*', 1, '2026-01-28 16:39:53');
INSERT INTO `file_attachment` VALUES (3, 'FEEDBACK', 4, 'feedback-upload', 'http://localhost:8080/upload/feedback/e0adb37298fe4e77b497001b10792dd5.jpg', 0, 'image/*', 1, '2026-02-18 19:59:12');

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
INSERT INTO `health_article` VALUES (1, '健康饮食小常识', '每天吃什么更健康', 'https://tse1.mm.bing.net/th/id/OIP.grBwAREgVrW__eBIu2lbzgAAAA?cb=defcachec2&rs=1&pid=ImgDetMain&o=7&rm=3', '少油少盐，多吃蔬菜水果，坚持运动。', 1, NULL, '2026-01-28 14:48:10', 0, '2026-01-28 14:48:09', '2026-02-19 19:08:32');

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of health_record
-- ----------------------------
INSERT INTO `health_record` VALUES (1, 1, '2026-01-26', 175.00, 70.50, 120, 80, 72, 8000, 7.50, '状态不错', '2026-01-26 16:22:58', '2026-01-26 16:22:58');
INSERT INTO `health_record` VALUES (2, 1, '2026-02-18', 175.00, 60.00, NULL, NULL, NULL, NULL, NULL, NULL, '2026-02-18 20:45:12', '2026-02-18 20:45:12');

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
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of health_risk_alert
-- ----------------------------
INSERT INTO `health_risk_alert` VALUES (1, 1, 'LOW', 18, NULL, '0000000000000000', '[\"BMI偏高（超重）\"]', '建议减少高糖高油饮食，保持每周≥3次有氧运动', 2, '2026-01-29 17:43:18', NULL, NULL);
INSERT INTO `health_risk_alert` VALUES (2, 1, 'LOW', 18, NULL, '0000000000000000', '[\"BMI偏高（超重）\"]', '建议减少高糖高油饮食，保持每周≥3次有氧运动', 2, '2026-01-29 17:44:16', NULL, NULL);
INSERT INTO `health_risk_alert` VALUES (3, 1, 'HIGH', 100, NULL, '0000000000000000', '[\"BMI偏高（肥胖）\",\"血压偏高（≥140/90）\",\"血糖偏高（≥7.0）\",\"BMI较上次上升\",\"收缩压较上次上升\",\"舒张压较上次上升\",\"血糖较上次上升\",\"睡眠不足（<6小时）\",\"日均步数偏低（<5000）\"]', '建议控制总热量摄入，增加有氧+力量训练，持续监测体重变化；建议减少高盐饮食，规律作息；如长期偏高建议就医评估；建议控制精制碳水摄入，餐后适度运动；如持续偏高建议就医；建议保持规律作息，逐步提高到6.5-8小时睡眠；建议每天增加步行量，逐步达到≥6000-8000步', 3, '2026-01-29 18:35:05', NULL, NULL);
INSERT INTO `health_risk_alert` VALUES (4, 1, 'HIGH', 100, NULL, '0000000000000000', '[\"BMI偏高（肥胖）\",\"血压偏高（≥140/90）\",\"血糖偏高（≥7.0）\",\"BMI较上次上升\",\"收缩压较上次上升\",\"舒张压较上次上升\",\"血糖较上次上升\",\"睡眠不足（<6小时）\",\"日均步数偏低（<5000）\"]', '建议控制总热量摄入，增加有氧+力量训练，持续监测体重变化；建议减少高盐饮食，规律作息；如长期偏高建议就医评估；建议控制精制碳水摄入，餐后适度运动；如持续偏高建议就医；建议保持规律作息，逐步提高到6.5-8小时睡眠；建议每天增加步行量，逐步达到≥6000-8000步', 3, '2026-01-29 18:37:48', NULL, NULL);
INSERT INTO `health_risk_alert` VALUES (5, 1, 'HIGH', 100, NULL, '0000000000000000', '[\"BMI偏高（肥胖）\",\"血压偏高（≥140/90）\",\"血糖偏高（≥7.0）\",\"BMI较上次上升\",\"收缩压较上次上升\",\"舒张压较上次上升\",\"血糖较上次上升\",\"睡眠不足（<6小时）\",\"日均步数偏低（<5000）\"]', '建议控制总热量摄入，增加有氧+力量训练，持续监测体重变化；建议减少高盐饮食，规律作息；如长期偏高建议就医评估；建议控制精制碳水摄入，餐后适度运动；如持续偏高建议就医；建议保持规律作息，逐步提高到6.5-8小时睡眠；建议每天增加步行量，逐步达到≥6000-8000步', 3, '2026-01-29 19:14:12', NULL, NULL);
INSERT INTO `health_risk_alert` VALUES (6, 1, 'HIGH', 100, 'MRzsaVVSOQ5vWi0+MiULz2N1C4g8S5fRoMtDXOuJWh0=', 'GENESIS', 'EaZPR35OmzKeul8bBM9Kf9t+KjGNceBts0AZmnPxCAFcm6gQIZgprcurFoywR7q43g11zqPnV1PlO4z1FcpA7cuvNy0DsdPgxac3zXRex8Av5E0jw5aBYbgE5765QeHJg2Otz0NoiHOjZwlJtb6kxLY5J2QNRoWybetN6LeU8wPs2UAUURk5TNoGzYUYQ7Heea/eFAAE63peZz/Ml8WRJ4Mkc4qTft3SJB9OqxRR40ASEqrF1pCE1ZyNZBvYm3VoyxU07E2JMUCJL6sITQHr/EOMSmgCBi03N2zTLNr2fRnCv8QhTDAIE2usRcMlP8qA', '建议控制总热量摄入，增加有氧+力量训练，持续监测体重变化；建议严格控制盐摄入，规律作息；如长期偏高建议就医评估；建议严格控制精制碳水摄入，餐后适度运动；建议保持规律作息，逐步提高到6.5-8小时睡眠；建议每天增加步行量，逐步达到≥6000-8000步', 3, '2026-02-21 20:17:56', '根据您最近的健康数据分析，系统判定当前风险等级为 HIGH（100分），总体风险较高。主要异常包括：BMI偏高（肥胖）、血压偏高（需就医评估）、血糖偏高、BMI较上次上升。当前建议：建议控制总热量摄入，增加有氧+力量训练，持续监测体重变化；建议严格控制盐摄入，规律作息；如长期偏高建议就医评估；建议严格控制精制碳水摄入，餐后适度运动；建议保持规律作息，逐步提高到6.5-8小时睡眠；建议每天增加步行量，逐步达到≥6000-8000步。综合提示：建议近期优先处理主要异常指标，并在必要时及时就医评估。', '{\"trend\":\"UP\",\"predictedScore\":100,\"predictedLevel\":\"HIGH\",\"confidence\":0.8,\"message\":\"基于最近 5 次风险评分趋势，未来 7 天风险可能继续上升，请提前干预。\"}');

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
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统公告' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notice
-- ----------------------------
INSERT INTO `notice` VALUES (1, '系统维护通知', '本周六凌晨 1:00-2:00 系统维护，请提前保存数据。', 1, NULL, NULL, '2026-01-29 15:23:44', '2026-02-12 15:57:28');
INSERT INTO `notice` VALUES (2, '【健康风险预警】检测到较高风险，请及时关注', '触发原因：BMI偏高（肥胖）、血压偏高（≥140/90）、血糖偏高（≥7.0）、BMI较上次上升、收缩压较上次上升、舒张压较上次上升、血糖较上次上升、睡眠不足（<6小时）、日均步数偏低（<5000）。\n建议：建议控制总热量摄入，增加有氧+力量训练，持续监测体重变化；建议减少高盐饮食，规律作息；如长期偏高建议就医评估；建议控制精制碳水摄入，餐后适度运动；如持续偏高建议就医；建议保持规律作息，逐步提高到6.5-8小时睡眠；建议每天增加步行量，逐步达到≥6000-8000步。\n（系统自动生成）', 1, NULL, NULL, '2026-01-29 18:35:05', '2026-01-29 18:35:05');
INSERT INTO `notice` VALUES (3, '2222', '<p>1111</p>', 1, NULL, NULL, '2026-02-12 16:04:13', '2026-02-13 15:13:05');
INSERT INTO `notice` VALUES (4, '更新第一次20260212', '11111', 1, NULL, NULL, '2026-02-12 16:21:52', '2026-02-14 14:59:04');
INSERT INTO `notice` VALUES (5, '新标题1', '<p><s><em><strong>你好</strong></em></s></p>', 1, NULL, NULL, '2026-02-13 15:04:05', '2026-02-13 15:04:05');
INSERT INTO `notice` VALUES (6, '【健康风险预警】检测到较高风险，请及时关注', '触发原因：BMI偏高（肥胖）、血压偏高（需就医评估）、血糖偏高、BMI较上次上升、收缩压较上次上升、舒张压较上次上升、血糖较上次上升、睡眠不足（<6小时）、日均步数偏低（<5000）\n建议：建议控制总热量摄入，增加有氧+力量训练，持续监测体重变化；建议严格控制盐摄入，规律作息；如长期偏高建议就医评估；建议严格控制精制碳水摄入，餐后适度运动；建议保持规律作息，逐步提高到6.5-8小时睡眠；建议每天增加步行量，逐步达到≥6000-8000步\n（系统自动生成）', 1, NULL, NULL, '2026-02-21 20:17:55', '2026-02-21 20:17:55');

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
-- Table structure for point_record
-- ----------------------------
DROP TABLE IF EXISTS `point_record`;
CREATE TABLE `point_record`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `points` int(11) NOT NULL,
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `biz_id` bigint(20) NULL DEFAULT NULL,
  `remark` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_time`(`user_id`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of point_record
-- ----------------------------
INSERT INTO `point_record` VALUES (1, 1, 50, 'CHALLENGE_FINISH', 1, '完成挑战：每日步数挑战', '2026-02-19 22:49:18');
INSERT INTO `point_record` VALUES (2, 1, 5, 'POST_CREATE', 1, '发布日志/帖子', '2026-02-21 19:52:47');
INSERT INTO `point_record` VALUES (3, 1, 2, 'POST_COMMENT', 1, '评论帖子', '2026-02-21 19:54:49');
INSERT INTO `point_record` VALUES (4, 1, 1, 'POST_LIKE', 1, '点赞帖子', '2026-02-21 19:55:30');

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
) ENGINE = InnoDB AUTO_INCREMENT = 48 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of question_bank
-- ----------------------------
INSERT INTO `question_bank` VALUES (1, 'SPORT', '你一周运动几次？', '[{\"label\":\"0次\",\"score\":0},{\"label\":\"1-2次\",\"score\":1},{\"label\":\"3-4次\",\"score\":2},{\"label\":\"5次及以上\",\"score\":3}]', 1, '2026-01-29 16:46:33', '2026-01-29 16:46:33');
INSERT INTO `question_bank` VALUES (2, 'SLEEP', '你一天睡多少个小时', '[{\"text\":\"每天睡10个小时\",\"score\":4},{\"text\":\"每天睡9个小时\",\"score\":5}]', 1, '2026-02-13 18:36:11', '2026-02-13 18:36:11');
INSERT INTO `question_bank` VALUES (3, 'STRESS', '你的压力值如何', '[{\"text\":\"正常\",\"score\":10},{\"text\":\"略微有压力\",\"score\":12}]', 1, '2026-02-13 18:56:31', '2026-02-13 18:56:31');
INSERT INTO `question_bank` VALUES (7, 'DIET', '你每天吃蔬菜水果吗？', '[{\"text\":\"每天都吃\",\"score\":10},{\"text\":\"偶尔吃\",\"score\":5},{\"text\":\"几乎不吃\",\"score\":0}]', 1, '2026-02-13 19:56:37', '2026-02-13 19:56:37');
INSERT INTO `question_bank` VALUES (16, 'DIET', '你的晚餐时间通常？', '[{\"text\":\"19点前\",\"score\":10},{\"text\":\"19-21点\",\"score\":6},{\"text\":\"21点后\",\"score\":0}]', 1, '2026-02-13 20:21:57', '2026-02-13 20:21:57');
INSERT INTO `question_bank` VALUES (17, 'DIET', '你是否会暴饮暴食？', '[{\"text\":\"几乎不会\",\"score\":10},{\"text\":\"偶尔\",\"score\":5},{\"text\":\"经常\",\"score\":0}]', 1, '2026-02-13 20:21:57', '2026-02-13 20:21:57');
INSERT INTO `question_bank` VALUES (18, 'SLEEP', '你平均每天睡眠多久？', '[{\"text\":\"7-9小时\",\"score\":10},{\"text\":\"6-7小时\",\"score\":6},{\"text\":\"少于6小时\",\"score\":0}]', 1, '2026-02-13 20:21:57', '2026-02-13 20:21:57');
INSERT INTO `question_bank` VALUES (19, 'SLEEP', '你入睡是否困难？', '[{\"text\":\"几乎不困难\",\"score\":10},{\"text\":\"偶尔困难\",\"score\":5},{\"text\":\"经常困难\",\"score\":0}]', 1, '2026-02-13 20:21:57', '2026-02-13 20:21:57');
INSERT INTO `question_bank` VALUES (20, 'SLEEP', '你夜间是否会频繁醒来？', '[{\"text\":\"很少\",\"score\":10},{\"text\":\"偶尔\",\"score\":5},{\"text\":\"经常\",\"score\":0}]', 1, '2026-02-13 20:21:57', '2026-02-13 20:21:57');
INSERT INTO `question_bank` VALUES (21, 'SLEEP', '你起床后是否精神充足？', '[{\"text\":\"精神很好\",\"score\":10},{\"text\":\"一般\",\"score\":5},{\"text\":\"很疲惫\",\"score\":0}]', 1, '2026-02-13 20:21:57', '2026-02-13 20:21:57');
INSERT INTO `question_bank` VALUES (22, 'SLEEP', '你睡前会长时间使用手机吗？', '[{\"text\":\"很少\",\"score\":10},{\"text\":\"偶尔\",\"score\":5},{\"text\":\"经常\",\"score\":0}]', 1, '2026-02-13 20:21:57', '2026-02-13 20:21:57');
INSERT INTO `question_bank` VALUES (23, 'STRESS', '你最近压力水平如何？', '[{\"text\":\"很低\",\"score\":10},{\"text\":\"一般\",\"score\":5},{\"text\":\"很高\",\"score\":0}]', 1, '2026-02-13 20:21:57', '2026-02-13 20:21:57');
INSERT INTO `question_bank` VALUES (24, 'STRESS', '你是否经常感到焦虑？', '[{\"text\":\"很少\",\"score\":10},{\"text\":\"偶尔\",\"score\":5},{\"text\":\"经常\",\"score\":0}]', 1, '2026-02-13 20:21:57', '2026-02-13 20:21:57');
INSERT INTO `question_bank` VALUES (25, 'STRESS', '你是否会通过运动/兴趣减压？', '[{\"text\":\"经常\",\"score\":10},{\"text\":\"偶尔\",\"score\":5},{\"text\":\"从不\",\"score\":0}]', 1, '2026-02-13 20:21:57', '2026-02-13 20:21:57');
INSERT INTO `question_bank` VALUES (26, 'STRESS', '你是否有足够的休闲时间？', '[{\"text\":\"充足\",\"score\":10},{\"text\":\"一般\",\"score\":5},{\"text\":\"很少\",\"score\":0}]', 1, '2026-02-13 20:21:57', '2026-02-13 20:21:57');
INSERT INTO `question_bank` VALUES (27, 'STRESS', '你最近情绪波动是否明显？', '[{\"text\":\"不明显\",\"score\":10},{\"text\":\"偶尔\",\"score\":5},{\"text\":\"很明显\",\"score\":0}]', 1, '2026-02-13 20:21:57', '2026-02-13 20:21:57');
INSERT INTO `question_bank` VALUES (28, 'SPORT', '你每周运动几次？', '[{\"text\":\"5次及以上\",\"score\":10},{\"text\":\"3-4次\",\"score\":7},{\"text\":\"1-2次\",\"score\":4},{\"text\":\"几乎不运动\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (29, 'SPORT', '你每次运动时长通常是多少？', '[{\"text\":\"60分钟以上\",\"score\":10},{\"text\":\"30-60分钟\",\"score\":7},{\"text\":\"10-30分钟\",\"score\":4},{\"text\":\"少于10分钟\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (30, 'SPORT', '你是否会做力量训练？', '[{\"text\":\"每周规律进行\",\"score\":10},{\"text\":\"偶尔进行\",\"score\":5},{\"text\":\"从不\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (31, 'SPORT', '你日常步行/活动量如何？', '[{\"text\":\"每天8000步以上\",\"score\":10},{\"text\":\"每天5000-8000步\",\"score\":6},{\"text\":\"每天少于5000步\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (32, 'SPORT', '你是否久坐（连续坐超过1小时）？', '[{\"text\":\"经常\",\"score\":0},{\"text\":\"偶尔\",\"score\":5},{\"text\":\"很少\",\"score\":10}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (33, 'DIET', '你每天吃蔬菜水果吗？', '[{\"text\":\"每天都吃\",\"score\":10},{\"text\":\"偶尔吃\",\"score\":5},{\"text\":\"几乎不吃\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (34, 'DIET', '你每周喝含糖饮料的频率？', '[{\"text\":\"几乎不喝\",\"score\":10},{\"text\":\"每周1-2次\",\"score\":6},{\"text\":\"每周3次及以上\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (35, 'DIET', '你是否经常吃油炸食品？', '[{\"text\":\"很少\",\"score\":10},{\"text\":\"偶尔\",\"score\":5},{\"text\":\"经常\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (36, 'DIET', '你的晚餐时间通常？', '[{\"text\":\"19点前\",\"score\":10},{\"text\":\"19-21点\",\"score\":6},{\"text\":\"21点后\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (37, 'DIET', '你是否会暴饮暴食？', '[{\"text\":\"几乎不会\",\"score\":10},{\"text\":\"偶尔\",\"score\":5},{\"text\":\"经常\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (38, 'SLEEP', '你平均每天睡眠多久？', '[{\"text\":\"7-9小时\",\"score\":10},{\"text\":\"6-7小时\",\"score\":6},{\"text\":\"少于6小时\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (39, 'SLEEP', '你入睡是否困难？', '[{\"text\":\"几乎不困难\",\"score\":10},{\"text\":\"偶尔困难\",\"score\":5},{\"text\":\"经常困难\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (40, 'SLEEP', '你夜间是否会频繁醒来？', '[{\"text\":\"很少\",\"score\":10},{\"text\":\"偶尔\",\"score\":5},{\"text\":\"经常\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (41, 'SLEEP', '你起床后是否精神充足？', '[{\"text\":\"精神很好\",\"score\":10},{\"text\":\"一般\",\"score\":5},{\"text\":\"很疲惫\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (42, 'SLEEP', '你睡前会长时间使用手机吗？', '[{\"text\":\"很少\",\"score\":10},{\"text\":\"偶尔\",\"score\":5},{\"text\":\"经常\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (43, 'STRESS', '你最近压力水平如何？', '[{\"text\":\"很低\",\"score\":10},{\"text\":\"一般\",\"score\":5},{\"text\":\"很高\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (44, 'STRESS', '你是否经常感到焦虑？', '[{\"text\":\"很少\",\"score\":10},{\"text\":\"偶尔\",\"score\":5},{\"text\":\"经常\",\"score\":0}]', 1, '2026-02-14 13:00:14', '2026-02-14 13:00:14');
INSERT INTO `question_bank` VALUES (45, 'STRESS', '你是否会通过运动/兴趣减压？', '[{\"text\":\"经常\",\"score\":10},{\"text\":\"偶尔\",\"score\":5},{\"text\":\"从不\",\"score\":0}]', 1, '2026-02-14 13:00:15', '2026-02-14 13:00:15');
INSERT INTO `question_bank` VALUES (46, 'STRESS', '你是否有足够的休闲时间？', '[{\"text\":\"充足\",\"score\":10},{\"text\":\"一般\",\"score\":5},{\"text\":\"很少\",\"score\":0}]', 1, '2026-02-14 13:00:15', '2026-02-14 13:00:15');
INSERT INTO `question_bank` VALUES (47, 'STRESS', '你最近情绪波动是否明显？', '[{\"text\":\"不明显\",\"score\":10},{\"text\":\"偶尔\",\"score\":5},{\"text\":\"很明显\",\"score\":0}]', 1, '2026-02-14 13:00:15', '2026-02-14 13:00:15');

-- ----------------------------
-- Table structure for social_comment
-- ----------------------------
DROP TABLE IF EXISTS `social_comment`;
CREATE TABLE `social_comment`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `post_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post_time`(`post_id`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of social_comment
-- ----------------------------
INSERT INTO `social_comment` VALUES (1, 1, 1, NULL, '加油，坚持！', 1, '2026-02-21 19:54:49');

-- ----------------------------
-- Table structure for social_like
-- ----------------------------
DROP TABLE IF EXISTS `social_like`;
CREATE TABLE `social_like`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `post_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_user`(`post_id`, `user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of social_like
-- ----------------------------
INSERT INTO `social_like` VALUES (1, 1, 1, '2026-02-21 19:55:30');

-- ----------------------------
-- Table structure for social_post
-- ----------------------------
DROP TABLE IF EXISTS `social_post`;
CREATE TABLE `social_post`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `images_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `status` tinyint(4) NOT NULL DEFAULT 1,
  `like_count` int(11) NOT NULL DEFAULT 0,
  `comment_count` int(11) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_time`(`create_time`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of social_post
-- ----------------------------
INSERT INTO `social_post` VALUES (1, 1, NULL, '今天跑了3公里，状态不错！', '[\"http://xx/1.png\"]', 0, 1, 1, '2026-02-21 19:52:47', '2026-02-21 20:04:08');

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
-- Table structure for sys_message
-- ----------------------------
DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `biz_id` bigint(20) NULL DEFAULT NULL,
  `is_read` tinyint(4) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `read_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_time`(`user_id`, `create_time`) USING BTREE,
  INDEX `idx_user_read`(`user_id`, `is_read`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_message
-- ----------------------------
INSERT INTO `sys_message` VALUES (1, 1, 'RISK', '【风险预警】今日检测到较高健康风险', '触发原因：BMI偏高（肥胖）、血压偏高（需就医评估）、血糖偏高、BMI较上次上升、收缩压较上次上升、舒张压较上次上升、血糖较上次上升、睡眠不足（<6小时）、日均步数偏低（<5000）\n建议：建议控制总热量摄入，增加有氧+力量训练，持续监测体重变化；建议严格控制盐摄入，规律作息；如长期偏高建议就医评估；建议严格控制精制碳水摄入，餐后适度运动；建议保持规律作息，逐步提高到6.5-8小时睡眠；建议每天增加步行量，逐步达到≥6000-8000步', NULL, 0, '2026-02-21 20:17:56', NULL);

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
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'test01', '$2a$10$AQ/2bk6DzHJI0YEXIM1s4u391b.CbgU6TOCgRbqdrUK.J.Bw9rT1C', 'USER', '小明', NULL, 1, '2026-02-21 20:17:24', '2026-01-26 15:58:45', '2026-01-26 15:58:45');
INSERT INTO `sys_user` VALUES (2, 'admin01', '$2a$10$mOFffAiWHj1T2HSoo8W7CuAaPuH4D5RkJdUD2BwAYjzPXCZFLS0Si', 'USER', NULL, NULL, 1, '2026-01-28 18:44:08', '2026-01-28 18:42:32', '2026-01-28 18:42:32');
INSERT INTO `sys_user` VALUES (3, 'admin', '$2a$10$yxevWha1ncDej.D6ReAfBeXNZGkPKFTG/5Q.ESbQ5QHYa0.bbBTQS', 'ADMIN', '超级管理员', NULL, 1, '2026-02-21 20:03:22', '2026-01-29 14:53:15', '2026-01-29 14:53:15');
INSERT INTO `sys_user` VALUES (4, 'admin02', '$2a$10$pCJpuvzsg.MOUDMdXOEMRuYJXL8Wdjr7qt8g6tEMA5tp1AiSb.Aq.', 'ADMIN', '管理员2号', NULL, 1, '2026-01-29 15:47:16', '2026-01-29 15:01:16', '2026-01-29 15:01:16');
INSERT INTO `sys_user` VALUES (5, 'admin20260212', '$2a$10$NWboR49gyR/SP3EcU/J8k..YvyGlwdVmE4GJi72YOg/dJbgujyTbe', 'ADMIN', '张三', NULL, 1, '2026-02-13 22:02:32', '2026-02-12 15:43:36', '2026-02-12 15:43:36');
INSERT INTO `sys_user` VALUES (6, 'admin03', '$2a$10$Eb3cpV5kkRe7Bug7YM9J1Oj.Ca6VJEfThgI5fZUQF.eWkkk.dyiIS', 'ADMIN', '李四', NULL, 1, NULL, '2026-02-14 13:15:30', '2026-02-14 13:15:30');
INSERT INTO `sys_user` VALUES (7, 'admin04', '$2a$10$HJJ3xACTlVCM.0pM.OkVP.TTdHpzVeCL7Zobc.1jsPE7UyWQYnwJu', 'ADMIN', '王五', '13488827865', 1, NULL, '2026-02-14 13:29:11', '2026-02-14 13:29:11');
INSERT INTO `sys_user` VALUES (8, 'admin05', '$2a$10$PUNVeazWo/rN2d8XyKPcLe5OIz07NrBcoTp2.DijwqK4j4ZBKrGX6', 'ADMIN', '管理员5号', '12345678901', 1, NULL, '2026-02-14 13:31:41', '2026-02-14 13:31:41');
INSERT INTO `sys_user` VALUES (9, 'test02', '$2a$10$MLu3qgExLn7Pk0mn/DCyfusL7MwQ.pjZBW1VGe4uxtMlUWESdVxv.', 'USER', '辰于', '15227298800', 1, '2026-02-15 12:24:43', '2026-02-15 12:20:20', '2026-02-15 12:20:20');

-- ----------------------------
-- Table structure for user_badge
-- ----------------------------
DROP TABLE IF EXISTS `user_badge`;
CREATE TABLE `user_badge`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `badge_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_badge`(`user_id`, `badge_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_badge
-- ----------------------------

-- ----------------------------
-- Table structure for user_like
-- ----------------------------
DROP TABLE IF EXISTS `user_like`;
CREATE TABLE `user_like`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `post_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_user`(`post_id`, `user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_like
-- ----------------------------

-- ----------------------------
-- Table structure for user_plan
-- ----------------------------
DROP TABLE IF EXISTS `user_plan`;
CREATE TABLE `user_plan`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `diet_plan_id` bigint(20) NULL DEFAULT NULL,
  `sport_plan_id` bigint(20) NULL DEFAULT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_status`(`user_id`, `status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_plan
-- ----------------------------

-- ----------------------------
-- Table structure for user_plan_checkin
-- ----------------------------
DROP TABLE IF EXISTS `user_plan_checkin`;
CREATE TABLE `user_plan_checkin`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_plan_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `checkin_date` date NOT NULL,
  `diet_done` tinyint(4) NOT NULL DEFAULT 0,
  `sport_done` tinyint(4) NOT NULL DEFAULT 0,
  `remark` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_plan_date`(`user_plan_id`, `checkin_date`) USING BTREE,
  INDEX `idx_user_date`(`user_id`, `checkin_date`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_plan_checkin
-- ----------------------------

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
