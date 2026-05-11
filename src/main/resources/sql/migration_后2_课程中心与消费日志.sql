-- ============================================
-- 中期补充阶段数据库迁移脚本（后2任务）
-- 创建时间: 2026-04-27
-- 说明: 课程中心功能 + 学生消费日志管理
-- ============================================

-- 1. fee表新增字段（消费分类、备注、创建时间）
ALTER TABLE fee ADD COLUMN IF NOT EXISTS consumption_type VARCHAR(20) COMMENT '消费类型(dining-餐饮/study-学习用品/transport-交通费/life-生活用品/entertainment-娱乐)';
ALTER TABLE fee ADD COLUMN IF NOT EXISTS description VARCHAR(500) COMMENT '备注';
ALTER TABLE fee ADD COLUMN IF NOT EXISTS create_time DATETIME COMMENT '创建时间';

-- 为现有数据设置默认值
UPDATE fee SET consumption_type = 'other' WHERE consumption_type IS NULL;
UPDATE fee SET create_time = NOW() WHERE create_time IS NULL;

-- 2. 创建course_material表（课程资料）
CREATE TABLE IF NOT EXISTS course_material (
  material_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '资料ID',
  course_id INT COMMENT '课程ID',
  course_name VARCHAR(100) COMMENT '课程名称',
  material_name VARCHAR(200) NOT NULL COMMENT '资料名称',
  course_type VARCHAR(50) COMMENT '资料类型(textbook-教材/courseware-课件/reference-参考资料)',
  file_path VARCHAR(500) COMMENT '文件路径',
  file_name VARCHAR(200) COMMENT '文件名',
  file_size BIGINT COMMENT '文件大小(字节)',
  description TEXT COMMENT '描述',
  uploader_id INT COMMENT '上传者ID',
  uploader_name VARCHAR(50) COMMENT '上传者姓名',
  upload_time DATETIME COMMENT '上传时间',
  INDEX idx_course_id (course_id),
  INDEX idx_course_type (course_type),
  INDEX idx_uploader_id (uploader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程资料表';

-- 3. 添加索引优化查询性能
CREATE INDEX IF NOT EXISTS idx_fee_consumption_type ON fee(consumption_type);
CREATE INDEX IF NOT EXISTS idx_fee_create_time ON fee(create_time);
CREATE INDEX IF NOT EXISTS idx_fee_person_day ON fee(personId, day);

-- ============================================
-- 测试数据（可选）
-- ============================================

-- 插入测试课程资料数据
INSERT INTO course_material (course_id, course_name, material_name, course_type, file_name, file_size, description, uploader_id, uploader_name, upload_time) VALUES
(1, '高等数学', '高等数学教材第9版', 'textbook', '高等数学.pdf', 5242880, '2024版教材', 1, 'admin', NOW()),
(1, '高等数学', '高数课件第一章', 'courseware', '高数第一章.pptx', 2097152, '函数与极限', 1, 'admin', NOW()),
(1, '高等数学', '考研数学复习指南', 'reference', '考研数学.pdf', 8388608, '参考书籍', 1, 'admin', NOW());

-- 插入测试消费数据
INSERT INTO fee (personId, day, money, consumption_type, description, create_time) VALUES
(1, '2024-03-01', 15.5, 'dining', '早餐', NOW()),
(1, '2024-03-01', 25.0, 'dining', '午餐', NOW()),
(1, '2024-03-02', 50.0, 'study', '购买教材', NOW()),
(1, '2024-03-03', 10.0, 'transport', '公交车费', NOW()),
(1, '2024-03-05', 100.0, 'life', '购买生活用品', NOW()),
(1, '2024-03-07', 80.0, 'entertainment', '看电影', NOW());

-- ============================================
-- 验证脚本
-- ============================================

-- 验证fee表结构
DESC fee;

-- 验证course_material表结构
DESC course_material;

-- 验证课程资料数据
SELECT * FROM course_material;

-- 验证消费数据统计
SELECT 
  consumption_type,
  COUNT(*) as count,
  SUM(money) as total_amount,
  AVG(money) as avg_amount
FROM fee 
WHERE consumption_type IS NOT NULL 
GROUP BY consumption_type;

-- 验证月度消费统计
SELECT 
  DATE_FORMAT(create_time, '%Y-%m') as month,
  consumption_type,
  SUM(money) as total_amount
FROM fee 
WHERE personId = 1 
  AND create_time >= '2024-03-01' 
  AND create_time < '2024-04-01'
GROUP BY DATE_FORMAT(create_time, '%Y-%m'), consumption_type
ORDER BY month, consumption_type;
