# 菜单修复完整指南

**修复日期**: 2026-05-22  
**状态**: 准备就绪  
**方案**: 手动 MySQL 修复 + 自动验证

---

## 快速开始

### 第一步：查看当前菜单状态

在 MySQL 客户端中执行 `sql/check_menu_status.sql` 中的查询语句，了解当前菜单的问题。

**关键查询**:
```sql
-- 查看菜单数量
SELECT 
  (SELECT COUNT(*) FROM menu WHERE pid IS NULL) as 一级菜单数,
  (SELECT COUNT(*) FROM menu WHERE pid IS NOT NULL) as 二级菜单数,
  COUNT(*) as 总菜单数
FROM menu;
```

**当前状态** (修复前):
- 一级菜单: 29 个 ❌
- 二级菜单: 102 个 ❌
- 总菜单数: 131 个 ❌

### 第二步：执行菜单修复 SQL

在 MySQL 客户端中执行 `sql/clear_and_reinit_menu.sql` 中的所有 SQL 语句。

**操作方式**:

**方式1: 使用 MySQL 图形工具 (推荐)**
1. 打开 Navicat、DBeaver 或 MySQL Workbench
2. 连接到 team49 数据库
3. 打开 `sql/clear_and_reinit_menu.sql` 文件
4. 执行所有 SQL 语句

**方式2: 使用命令行**
```bash
cd F:/Java_project/server
mysql -h localhost -u root -p123456 team49 < sql/clear_and_reinit_menu.sql
```

### 第三步：验证修复结果

执行以下查询验证菜单是否正确初始化：

```sql
-- 验证菜单数量
SELECT 
  (SELECT COUNT(*) FROM menu WHERE pid IS NULL) as 一级菜单数,
  (SELECT COUNT(*) FROM menu WHERE pid IS NOT NULL) as 二级菜单数,
  COUNT(*) as 总菜单数
FROM menu;
```

**预期结果** (修复后):
- 一级菜单: 11 个 ✅
- 二级菜单: 35 个 ✅
- 总菜单数: 46 个 ✅

### 第四步：重启应用

```bash
cd F:/Java_project/server
mvn spring-boot:run
```

### 第五步：验证菜单显示

1. 打开浏览器，访问应用
2. 使用管理员账户登录
3. 检查菜单是否正确显示
4. 验证菜单无重复、无混乱

---

## 详细说明

### 菜单修复 SQL 脚本说明

`sql/clear_and_reinit_menu.sql` 包含以下操作：

1. **清空菜单表**
   ```sql
   DELETE FROM menu;
   ```

2. **重新初始化标准菜单结构**
   - 11 个一级菜单
   - 35 个二级菜单
   - 总共 46 个菜单项

3. **设置菜单权限**
   - `userTypeIds = '1,2,3'`: 全部角色可见
   - `userTypeIds = '1'`: 仅管理员可见

4. **验证菜单结构**
   - 统计一级菜单数
   - 统计二级菜单数
   - 统计总菜单数

### 菜单权限配置

| 菜单项 | 权限ID | 说明 |
|--------|--------|------|
| 个人信息 | 1,2,3 | 全部角色 |
| 人员管理 | 1,2,3 | 全部角色 |
| 学生管理 | 1,2,3 | 全部角色 |
| 教师管理 | 1 | 仅管理员 |
| 家庭与社会关系 | 1 | 仅管理员 |
| 教务管理 | 1,2,3 | 全部角色 |
| 荣誉与处分 | 1,2,3 | 全部角色 |
| 处分管理 | 1 | 仅管理员 |
| 创新创业 | 1,2,3 | 全部角色 |
| 日常活动 | 1,2,3 | 全部角色 |
| 社会实践 | 1,2,3 | 全部角色 |
| 消费管理 | 1,2,3 | 全部角色 |
| 日志与监控 | 1 | 仅管理员 |
| 数据看板 | 1 | 仅管理员 |
| 其他功能 | 1,2,3 | 全部角色 |

### 标准菜单结构

