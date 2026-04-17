# 头像上传功能实现计划

## Context
用户需要在学生管理系统中添加头像上传功能。这是一个课程设计作业,用户量不大。需要完成后端实现并提供前端对接文档。

## 技术方案选择

### 存储方案:文件系统存储(推荐)
- **原因**: 
  - 数据库负担小,只存文件路径
  - 读取效率高
  - 适合课程设计场景
  - 项目已有 `/Java_project/` 作为文件存储目录

### 实现方式
在 `Person` 表中添加 `photoPath` 字段(VARCHAR类型),存储相对路径如 `avatars/uuid_xxx.jpg`

## 实现步骤

### 1. 修改数据库实体 Person
**文件**: `d:\JavaProject\server\src\main\java\cn\edu\sdu\java\server\models\Person.java`

添加字段:
```java
@Column(name = "photo_path", length = 255, nullable = true)
private String photoPath;  // 头像文件路径
```

### 2. 创建头像上传专用接口
**文件**: `d:\JavaProject\server\src\main\java\cn\edu\sdu\java\server\controllers\AvatarController.java`(新建)

提供以下接口:
- `POST /api/avatar/upload` - 上传头像(Multipart方式)
- `GET /api/avatar/{personId}` - 获取头像
- `DELETE /api/avatar` - 删除头像

### 3. 实现头像服务层
**文件**: `d:\JavaProject\server\src\main\java\cn\edu\sdu\java\server\services\AvatarService.java`(新建)

核心功能:
- 文件验证(格式:jpg/png/gif,大小:≤5MB)
- UUID生成唯一文件名
- 保存到 `attach.folder + "avatars/"` 目录
- 更新Person表的photoPath字段
- 旧头像文件清理

### 4. 配置静态资源访问
**文件**: `d:\JavaProject\server\src\main\java\cn\edu\sdu\java\server\configs\WebConfig.java`(新建或修改)

配置 `/avatars/**` 路径映射到实际文件目录

### 5. 安全性增强
- 权限控制:仅允许STUDENT和ADMIN上传自己的头像
- 文件类型白名单验证
- 文件大小限制
- 防止路径遍历攻击

## 关键文件清单

### 需要修改的文件
1. `models/Person.java` - 添加photoPath字段
2. `application.yml` - 可选:调整上传大小限制

### 需要新建的文件
1. `controllers/AvatarController.java` - 头像控制器
2. `services/AvatarService.java` - 头像服务
3. `configs/WebConfig.java` - Web配置(静态资源映射)

### 不需要修改的文件
- 现有的BaseController/BaseService保持不变(用于兼容)

## 前端对接API文档

### API 1: 上传头像
**接口**: `POST /api/avatar/upload`  
**认证**: 需要JWT Token (Bearer)  
**权限**: STUDENT, ADMIN  
**Content-Type**: `multipart/form-data`

**请求参数**:
- `file`: 图片文件 (必填)
  - 支持格式: jpg, jpeg, png, gif
  - 最大大小: 5MB

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "avatarUrl": "/avatars/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg"
  },
  "msg": "success"
}
```

**错误响应**:
```json
{
  "code": 1,
  "data": null,
  "msg": "文件格式不支持,仅支持jpg/png/gif"
}
```

---

### API 2: 获取头像
**接口**: `GET /api/avatar/{personId}`  
**认证**: 不需要(公开访问)  

**路径参数**:
- `personId`: 人员ID (整数)

**响应**:
- 成功: 返回图片二进制流 (Content-Type: image/jpeg 或 image/png)
- 失败: HTTP 404 (未设置头像)

**使用示例**:
```html
<img src="http://localhost:22223/api/avatar/123" alt="头像" />
```

---

### API 3: 删除头像
**接口**: `DELETE /api/avatar`  
**认证**: 需要JWT Token (Bearer)  
**权限**: STUDENT(仅自己), ADMIN(所有人)  
**Content-Type**: `application/json`

**请求体**:
```json
{
  "personId": 123
}
```

**响应示例**:
```json
{
  "code": 0,
  "data": null,
  "msg": "success"
}
```

---

### API 4: 获取当前用户信息(含头像)
**接口**: `POST /api/auth/getUserInfo`  
**认证**: 需要JWT Token (Bearer)  

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "personId": 123,
    "name": "张三",
    "avatarUrl": "/avatars/xxx.jpg",
    "fullAvatarUrl": "http://localhost:22223/avatars/xxx.jpg"
  },
  "msg": "success"
}
```

## 前端集成示例

### Vue3 示例
```javascript
// 上传头像
async function uploadAvatar(file) {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await fetch('http://localhost:22223/api/avatar/upload', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });
  
  const result = await response.json();
  if (result.code === 0) {
    avatarUrl.value = result.data.avatarUrl;
  }
}

// 显示头像
<img :src="`http://localhost:22223${avatarUrl}`" alt="头像" />
```

### React 示例
```jsx
const [avatar, setAvatar] = useState(null);

const handleUpload = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  
  const res = await axios.post(
    'http://localhost:22223/api/avatar/upload',
    formData,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    }
  );
  
  if (res.data.code === 0) {
    setAvatar(res.data.data.avatarUrl);
  }
};

// 显示
<img src={`http://localhost:22223${avatar}`} alt="头像" />
```

## 测试验证

### 后端测试
1. 启动Spring Boot应用
2. 使用Postman/curl测试上传接口
3. 验证文件是否正确保存到 `/Java_project/avatars/`
4. 验证数据库中photoPath字段是否更新
5. 测试获取头像接口返回正确的图片

### 前端联调
1. 确认CORS跨域配置正确
2. 测试JWT认证是否正常
3. 验证文件上传进度显示
4. 测试各种错误场景(超大文件、错误格式等)

## 注意事项

1. **文件存储路径**: 确保 `/Java_project/avatars/` 目录存在且有写权限
2. **文件大小**: 默认限制5MB,可在application.yml中调整
3. **旧数据兼容**: 原有的Person.photo(BLOB)字段保留,新代码优先使用photoPath
4. **安全性**: 严格验证文件类型,防止上传恶意文件
5. **性能**: 大量用户时可考虑CDN加速或对象存储
