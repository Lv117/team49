-- ============================================
-- 为 course 表添加 teacher_id 字段
-- 修复课程表缺少教师关联字段的问题
-- ============================================

-- 第一步：检查 course 表结构
DESCRIBE course;

-- 第二步：如果缺少 teacher_id 字段，执行以下语句添加
ALTER TABLE course 
ADD COLUMN IF NOT EXISTS teacher_id INT COMMENT '任课教师ID' AFTER schedule;

-- 第三步：添加外键约束
ALTER TABLE course 
ADD CONSTRAINT fk_course_teacher 
FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id) 
ON DELETE SET NULL 
ON UPDATE CASCADE;

-- 第四步：查看当前所有课程及其教师分配情况
SELECT 
    c.course_id,
    c.num AS course_num,
    c.name AS course_name,
    c.teacher_id,
    p.name AS teacher_name
FROM course c
LEFT JOIN person p ON c.teacher_id = p.person_id
ORDER BY c.course_id;

-- 第五步：查看某个特定教师（例如：张三）授课的所有课程
-- 将 '张三' 替换为实际的教师姓名
SELECT 
    c.course_id,
    c.num AS course_num,
    c.name AS course_name,
    p.name AS teacher_name
FROM course c
INNER JOIN person p ON c.teacher_id = p.person_id
WHERE p.name = '张三'
ORDER BY c.course_id;

-- 第六步：如果发现错误的教师关联，可以修正
-- 示例：将课程ID为1的课程的教师改为教师ID为2的教师
-- UPDATE course SET teacher_id = 2 WHERE course_id = 1;

-- 第七步：如果要清空某门课程的教师关联
-- UPDATE course SET teacher_id = NULL WHERE course_id = ?;

-- ============================================
-- 使用说明：
-- 1. 在 MySQL Workbench 中打开此脚本
-- 2. 逐步执行每个查询，检查结果
-- 3. 如果发现课程的 teacher_id 设置错误，使用 UPDATE 语句修正
-- 4. 确保每个课程的 teacher_id 正确指向任课教师
-- ============================================
