package cn.edu.sdu.java.server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.HomeworkService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Homework 作业管理 Controller
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/homework")
@Tag(name = "Homework", description = "Homework publish, submit, grade and statistics APIs")
public class HomeworkController {
    private final HomeworkService homeworkService;

    public HomeworkController(HomeworkService homeworkService) {
        this.homeworkService = homeworkService;
    }

    /**
     * 获取作业列表
     */
    @Operation(summary = "获取作业列表")
    @PostMapping("/getHomeworkList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getHomeworkList(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.getHomeworkList(dataRequest);
    }

    /**
     * 保存作业
     */
    @Operation(summary = "保存作业")
    @PostMapping("/homeworkSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse homeworkSave(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.homeworkSave(dataRequest);
    }

    /**
     * 删除作业
     */
    @Operation(summary = "删除作业")
    @PostMapping("/homeworkDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse homeworkDelete(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.homeworkDelete(dataRequest);
    }

    /**
     * 删除学生作业提交记录
     */
    @Operation(summary = "删除学生作业提交记录")
    @PostMapping("/submissionDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse submissionDelete(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.submissionDelete(dataRequest);
    }

    /**
     * 获取作业提交列表
     */
    @Operation(summary = "获取作业提交列表")
    @PostMapping("/getSubmissionList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getSubmissionList(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.getSubmissionList(dataRequest);
    }

    /**
     * 提交作业
     */
    @Operation(summary = "提交作业")
    @PostMapping("/submitHomework")
    @PreAuthorize("hasRole('STUDENT')")
    public DataResponse submitHomework(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.submitHomework(dataRequest);
    }

    /**
     * 批改作业
     */
    @Operation(summary = "批改作业")
    @PostMapping("/gradeHomework")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse gradeHomework(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.gradeHomework(dataRequest);
    }

    /**
     * 获取作业统计
     */
    @Operation(summary = "获取作业统计")
    @PostMapping("/getHomeworkStatistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse getHomeworkStatistics(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.getHomeworkStatistics(dataRequest);
    }
}
