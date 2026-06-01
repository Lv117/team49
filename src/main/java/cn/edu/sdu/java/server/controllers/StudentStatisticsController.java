package cn.edu.sdu.java.server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.StudentStatisticsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/studentStatistics")

@Tag(name = "Student Statistics", description = "Student statistics APIs")
public class StudentStatisticsController {
    private final StudentStatisticsService studentStatisticsService;

    public StudentStatisticsController(StudentStatisticsService studentStatisticsService) {
        this.studentStatisticsService = studentStatisticsService;
    }

    @Operation(summary = "Get student statistics list")
    @PostMapping("/getStudentStatisticsList")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse getStudentStatisticsList(@Valid @RequestBody DataRequest dataRequest) {
        return studentStatisticsService.getStudentStatisticsList(dataRequest);
    }
    @Operation(summary = "Execute student statistics")
    @PostMapping("/doStudentStatistics")
    @PreAuthorize("hasRole('ADMIN') ")
    public DataResponse doStudentStatistics(@Valid @RequestBody DataRequest dataRequest) {
        return studentStatisticsService.doStudentStatistics(dataRequest);
    }

}
