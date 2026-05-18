package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.CourseTeachingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * CourseTeachingController 任课安排控制器
 * 提供任课安排的REST API接口
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/courseTeaching")
public class CourseTeachingController {
    private final CourseTeachingService courseTeachingService;

    public CourseTeachingController(CourseTeachingService courseTeachingService) {
        this.courseTeachingService = courseTeachingService;
    }

    /**
     * 获取任课安排列表（管理员使用）
     */
    @PostMapping("/getCourseTeachingList")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse getCourseTeachingList(@Valid @RequestBody DataRequest dataRequest) {
        return courseTeachingService.getCourseTeachingList(dataRequest);
    }

    /**
     * 保存任课安排（管理员使用）
     */
    @PostMapping("/courseTeachingSave")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse courseTeachingSave(@Valid @RequestBody DataRequest dataRequest) {
        return courseTeachingService.courseTeachingSave(dataRequest);
    }

    /**
     * 删除任课安排（管理员使用）
     */
    @PostMapping("/courseTeachingDelete")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse courseTeachingDelete(@Valid @RequestBody DataRequest dataRequest) {
        return courseTeachingService.courseTeachingDelete(dataRequest);
    }

    /**
     * 获取教师任课安排（教师端使用）
     */
    @PostMapping("/getTeacherCourseList")
    public DataResponse getTeacherCourseList(@Valid @RequestBody DataRequest dataRequest) {
        return courseTeachingService.getTeacherCourseList(dataRequest);
    }
}
