-- 处分管理表
CREATE TABLE IF NOT EXISTS punishment (
    punishment_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '处分ID',
    student_id INT NOT NULL COMMENT '学生ID',
    student_name VARCHAR(50) COMMENT '学生姓名',
    student_num VARCHAR(20) COMMENT '学生学号',
    punishment_type VARCHAR(30) NOT NULL COMMENT '处分类型：warning(警告)、serious_warning(严重警告)、demerit(记过)、probation(留校察看)、expulsion(开除)',
    punishment_date DATE COMMENT '处分日期',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '处分状态：pending(待处理)、active(已生效)、revoked(已撤销)',
    reason TEXT COMMENT '违纪原因',
    remark TEXT COMMENT '备注',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_student_id (student_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生处分记录表';
