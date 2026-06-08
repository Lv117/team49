-- 为教师"曼波"（personId=16）添加任课记录
-- 假设该教师教授课程ID为1的课程（请根据实际情况修改course_id）

INSERT INTO course_teaching (teacher_id, course_id, classroom, semester, status)
VALUES 
(16, 1, '教室A-101', '2024-2025-1', 'active');

-- 如果有多个课程，可以添加多条记录
-- INSERT INTO course_teaching (teacher_id, course_id, classroom, semester, status)
-- VALUES 
-- (16, 2, '教室A-102', '2024-2025-1', 'active'),
-- (16, 3, '教室A-103', '2024-2025-1', 'active');
