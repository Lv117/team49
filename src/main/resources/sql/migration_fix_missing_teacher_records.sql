-- 修复历史数据：补齐“教师账号已存在但 teacher 表缺失记录”的情况
-- 说明：
-- 1) 以 user_type.name = 'ROLE_TEACHER' 为准识别教师账号；
-- 2) 同时限定 person.type = '2'，避免误插入；
-- 3) 仅补缺失记录，不影响已存在 teacher 数据。

INSERT INTO teacher (person_id)
SELECT u.person_id
FROM `user` u
JOIN user_type ut ON ut.id = u.user_type_id
JOIN person p ON p.person_id = u.person_id
LEFT JOIN teacher t ON t.person_id = u.person_id
WHERE ut.name = 'ROLE_TEACHER'
  AND p.type = '2'
  AND t.person_id IS NULL;
