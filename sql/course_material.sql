-- 课程资料表
-- 创建时间: 2026-04-27

CREATE TABLE IF NOT EXISTS course_material (
    material_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '资料ID',
    course_id INT COMMENT '课程ID',
    course_name VARCHAR(100) COMMENT '课程名称',
    material_name VARCHAR(200) NOT NULL COMMENT '资料名称',
    course_type VARCHAR(50) COMMENT '资料类型(textbook/courseware/reference)',
    file_path VARCHAR(500) COMMENT '文件路径',
    file_name VARCHAR(200) COMMENT '文件名',
    file_size BIGINT COMMENT '文件大小(字节)',
    description TEXT COMMENT '描述',
    uploader_id INT COMMENT '上传者ID',
    uploader_name VARCHAR(50) COMMENT '上传者姓名',
    upload_time DATETIME COMMENT '上传时间',
    KEY idx_course_id (course_id),
    KEY idx_course_type (course_type),
    KEY idx_uploader_id (uploader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程资料表';