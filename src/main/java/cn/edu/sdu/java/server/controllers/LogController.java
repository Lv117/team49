package cn.edu.sdu.java.server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.LogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * LogController 日志与监控控制器
 * 提供日志查询和系统监控统计接口
 */
@RestController
@RequestMapping("/api/log")
@Tag(name = "Log Monitor", description = "Request log, modify log and monitor statistics APIs")
public class LogController {
    
    private final LogService logService;
    
    public LogController(LogService logService) {
        this.logService = logService;
    }
    
    /**
     * 获取请求日志列表
     * 支持按时间范围、操作人筛选
     * 权限：ADMIN可查看所有日志，TEACHER/STUDENT只能查看自己的操作日志
     */
    @Operation(summary = "获取请求日志列表")
    @PostMapping("/getRequestLogList")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public DataResponse getRequestLogList(@RequestBody DataRequest dataRequest) {
        return logService.getRequestLogList(dataRequest);
    }
    
    /**
     * 获取修改日志列表
     * 支持按时间范围、操作人、操作类型筛选
     * 权限：ADMIN可查看所有日志，TEACHER/STUDENT只能查看自己的操作日志
     */
    @Operation(summary = "获取修改日志列表")
    @PostMapping("/getModifyLogList")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public DataResponse getModifyLogList(@RequestBody DataRequest dataRequest) {
        return logService.getModifyLogList(dataRequest);
    }
    
    /**
     * 获取系统监控统计数据
     * 返回系统访问量、活跃用户、接口调用频次、平均响应时间等统计数据
     * 权限：仅ADMIN可访问
     */
    @Operation(summary = "获取系统监控统计数据")
    @PostMapping("/getSystemStatistics")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse getSystemStatistics(@RequestBody DataRequest dataRequest) {
        return logService.getSystemStatistics(dataRequest);
    }
}
