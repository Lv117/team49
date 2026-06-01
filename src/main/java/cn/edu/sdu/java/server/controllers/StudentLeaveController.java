package cn.edu.sdu.java.server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.OptionItemList;
import cn.edu.sdu.java.server.services.StudentLeaveService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/studentLeave")
@Tag(name = "Student Leave", description = "Student leave request and approval APIs")
public class StudentLeaveController {
    private final StudentLeaveService studentLeaveService;
    public StudentLeaveController(StudentLeaveService studentLeaveService) {
        this.studentLeaveService = studentLeaveService;
    }
    @Operation(summary = "Get teacher option list")
    @PostMapping("/getTeacherItemOptionList")
    @PreAuthorize("hasRole('STUDENT')")
    public OptionItemList getTeacherItemOptionList(@Valid @RequestBody DataRequest dataRequest) {
        return studentLeaveService.getTeacherItemOptionList(dataRequest);
    }
    @Operation(summary = "Get student leave list")
    @PostMapping("/getStudentLeaveList")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')  or hasRole('TEACHER')")
    public DataResponse getStudentLeaveList(@Valid @RequestBody DataRequest dataRequest) {
        return studentLeaveService.getStudentLeaveList(dataRequest);
    }
    @Operation(summary = "保存请假申请")
    @PostMapping("/studentLeaveSave")
    @PreAuthorize("hasRole('STUDENT') ")
    public DataResponse studentLeaveSave(@Valid @RequestBody DataRequest dataRequest) {
        return studentLeaveService.studentLeaveSave(dataRequest);
    }
    @Operation(summary = "教师审批请假申请")
    @PostMapping("/studentLeaveCheck")
    @PreAuthorize("hasRole('TEACHER')")
    public DataResponse studentLeaveCheck(@Valid @RequestBody DataRequest dataRequest) {
        return studentLeaveService.studentLeaveCheck(dataRequest);
    }

    @Operation(summary = "学生返校报备")
    @PostMapping("/studentReturn")
    @PreAuthorize("hasRole('STUDENT')")
    public DataResponse studentReturn(@Valid @RequestBody DataRequest dataRequest) {
        return studentLeaveService.studentReturn(dataRequest);
    }

    @Operation(summary = "Get leave progress")
    @PostMapping("/getLeaveProgress")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse getLeaveProgress(@Valid @RequestBody DataRequest dataRequest) {
        return studentLeaveService.getLeaveProgress(dataRequest);
    }

    @Operation(summary = "Sync leave with attendance")
    @PostMapping("/syncLeaveWithAttendance")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse syncLeaveWithAttendance(@Valid @RequestBody DataRequest dataRequest) {
        return studentLeaveService.syncLeaveWithAttendance(dataRequest);
    }

}
