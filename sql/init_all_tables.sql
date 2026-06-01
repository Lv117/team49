-- ============================================
-- 完整数据库初始化脚本
-- 数据库: java_2_49
-- 创建所有必需的表
-- ============================================

-- 1. 用户类型表
CREATE TABLE IF NOT EXISTS user_type (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户类型ID',
    name VARCHAR(20) COMMENT '角色名称: ROLE_ADMIN, ROLE_STUDENT, ROLE_TEACHER'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户类型表';

-- 2. 数据字典表
CREATE TABLE IF NOT EXISTS dictionary (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '字典ID',
    pid INT COMMENT '父节点ID',
    value VARCHAR(40) COMMENT '字典值',
    label VARCHAR(40) COMMENT '字典名'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典表';

-- 3. 用户表
CREATE TABLE IF NOT EXISTS user (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '电话',
    user_type_id INT COMMENT '用户类型ID',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_user_type_id (user_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 4. 教师表
CREATE TABLE IF NOT EXISTS teacher (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '教师ID',
    user_id INT NOT NULL COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '教师名称',
    employee_id VARCHAR(20) UNIQUE COMMENT '工号',
    department VARCHAR(50) COMMENT '部门',
    phone VARCHAR(20) COMMENT '电话',
    email VARCHAR(100) COMMENT '邮箱',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_employee_id (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师表';

-- 5. 学生表
CREATE TABLE IF NOT EXISTS student (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '学生ID',
    user_id INT NOT NULL COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '学生名称',
    student_num VARCHAR(20) NOT NULL UNIQUE COMMENT '学号',
    class_name VARCHAR(50) COMMENT '班级',
    major VARCHAR(50) COMMENT '专业',
    phone VARCHAR(20) COMMENT '电话',
    email VARCHAR(100) COMMENT '邮箱',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_student_num (student_num)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生表';

-- 6. 课程表
CREATE TABLE IF NOT EXISTS course (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '课程ID',
    name VARCHAR(100) NOT NULL COMMENT '课程名称',
    code VARCHAR(20) UNIQUE COMMENT '课程代码',
    description TEXT COMMENT '课程描述',
    credits INT COMMENT '学分',
    hours INT COMMENT '学时',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';

-- 7. 课程选择表
CREATE TABLE IF NOT EXISTS course_selection (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '选课ID',
    student_id INT NOT NULL COMMENT '学生ID',
    course_id INT NOT NULL COMMENT '课程ID',
    teacher_id INT COMMENT '教师ID',
    selection_date DATETIME COMMENT '选课日期',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id),
    INDEX idx_teacher_id (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程选择表';

-- 8. 课程教学表
CREATE TABLE IF NOT EXISTS course_teaching (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '教学ID',
    course_id INT NOT NULL COMMENT '课程ID',
    teacher_id INT NOT NULL COMMENT '教师ID',
    semester VARCHAR(20) COMMENT '学期',
    classroom VARCHAR(50) COMMENT '教室',
    schedule VARCHAR(100) COMMENT '上课时间',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_course_id (course_id),
    INDEX idx_teacher_id (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程教学表';

-- 9. 课程材料表
CREATE TABLE IF NOT EXISTS course_material (
    material_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '资料ID',
    course_id INT NOT NULL COMMENT '课程ID',
    title VARCHAR(100) COMMENT '资料标题',
    description TEXT COMMENT '资料描述',
    file_path VARCHAR(255) COMMENT '文件路径',
    file_size INT COMMENT '文件大小',
    upload_date DATETIME COMMENT '上传日期',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程材料表';

-- 10. 作业表
CREATE TABLE IF NOT EXISTS homework (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '作业ID',
    course_id INT NOT NULL COMMENT '课程ID',
    title VARCHAR(100) NOT NULL COMMENT '作业标题',
    description TEXT COMMENT '作业描述',
    due_date DATETIME COMMENT '截止日期',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_course_id (course_id),
    INDEX idx_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表';

-- 11. 作业提交表
CREATE TABLE IF NOT EXISTS homework_submission (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '提交ID',
    homework_id INT NOT NULL COMMENT '作业ID',
    student_id INT NOT NULL COMMENT '学生ID',
    submission_date DATETIME COMMENT '提交日期',
    file_path VARCHAR(255) COMMENT '文件路径',
    score DECIMAL(5,2) COMMENT '分数',
    comment TEXT COMMENT '评语',
    status VARCHAR(20) DEFAULT 'submitted' COMMENT '状态',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_homework_id (homework_id),
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交表';

-- 12. 考试安排表
CREATE TABLE IF NOT EXISTS exam_schedule (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '考试安排ID',
    course_id INT NOT NULL COMMENT '课程ID',
    exam_date DATETIME COMMENT '考试日期',
    classroom VARCHAR(50) COMMENT '考试教室',
    duration INT COMMENT '考试时长(分钟)',
    total_score INT COMMENT '总分',
    status VARCHAR(20) DEFAULT 'scheduled' COMMENT '状态',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_course_id (course_id),
    INDEX idx_exam_date (exam_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试安排表';

-- 13. 学生考试安排表
CREATE TABLE IF NOT EXISTS student_exam_schedule (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '学生考试安排ID',
    exam_schedule_id INT NOT NULL COMMENT '考试安排ID',
    student_id INT NOT NULL COMMENT '学生ID',
    seat_number VARCHAR(20) COMMENT '座位号',
    status VARCHAR(20) DEFAULT 'scheduled' COMMENT '状态',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_exam_schedule_id (exam_schedule_id),
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生考试安排表';

-- 14. 成绩表
CREATE TABLE IF NOT EXISTS score (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '成绩ID',
    student_id INT NOT NULL COMMENT '学生ID',
    course_id INT NOT NULL COMMENT '课程ID',
    exam_score DECIMAL(5,2) COMMENT '考试成绩',
    usual_score DECIMAL(5,2) COMMENT '平时成绩',
    final_score DECIMAL(5,2) COMMENT '最终成绩',
    status VARCHAR(20) DEFAULT 'recorded' COMMENT '状态',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩表';

-- 15. 考勤表
CREATE TABLE IF NOT EXISTS attendance (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '考勤ID',
    student_id INT NOT NULL COMMENT '学生ID',
    course_id INT NOT NULL COMMENT '课程ID',
    attendance_date DATE COMMENT '考勤日期',
    status VARCHAR(20) COMMENT '状态: present(出席), absent(缺席), late(迟到), leave(请假)',
    remark TEXT COMMENT '备注',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id),
    INDEX idx_attendance_date (attendance_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤表';

-- 16. 学生请假表
CREATE TABLE IF NOT EXISTS student_leave (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '请假ID',
    student_id INT NOT NULL COMMENT '学生ID',
    leave_type VARCHAR(20) COMMENT '请假类型: sick(病假), personal(事假), other(其他)',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    reason TEXT COMMENT '请假原因',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending(待审批), approved(已批准), rejected(已拒绝)',
    approver_id INT COMMENT '审批人ID',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_student_id (student_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生请假表';

-- 17. 处分表
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

-- 18. 荣誉表
CREATE TABLE IF NOT EXISTS honor (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '荣誉ID',
    student_id INT NOT NULL COMMENT '学生ID',
    honor_type VARCHAR(50) COMMENT '荣誉类型',
    honor_name VARCHAR(100) COMMENT '荣誉名称',
    award_date DATE COMMENT '获奖日期',
    issuer VARCHAR(100) COMMENT '颁发机构',
    certificate_path VARCHAR(255) COMMENT '证书路径',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='荣誉表';

-- 19. 日常活动表
CREATE TABLE IF NOT EXISTS daily_activity (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '活动ID',
    student_id INT NOT NULL COMMENT '学生ID',
    activity_type VARCHAR(50) COMMENT '活动类型',
    activity_name VARCHAR(100) COMMENT '活动名称',
    activity_date DATE COMMENT '活动日期',
    description TEXT COMMENT '活动描述',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_student_id (student_id),
    INDEX idx_activity_date (activity_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常活动表';

-- 20. 学生发展表
CREATE TABLE IF NOT EXISTS student_development (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '发展记录ID',
    student_id INT NOT NULL COMMENT '学生ID',
    development_type VARCHAR(50) COMMENT '发展类型',
    description TEXT COMMENT '发展描述',
    record_date DATE COMMENT '记录日期',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生发展表';

-- 21. 创新项目表
CREATE TABLE IF NOT EXISTS innovation_project (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '项目ID',
    student_id INT NOT NULL COMMENT '学生ID',
    project_name VARCHAR(100) COMMENT '项目名称',
    description TEXT COMMENT '项目描述',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    status VARCHAR(20) DEFAULT 'ongoing' COMMENT '状态',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创新项目表';

-- 22. 社会关系表
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

-- 23. 家庭成员表
CREATE TABLE IF NOT EXISTS family_member (
    member_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '家庭成员ID',
    person_id INT COMMENT '学生ID (外键)',
    relation VARCHAR(10) COMMENT '关系',
    name VARCHAR(30) COMMENT '成员名称',
    gender VARCHAR(10) COMMENT '性别',
    age INT COMMENT '年龄',
    unit VARCHAR(50) COMMENT '工作单位',
    INDEX idx_person_id (person_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭成员表';

-- 24. 人员表
CREATE TABLE IF NOT EXISTS person (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '人员ID',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    id_number VARCHAR(20) UNIQUE COMMENT '身份证号',
    phone VARCHAR(20) COMMENT '电话',
    email VARCHAR(100) COMMENT '邮箱',
    address TEXT COMMENT '地址',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_id_number (id_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人员表';

-- 25. 费用表
CREATE TABLE IF NOT EXISTS fee (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '费用ID',
    student_id INT NOT NULL COMMENT '学生ID',
    fee_type VARCHAR(50) COMMENT '费用类型',
    amount DECIMAL(10,2) COMMENT '金额',
    due_date DATE COMMENT '截止日期',
    payment_date DATE COMMENT '支付日期',
    status VARCHAR(20) DEFAULT 'unpaid' COMMENT '状态: unpaid(未支付), paid(已支付)',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_student_id (student_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用表';

-- 26. 审批记录表
CREATE TABLE IF NOT EXISTS approval_record (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '审批记录ID',
    approval_type VARCHAR(50) COMMENT '审批类型',
    applicant_id INT COMMENT '申请人ID',
    approver_id INT COMMENT '审批人ID',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending(待审批), approved(已批准), rejected(已拒绝)',
    reason TEXT COMMENT '审批原因',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_applicant_id (applicant_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批记录表';

-- 27. 修改日志表
CREATE TABLE IF NOT EXISTS modify_log (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    table_name VARCHAR(50) COMMENT '表名',
    record_id INT COMMENT '记录ID',
    operation VARCHAR(20) COMMENT '操作: INSERT, UPDATE, DELETE',
    old_value TEXT COMMENT '旧值',
    new_value TEXT COMMENT '新值',
    operator_id INT COMMENT '操作人ID',
    create_time DATETIME COMMENT '创建时间',
    INDEX idx_table_name (table_name),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='修改日志表';

-- 28. 请求日志表
CREATE TABLE IF NOT EXISTS request_log (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id INT COMMENT '用户ID',
    request_url VARCHAR(255) COMMENT '请求URL',
    request_method VARCHAR(10) COMMENT '请求方法',
    request_params TEXT COMMENT '请求参数',
    response_code INT COMMENT '响应码',
    create_time DATETIME COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请求日志表';

-- 29. 系统信息表
CREATE TABLE IF NOT EXISTS system_info (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '系统信息ID',
    info_key VARCHAR(50) UNIQUE COMMENT '信息键',
    info_value TEXT COMMENT '信息值',
    description TEXT COMMENT '描述',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统信息表';

-- 30. 学生统计表
CREATE TABLE IF NOT EXISTS student_statistics (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '统计ID',
    student_id INT NOT NULL COMMENT '学生ID',
    total_courses INT COMMENT '总课程数',
    passed_courses INT COMMENT '通过课程数',
    failed_courses INT COMMENT '不通过课程数',
    average_score DECIMAL(5,2) COMMENT '平均成绩',
    total_credits INT COMMENT '总学分',
    earned_credits INT COMMENT '已获学分',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生统计表';

-- 31. 统计日表
CREATE TABLE IF NOT EXISTS statistics_day (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '统计ID',
    statistics_date DATE COMMENT '统计日期',
    total_students INT COMMENT '学生总数',
    total_teachers INT COMMENT '教师总数',
    total_courses INT COMMENT '课程总数',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_statistics_date (statistics_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统计日表';

-- 32. 菜单表
CREATE TABLE IF NOT EXISTS menu (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '菜单ID',
    parent_id INT COMMENT '父菜单ID',
    menu_name VARCHAR(50) COMMENT '菜单名称',
    menu_url VARCHAR(255) COMMENT '菜单URL',
    menu_icon VARCHAR(50) COMMENT '菜单图标',
    sort_order INT COMMENT '排序',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

-- ============================================
-- 初始化完成
-- ============================================
