-- ============================================
-- 社会关系表创建脚本
-- 数据库: java_2_49
-- ============================================

-- 创建社会关系表
CREATE TABLE IF NOT EXISTS social_relation (
    relation_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '关系ID',
    person_id INT COMMENT '学生ID (外键)',
    relation_type VARCHAR(30) COMMENT '关系类型',
    name VARCHAR(30) COMMENT '关系人名称',
    gender VARCHAR(10) COMMENT '性别',
    phone VARCHAR(20) COMMENT '电话',
    age INT COMMENT '年龄',
    remark VARCHAR(200) COMMENT '备注',
    INDEX idx_person_id (person_id),
    CONSTRAINT fk_social_relation_student FOREIGN KEY (person_id) REFERENCES student(person_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社会关系表';

-- ============================================
-- 创建完成
-- ============================================
