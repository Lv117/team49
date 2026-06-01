# 社会关系添加失败问题分析与解决方案

## 问题诊断

社会关系无法添加，而家庭成员可以正常添加。通过对比两个服务的实现，发现了关键差异。

---

## 根本原因分析

### 1. 参数名称不一致

**FamilyMemberService (工作正常):**
```java
public DataResponse familyMemberSave(DataRequest dataRequest) {
    Map<String, Object> form = dataRequest.getMap("form");
    Integer memberId = CommonMethod.getInteger(form, "memberId");
    Integer studentId = CommonMethod.getInteger(form, "studentId");  // ✅ 使用 studentId
    ...
}
```

**StudentService.socialRelationSave (存在问题):**
```java
public DataResponse socialRelationSave(DataRequest dataRequest) {
    Map<String, Object> form = dataRequest.getMap("form");
    Integer personId = CommonMethod.getInteger(form, "personId");   // ❌ 使用 personId
    Integer relationId = CommonMethod.getInteger(form, "relationId");
    ...
}
```

### 2. 问题所在

- **家庭成员**: 前端发送 `studentId` → 后端接收 `studentId` ✅ 匹配
- **社会关系**: 前端可能发送 `studentId` → 后端期望 `personId` ❌ 不匹配

当前端发送的参数名为 `studentId` 时，`CommonMethod.getInteger(form, "personId")` 会返回 `null`，导致后续逻辑失败。

---

## 解决方案

### 方案 A: 修改 StudentService (推荐)

将 `StudentService.socialRelationSave` 改为使用 `studentId`，与 `familyMemberSave` 保持一致。

**修改位置**: `src/main/java/cn/edu/sdu/java/server/services/StudentService.java` 第 711-749 行

**修改内容**:

```java
public DataResponse socialRelationSave(DataRequest dataRequest) {
    Map<String, Object> form = dataRequest.getMap("form");
    Integer relationId = CommonMethod.getInteger(form, "relationId");
    Integer studentId = CommonMethod.getInteger(form, "studentId");  // 改为 studentId
    
    // 学生只能添加自己的社会关系
    if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
        Integer currentPersonId = CommonMethod.getPersonId();
        if (currentPersonId == null || currentPersonId <= 0) {
            throw new BusinessException(ErrorCodes.AUTH_FAILED, "当前登录状态无效，请重新登录");
        }
        studentId = currentPersonId;
    }
    
    if (studentId == null || studentId <= 0) {
        throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "学生ID不能为空");
    }
    
    teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, "教师仅可管理本人授课学生的社会关系");
    
    Optional<Student> studentOp = studentRepository.findById(studentId);
    if (studentOp.isEmpty()) {
        throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "学生不存在");
    }
    
    SocialRelation relation;
    if (relationId != null && relationId > 0) {
        Optional<SocialRelation> op = socialRelationRepository.findById(relationId);
        if (op.isPresent()) {
            relation = op.get();
        } else {
            throw new BusinessException(ErrorCodes.SOCIAL_RELATION_NOT_FOUND, "社会关系不存在");
        }
    } else {
        relation = new SocialRelation();
        relation.setStudent(studentOp.get());
    }
    
    relation.setRelationType(CommonMethod.getString(form, "relationType"));
    relation.setName(CommonMethod.getString(form, "name"));
    relation.setGender(CommonMethod.getString(form, "gender"));
    relation.setPhone(CommonMethod.getString(form, "phone"));
    relation.setAge(CommonMethod.getInteger(form, "age"));
    relation.setRemark(CommonMethod.getString(form, "remark"));
    
    socialRelationRepository.save(relation);
    return CommonMethod.getReturnMessageOK();
}
```

### 方案 B: 修改前端发送参数

如果前端已经按照某种约定发送 `personId`，则需要修改前端代码以发送 `studentId`。

---

## 对比总结

| 方面 | FamilyMember | SocialRelation (当前) | SocialRelation (修复后) |
|------|-------------|-------------------|-------------------|
| 参数名 | `studentId` | `personId` | `studentId` |
| 权限检查 | ✅ 完整 | ⚠️ 复杂 | ✅ 完整 |
| 代码一致性 | ✅ 清晰 | ❌ 混乱 | ✅ 一致 |
| 易维护性 | ✅ 高 | ❌ 低 | ✅ 高 |

---

## 实施步骤

1. **备份原文件**
   ```bash
   cp src/main/java/cn/edu/sdu/java/server/services/StudentService.java \
      src/main/java/cn/edu/sdu/java/server/services/StudentService.java.bak
   ```

2. **应用修复** (见下面的代码修改)

3. **编译验证**
   ```bash
   mvn clean compile
   ```

4. **测试**
   - 添加新的社会关系记录
   - 编辑现有的社会关系记录
   - 删除社会关系记录
   - 查询社会关系列表

---

## 关键改进点

1. **参数一致性**: 使用 `studentId` 而不是 `personId`
2. **权限检查**: 简化权限检查逻辑，与 FamilyMemberService 保持一致
3. **错误处理**: 更清晰的错误消息
4. **代码可维护性**: 两个服务的实现模式一致，便于后续维护

