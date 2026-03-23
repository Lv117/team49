package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.CourseSelectionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * CourseSelection 选课管理 Controller
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/courseSelection")
public class CourseSelectionController {
    private final CourseSelectionService courseSelectionService;

    public CourseSelectionController(CourseSelectionService courseSelectionService) {
        this.courseSelectionService = courseSelectionService;
    }

    /**
     * 获取选课列表
     */
    @PostMapping("/getCourseSelectionList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getCourseSelectionList(@Valid @RequestBody DataRequest dataRequest) {
        return courseSelectionService.getCourseSelectionList(dataRequest);
    }

    /**
     * 学生选课
     */
    @PostMapping("/selectCourse")
    @PreAuthorize("hasRole('STUDENT')")
    public DataResponse selectCourse(@Valid @RequestBody DataRequest dataRequest) {
        return courseSelectionService.selectCourse(dataRequest);
    }

    /**
     * 退课
     */
    @PostMapping("/dropCourse")
    @PreAuthorize("hasRole('STUDENT')")
    public DataResponse dropCourse(@Valid @RequestBody DataRequest dataRequest) {
        return courseSelectionService.dropCourse(dataRequest);
    }

    /**
     * 批量导入选课
     */
    @PostMapping("/batchSelectCourse")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse batchSelectCourse(@Valid @RequestBody DataRequest dataRequest) {
        return courseSelectionService.batchSelectCourse(dataRequest);
    }

    /**
     * 删除选课记录
     */
    @PostMapping("/courseSelectionDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse courseSelectionDelete(@Valid @RequestBody DataRequest dataRequest) {
        return courseSelectionService.courseSelectionDelete(dataRequest);
    }

    /**
     * 获取选课统计
     */
    @PostMapping("/getCourseSelectionStatistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getCourseSelectionStatistics(@Valid @RequestBody DataRequest dataRequest) {
        return courseSelectionService.getCourseSelectionStatistics(dataRequest);
    }
}