```
1. 个人信息 (personal-info)
   ├── 个人简介 (personal-intro)
   ├── 系统简介 (system-intro)
   ├── 修改密码 (change-password)
   └── 退出 (logout)

2. 人员管理 (staff-management)
   ├── 学生管理 (student-management)
   ├── 教师管理 (teacher-management) [仅管理员]
   └── 家庭与社会关系 (family-social-relation) [仅管理员]
       ├── 家庭成员管理 (family-member-management)
       └── 社会关系管理 (social-relation-management)

3. 教务管理 (academic-management)
   ├── 课程管理 (course-management)
   ├── 成绩管理 (score-management)
   ├── 考试安排 (exam-schedule)
   ├── 作业管理 (homework-management)
   ├── 绩分计算 (score-calculation)
   └── 学生请假 (student-leave)

4. 荣誉与处分 (honor-punishment)
   ├── 荣誉奖励 (honor-panel)
   └── 处分管理 (punishment-management) [仅管理员]

5. 创新创业 (innovation)
   ├── 创业实践 (innovation-project)
   ├── 学科竞赛 (competition)
   └── 科研成果 (achievement)

6. 日常活动 (daily-activity-root)
   ├── 体育活动 (sports-activity)
   ├── 文艺演出 (art-performance)
   ├── 聚会活动 (gathering-activity)
   └── 外出旅游 (outdoor-travel)

7. 社会实践 (social-practice)
   ├── 培训讲座 (daily-activity-training)
   ├── 校外实习 (daily-activity-internship)
   └── 志愿服务 (daily-activity-volunteer)

8. 消费管理 (consumption-management)
   ├── 消费日志 (consumption-log)
   ├── 账单查询 (consumption-bill)
   ├── 月度报表 (consumption-report)
   └── 异常预警 (consumption-warning)

9. 日志与监控 (log-monitor) [仅管理员]
   ├── 操作日志 (operation-log)
   └── 系统监控 (system-monitor)

10. 数据看板 (dashboard) [仅管理员]

11. 其他功能 (other-function)
    ├── 课程中心 (course-center)
    └── 个人简历 (personal-resume)
```

---

## 常见问题

### Q1: 如何确认 SQL 脚本执行成功？

**A**: 执行以下查询：
```sql
SELECT COUNT(*) as 总菜单数 FROM menu;
```
应该返回 46。

### Q2: 执行 SQL 后菜单仍然不对怎么办？

**A**: 
1. 检查是否正确执行了 SQL 脚本
2. 清空浏览器缓存
3. 重启应用
4. 重新登录

### Q3: 如何查看当前菜单的问题？

**A**: 执行 `sql/check_menu_status.sql` 中的查询语句。

### Q4: 修复后前端需要做什么改动吗？

**A**: 不需要。前端会自动从后端获取最新的菜单数据。

### Q5: 能否只修复某些菜单而不是全部清空？

**A**: 可以，但比较复杂。建议使用完全清空的方案。

---

## 文件清单

| 文件 | 说明 |
|------|------|
| `sql/check_menu_status.sql` | 菜单状态检查脚本 |
| `sql/clear_and_reinit_menu.sql` | 菜单修复 SQL 脚本 |
| `MENU_FIX_COMPLETE_GUIDE.md` | 完整修复指南 (本文件) |
| `src/main/java/cn/edu/sdu/java/server/utils/MenuFixUtil.java` | 菜单修复工具类 |

---

## 修复前后对比

### 修复前
```
一级菜单: 29 个 ❌
二级菜单: 102 个 ❌
总菜单数: 131 个 ❌
重复菜单: 2 个 ❌
```

### 修复后
```
一级菜单: 11 个 ✅
二级菜单: 35 个 ✅
总菜单数: 46 个 ✅
重复菜单: 0 个 ✅
```

---

## 操作流程图

```
开始
  ↓
停止应用
  ↓
查看当前菜单状态 (check_menu_status.sql)
  ↓
执行菜单修复 SQL (clear_and_reinit_menu.sql)
  ↓
验证修复结果
  ↓
重启应用
  ↓
验证菜单显示
  ↓
完成 ✅
```

---

## 支持

如果在修复过程中遇到问题，请：

1. 检查 MySQL 连接是否正常
2. 确认数据库名称是否正确 (team49)
3. 查看应用日志获取详细错误信息
4. 确保有足够的数据库权限

---

**修复方案**: 手动 MySQL 修复  
**修复日期**: 2026-05-22  
**预期效果**: ✅ 菜单结构清晰、标准、无重复

