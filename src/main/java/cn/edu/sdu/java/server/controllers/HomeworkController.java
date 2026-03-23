package cn.edu.sdu.java.server.controllers;

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
public class HomeworkController {
    private final HomeworkService homeworkService;

    public HomeworkController(HomeworkService homeworkService) {
        this.homeworkService = homeworkService;
    }

    /**
     * 获取作业列表
     */
    @PostMapping("/getHomeworkList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getHomeworkList(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.getHomeworkList(dataRequest);
    }

    /**
     * 保存作业
     */
    @PostMapping("/homeworkSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse homeworkSave(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.homeworkSave(dataRequest);
    }

    /**
     * 删除作业
     */
    @PostMapping("/homeworkDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse homeworkDelete(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.homeworkDelete(dataRequest);
    }

    /**
     * 获取作业提交列表
     */
    @PostMapping("/getSubmissionList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getSubmissionList(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.getSubmissionList(dataRequest);
    }

    /**
     * 提交作业
     */
    @PostMapping("/submitHomework")
    @PreAuthorize("hasRole('STUDENT')")
    public DataResponse submitHomework(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.submitHomework(dataRequest);
    }

    /**
     * 批改作业
     */
    @PostMapping("/gradeHomework")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse gradeHomework(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.gradeHomework(dataRequest);
    }

    /**
     * 获取作业统计
     */
    @PostMapping("/getHomeworkStatistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse getHomeworkStatistics(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.getHomeworkStatistics(dataRequest);
    }
}
