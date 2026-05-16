-- 修复 student 表历史残留字段 person_person_id
-- 该字段不是当前实体映射需要的列，且为非空，导致新增学生时插入失败。

ALTER TABLE student
DROP COLUMN person_person_id;
