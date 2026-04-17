# 头像上传功能 - 前端对接文档

## 📋 概述

后端已完成头像上传功能的实现,提供REST API供前端调用。

**技术选型**:
- 存储方案: 文件系统存储(在Person表添加photoPath字段)
- 文件存储路径: `/Java_project/avatars/` (Windows环境)
- 支持格式: jpg, jpeg, png, gif
- 文件大小限制: 最大5MB

---

## 🔌 API接口列表

### 1. 上传头像

**接口地址**: `POST http://localhost:22223/api/avatar/upload`

**认证要求**: 需要JWT Token  
**权限要求**: STUDENT, TEACHER, ADMIN  
**Content-Type**: `multipart/form-data`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | 图片文件(jpg/jpeg/png/gif),最大5MB |

**成功响应** (HTTP 200):
```json
{
  "code": 0,
  "data": {
    "avatarUrl": "/avatars/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg"
  },
  "msg": "success"
}
```

**错误响应** (HTTP 400):
```json
{
  "code": 1,
  "data": null,
  "msg": "文件格式不支持,仅支持jpg/png/gif"
}
```

**可能的错误消息**:
- `文件不能为空`
- `文件大小不能超过5MB`
- `文件格式不支持,仅支持jpg/png/gif`
- `用户不存在`

---

### 2. 获取头像

**接口地址**: `GET http://localhost:22223/api/avatar/{personId}`

**认证要求**: 不需要(公开访问)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| personId | Integer | 是 | 人员ID |

**成功响应** (HTTP 200):
- Content-Type: `image/jpeg` 或 `image/png` 或 `image/gif`
- Body: 图片二进制数据

**失败响应**:
- HTTP 404: 该用户未设置头像
- HTTP 500: 服务器错误

**使用示例**:
```html
<!-- HTML直接使用 -->
<img src="http://localhost:22223/api/avatar/123" alt="头像" />

<!-- JavaFX加载 -->
Image image = new Image("http://localhost:22223/api/avatar/" + personId);
imageView.setImage(image);
```

---

### 3. 删除头像

**接口地址**: `DELETE http://localhost:22223/api/avatar`

**认证要求**: 需要JWT Token  
**权限要求**: STUDENT(仅自己), ADMIN(所有人)  
**Content-Type**: `application/json`

**请求体**:
```json
{
  "personId": 123
}
```

**成功响应** (HTTP 200):
```json
{
  "code": 0,
  "data": null,
  "msg": "success"
}
```

**错误响应** (HTTP 400):
```json
{
  "code": 1,
  "data": null,
  "msg": "用户不存在"
}
```

---

## 💻 前端调用示例

### JavaFX 示例

#### 上传头像

```java
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class AvatarUploader {

    /**
     * 上传头像
     * @param imageFile 选择的图片文件
     * @param token JWT Token
     * @return 头像URL,失败返回null
     */
    public static String uploadAvatar(File imageFile, String token) {
        try {
            URL url = new URL("http://localhost:22223/api/avatar/upload");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // 设置认证头
            conn.setRequestProperty("Authorization", "Bearer " + token);

            // 生成boundary
            String boundary = UUID.randomUUID().toString();
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            // 构建请求体
            OutputStream outputStream = conn.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

            // 添加文件部分
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                  .append(imageFile.getName()).append("\"").append("\r\n");
            writer.append("Content-Type: ").append(URLConnection.guessContentTypeFromName(imageFile.getName())).append("\r\n");
            writer.append("\r\n").flush();

            // 写入文件内容
            FileInputStream inputStream = new FileInputStream(imageFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            inputStream.close();

            writer.append("\r\n").flush();
            writer.append("--").append(boundary).append("--").append("\r\n");
            writer.flush();

            // 获取响应
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // 解析JSON响应,提取avatarUrl
                // 这里简化处理,实际应该使用JSON库
                String jsonResponse = response.toString();
                // 假设响应: {"code":0,"data":{"avatarUrl":"/avatars/xxx.jpg"},"msg":"success"}
                int start = jsonResponse.indexOf("\"avatarUrl\":\"") + 13;
                int end = jsonResponse.indexOf("\"", start);
                String avatarUrl = jsonResponse.substring(start, end);

                return avatarUrl;
            } else {
                System.err.println("上传失败,响应码: " + responseCode);
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
```

#### 显示头像

```java
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class AvatarDisplay {

    /**
     * 加载并显示头像
     * @param personId 人员ID
     * @param imageView 显示头像的ImageView控件
     */
    public static void displayAvatar(Integer personId, ImageView imageView) {
        if (personId == null) {
            return;
        }

        String avatarUrl = "http://localhost:22223/api/avatar/" + personId;

        // 加载图片,如果失败会显示默认图片
        Image image = new Image(avatarUrl, true);

        // 监听加载状态
        image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
            if (newProgress.doubleValue() == 1.0) {
                if (image.isError()) {
                    // 加载失败,使用默认头像
                    System.out.println("头像加载失败,使用默认头像");
                    // imageView.setImage(new Image("/default-avatar.png"));
                } else {
                    // 加载成功
                    imageView.setImage(image);
                }
            }
        });
    }
}
```

---

### Web前端示例 (Vue3 / React)

#### Vue3 示例

