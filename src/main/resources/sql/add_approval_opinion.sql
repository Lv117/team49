-- 为 honor 表添加审批意见字段
ALTER TABLE honor ADD COLUMN approval_opinion VARCHAR(500) COMMENT '审批意见';

-- 为 daily_activity 表添加审批意见字段
ALTER TABLE daily_activity ADD COLUMN approval_opinion VARCHAR(500) COMMENT '审批意见';

-- 说明：innovation_project 表已经有 approval_opinion 字段，无需添加
