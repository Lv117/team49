package cn.edu.sdu.java.server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.OptionItemList;
import cn.edu.sdu.java.server.services.ScoreService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/score")
@Tag(name = "Score", description = "Score management APIs")
public class ScoreController {
    private final ScoreService scoreService;
    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }
    @Operation(summary = "获取学生选项列表")
    @PostMapping("/getStudentItemOptionList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public OptionItemList getStudentItemOptionList(@Valid @RequestBody DataRequest dataRequest) {
        return scoreService.getStudentItemOptionList(dataRequest);
    }

    @Operation(summary = "获取课程选项列表")
    @PostMapping("/getCourseItemOptionList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public OptionItemList getCourseItemOptionList(@Valid @RequestBody DataRequest dataRequest) {
        return scoreService.getCourseItemOptionList(dataRequest);
    }

    @Operation(summary = "获取教师授课课程列表（左侧课程列表）")
    @PostMapping("/getTeacherCourseList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse getTeacherCourseList(@Valid @RequestBody DataRequest dataRequest) {
        return scoreService.getTeacherCourseList(dataRequest);
    }

    @Operation(summary = "获取成绩列表")
    @PostMapping("/getScoreList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getScoreList(@Valid @RequestBody DataRequest dataRequest) {
        return scoreService.getScoreList(dataRequest);
    }
    @Operation(summary = "保存成绩")
    @PostMapping("/scoreSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse scoreSave(@Valid @RequestBody DataRequest dataRequest) {
        return scoreService.scoreSave(dataRequest);
    }
    @Operation(summary = "删除成绩")
    @PostMapping("/scoreDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse scoreDelete(@Valid @RequestBody DataRequest dataRequest) {
        return scoreService.scoreDelete(dataRequest);
    }

}