```vue
<template>
  <div class="avatar-container">
    <img :src="avatarUrl" alt="头像" @error="handleImageError" />
    <input type="file" accept="image/*" @change="handleFileSelect" />
    <button @click="uploadAvatar" :disabled="!selectedFile">上传</button>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const personId = ref(123); // 当前用户ID
const selectedFile = ref(null);
const avatarUrl = ref('');

// 计算完整的头像URL
const fullAvatarUrl = computed(() => {
  return `http://localhost:22223/api/avatar/${personId.value}`;
});

// 文件选择
const handleFileSelect = (event) => {
  const file = event.target.files[0];
  if (!file) return;

  // 验证文件大小
  if (file.size > 5 * 1024 * 1024) {
    alert('文件大小不能超过5MB');
    return;
  }

  // 验证文件类型
  const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
  if (!allowedTypes.includes(file.type)) {
    alert('仅支持jpg/png/gif格式');
    return;
  }

  selectedFile.value = file;
};

// 上传头像
const uploadAvatar = async () => {
  if (!selectedFile.value) return;

  const formData = new FormData();
  formData.append('file', selectedFile.value);

  try {
    const response = await fetch('http://localhost:22223/api/avatar/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: formData
    });

    const result = await response.json();

    if (result.code === 0) {
      alert('上传成功');
      // 刷新头像显示
      avatarUrl.value = fullAvatarUrl.value + '?t=' + Date.now(); // 添加时间戳防止缓存
      selectedFile.value = null;
    } else {
      alert('上传失败: ' + result.msg);
    }
  } catch (error) {
    console.error('上传失败:', error);
    alert('上传失败,请检查网络连接');
  }
};

// 图片加载失败处理
const handleImageError = (event) => {
  event.target.src = '/default-avatar.png'; // 使用默认头像
};
</script>
```

#### React 示例

```jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

const AvatarUpload = ({ personId }) => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState('');
  const [uploading, setUploading] = useState(false);

  const avatarApiUrl = `http://localhost:22223/api/avatar/${personId}`;

  // 文件选择处理
  const handleFileSelect = (event) => {
    const file = event.target.files[0];
    if (!file) return;

    // 验证文件大小
    if (file.size > 5 * 1024 * 1024) {
      alert('文件大小不能超过5MB');
      return;
    }

    // 验证文件类型
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
    if (!allowedTypes.includes(file.type)) {
      alert('仅支持jpg/png/gif格式');
      return;
    }

    setSelectedFile(file);

    // 创建预览
    const reader = new FileReader();
    reader.onloadend = () => {
      setPreviewUrl(reader.result);
    };
    reader.readAsDataURL(file);
  };

  // 上传头像
  const handleUpload = async () => {
    if (!selectedFile) return;

    setUploading(true);

    const formData = new FormData();
    formData.append('file', selectedFile);

    try {
      const response = await axios.post(
        'http://localhost:22223/api/avatar/upload',
        formData,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'Content-Type': 'multipart/form-data'
          }
        }
      );

      if (response.data.code === 0) {
        alert('上传成功');
        setSelectedFile(null);
        setPreviewUrl('');
        // 刷新头像
        setPreviewUrl(avatarApiUrl + '?t=' + Date.now());
      } else {
        alert('上传失败: ' + response.data.msg);
      }
    } catch (error) {
      console.error('上传失败:', error);
      alert('上传失败: ' + (error.response?.data?.msg || '网络错误'));
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="avatar-upload">
      <img
        src={previewUrl || avatarApiUrl}
        alt="头像"
        onError={(e) => {
          e.target.src = '/default-avatar.png';
        }}
      />
      <input
        type="file"
        accept="image/*"
        onChange={handleFileSelect}
      />
      <button onClick={handleUpload} disabled={!selectedFile || uploading}>
        {uploading ? '上传中...' : '上传'}
      </button>
    </div>
  );
};

export default AvatarUpload;
```

---

## ⚠️ 注意事项

### 1. 跨域配置
后端已配置CORS允许所有来源访问:
```java
@CrossOrigin(origins = "*", maxAge = 3600)
```

### 2. JWT Token
- 上传和删除接口需要携带JWT Token
- Token放在HTTP Header中: `Authorization: Bearer <token>`
- 获取头像接口不需要Token(公开访问)

### 3. 文件限制
- **大小**: 最大5MB
- **格式**: 仅支持 jpg, jpeg, png, gif
- 建议在上传前在前端进行预验证

### 4. 缓存问题
头像更新后,浏览器可能会缓存旧图片。解决方法:
- 在URL后添加时间戳: `/api/avatar/123?t=1234567890`
- 或清除浏览器缓存

### 5. 错误处理
建议在前端处理以下常见错误:
- 网络错误
- 文件过大
- 文件格式不支持
- 未登录(Token过期)
- 权限不足

### 6. 用户体验优化建议
- 上传前显示图片预览
- 显示上传进度
- 上传成功后立即刷新头像显示
- 提供默认头像(当用户未设置头像时)
- 压缩图片后再上传(减小文件大小)

---

## 🧪 测试清单

前端开发完成后,请测试以下场景:

- [ ] 正常上传jpg图片
- [ ] 正常上传png图片
- [ ] 正常上传gif图片
- [ ] 上传超过5MB的文件(应提示错误)
- [ ] 上传不支持的格式(如bmp, webp等,应提示错误)
- [ ] 未登录状态下上传(应被拦截)
- [ ] 头像显示功能
- [ ] 未设置头像时显示默认头像
- [ ] 头像更新后立即刷新显示
- [ ] 删除头像功能

---

## 📞 联系方式

如有问题,请联系后端开发人员。

**服务器地址**: `http://localhost:22223`  
**API基础路径**: `/api`
