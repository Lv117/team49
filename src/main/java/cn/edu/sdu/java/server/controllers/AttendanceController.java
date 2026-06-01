package cn.edu.sdu.java.server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Attendance 考勤管理 Controller
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/attendance")
@Tag(name = "Attendance", description = "Attendance record and statistics APIs")
public class AttendanceController {
    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * 获取考勤列表
     */
    @Operation(summary = "获取考勤列表")
    @PostMapping("/getAttendanceList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getAttendanceList(@Valid @RequestBody DataRequest dataRequest) {
        return attendanceService.getAttendanceList(dataRequest);
    }

    /**
     * 保存考勤记录
     */
    @Operation(summary = "保存考勤记录")
    @PostMapping("/attendanceSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse attendanceSave(@Valid @RequestBody DataRequest dataRequest) {
        return attendanceService.attendanceSave(dataRequest);
    }

    /**
     * 批量保存考勤记录
     */
    @Operation(summary = "批量保存考勤记录")
    @PostMapping("/attendanceBatchSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse attendanceBatchSave(@Valid @RequestBody DataRequest dataRequest) {
        return attendanceService.attendanceBatchSave(dataRequest);
    }

    /**
     * 删除考勤记录
     */
    @Operation(summary = "删除考勤记录")
    @PostMapping("/attendanceDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse attendanceDelete(@Valid @RequestBody DataRequest dataRequest) {
        return attendanceService.attendanceDelete(dataRequest);
    }

    /**
     * 获取考勤统计
     */
    @Operation(summary = "获取考勤统计")
    @PostMapping("/getAttendanceStatistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getAttendanceStatistics(@Valid @RequestBody DataRequest dataRequest) {
        return attendanceService.getAttendanceStatistics(dataRequest);
    }
}
