-- ============================================
-- 学生发展表(student_development)创建脚本
-- 用途: 统一管理荣誉奖励、创新实践、学科竞赛、科技成果
-- 创建时间: 2026-04-21
-- ============================================

-- 1. 创建学生发展表
CREATE TABLE IF NOT EXISTS `student_development` (
    `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `student_id` INT NOT NULL COMMENT '学生ID',
    `student_name` VARCHAR(50) COMMENT '学生姓名',
    `title` VARCHAR(200) NOT NULL COMMENT '标题(项目名称/荣誉名称/竞赛名称/成果名称)',
    `description` TEXT COMMENT '描述',
    `development_type` VARCHAR(20) NOT NULL COMMENT '类型: honor-荣誉奖励, innovation-创新实践, competition-学科竞赛, achievement-科技成果',
    `status` VARCHAR(20) DEFAULT 'draft' COMMENT '状态: draft-草稿, submitted-已提交, approved-已通过, rejected-已驳回',
    `approval_opinion` TEXT COMMENT '审批意见',
    `approver_id` INT COMMENT '审批人ID',
    `approve_time` DATETIME COMMENT '审批时间',
    `extra_info` JSON COMMENT '扩展字段(JSON格式存储不同类型特有信息)',
    `start_date` DATE COMMENT '开始日期',
    `end_date` DATE COMMENT '结束日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX `idx_student_id` (`student_id`),
    INDEX `idx_development_type` (`development_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_student_type` (`student_id`, `development_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生发展记录表(荣誉/创新/竞赛/成果)';

-- 2. 数据迁移脚本(可选,从honor和innovation_project表迁移历史数据)
-- 注意: 执行前请备份原表数据!

-- 2.1 迁移荣誉奖励数据
INSERT INTO `student_development` (
    `student_id`,
    `student_name`,
    `title`,
    `description`,
    `development_type`,
    `status`,
    `approval_opinion`,
    `start_date`,
    `end_date`,
    `create_time`,
    `update_time`,
    `extra_info`
)
SELECT 
    `student_id`,
    `student_name`,
    `honor_name` AS `title`,
    `description`,
    'honor' AS `development_type`,
    `status`,
    `approval_opinion`,
    `award_date` AS `start_date`,
    `award_date` AS `end_date`,
    `create_time`,
    `update_time`,
    JSON_OBJECT('level', `honor_level`, 'issuer', '') AS `extra_info`
FROM `honor`
WHERE 1=1;  -- 可添加条件筛选

-- 2.2 迁移创新实践数据
INSERT INTO `student_development` (
    `student_id`,
    `student_name`,
    `title`,
    `description`,
    `development_type`,
    `status`,
    `approval_opinion`,
    `start_date`,
    `end_date`,
    `create_time`,
    `update_time`,
    `extra_info`
)
SELECT 
    `student_id`,
    `student_name`,
    `project_name` AS `title`,
    `description`,
    'innovation' AS `development_type`,
    `status`,
    `approval_opinion`,
    `start_date`,
    `end_date`,
    `create_time`,
    `update_time`,
    JSON_OBJECT('field', `project_type`, 'outcome', '') AS `extra_info`
FROM `innovation_project`
WHERE 1=1;  -- 可添加条件筛选

-- 3. 验证迁移结果
SELECT 
    development_type,
    COUNT(*) as count
FROM student_development
GROUP BY development_type;

-- 4. 注意事项
-- 4.1 迁移完成后,建议保留原表(honor, innovation_project)一段时间作为备份
-- 4.2 确认前端接口切换完成后,再考虑删除原表
-- 4.3 extra_info字段为JSON类型,需要MySQL 5.7+支持
-- 4.4 如果MySQL版本不支持JSON,可将extra_info改为TEXT类型,存储JSON字符串
