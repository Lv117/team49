package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.AvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 头像管理控制器
 * 提供头像上传、获取、删除等功能
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/avatar")
@RequiredArgsConstructor
public class AvatarController {

    private final AvatarService avatarService;

    /**
     * 上传头像
     * POST /api/avatar/upload
     * Content-Type: multipart/form-data
     * 权限: STUDENT, TEACHER, ADMIN
     *
     * @param file 上传的图片文件
     * @return 包含头像URL的响应
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<DataResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String avatarUrl = avatarService.uploadAvatar(file);
            Map<String, String> data = new HashMap<>();
            data.put("avatarUrl", avatarUrl);
            return ResponseEntity.ok(new DataResponse(0, data, "success"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new DataResponse(1, null, e.getMessage()));
        }
    }

    /**
     * 获取头像
     * GET /api/avatar/{personId}
     * 公开访问(不需要认证)
     *
     * @param personId 人员ID
     * @return 图片二进制流或404
     */
    @GetMapping("/{personId}")
    public ResponseEntity<?> getAvatar(@PathVariable Integer personId) {
        try {
            byte[] avatar = avatarService.getAvatar(personId);
            if (avatar != null) {
                String contentType = avatarService.getAvatarContentType(personId);
                return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .body(avatar);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 删除头像
     * DELETE /api/avatar
     * 权限: STUDENT(仅自己), ADMIN(所有人)
     *
     * @param request 包含personId的请求体
     * @return 操作结果
     */
    @DeleteMapping
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<DataResponse> deleteAvatar(@RequestBody Map<String, Integer> request) {
        try {
            Integer personId = request.get("personId");
            avatarService.deleteAvatar(personId);
            return ResponseEntity.ok(new DataResponse(0, null, "success"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new DataResponse(1, null, e.getMessage()));
        }
    }
}
