-- 修改exam_schedule表中的exam_time字段，从DATE类型改为VARCHAR类型，支持存储完整的时间信息（日期+时间段）
ALTER TABLE `exam_schedule` MODIFY COLUMN `exam_time` VARCHAR(100) NOT NULL COMMENT '考试时间（格式：yyyy-MM-dd 或 yyyy-MM-dd 第X-Y节(HH:MM-HH:MM)）';
