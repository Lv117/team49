-- ============================================
-- 修复student_development表中status为NULL的数据
-- 将所有status为NULL或空的记录设置为'draft'
-- ============================================

-- 更新status为NULL的记录
UPDATE `student_development`
SET `status` = 'draft'
WHERE `status` IS NULL OR `status` = '';

-- 验证修复结果
SELECT 
    id,
    student_name,
    title,
    development_type,
    status,
    create_time
FROM `student_development`
WHERE development_type = 'achievement'
ORDER BY create_time DESC
LIMIT 10;

-- 统计各状态的记录数
SELECT 
    status,
    COUNT(*) as count
FROM `student_development`
GROUP BY status;
