package cn.edu.sdu.java.server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.HonorActivityService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/honor")
@Tag(name = "Honor And Activity", description = "Honor award and daily activity APIs")
public class HonorActivityController {
    private final HonorActivityService honorActivityService;

    public HonorActivityController(HonorActivityService honorActivityService) {
        this.honorActivityService = honorActivityService;
    }

    // ==================== 荣誉奖励管理 ====================

    /**
     * 获取荣誉奖励列表
     */
    @Operation(summary = "获取荣誉奖励列表")
    @PostMapping("/getHonorList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getHonorList(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.getHonorList(dataRequest);
    }

    /**
     * 分页获取荣誉奖励数据
     */
    @Operation(summary = "分页获取荣誉奖励数据")
    @PostMapping("/getHonorPageData")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getHonorPageData(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.getHonorPageData(dataRequest);
    }

    /**
     * 保存荣誉奖励
     */
    @Operation(summary = "保存荣誉奖励")
    @PostMapping("/honorSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse honorSave(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.honorSave(dataRequest);
    }

    /**
     * 删除荣誉奖励
     */
    @Operation(summary = "删除荣誉奖励")
    @PostMapping("/honorDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse honorDelete(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.honorDelete(dataRequest);
    }

    /**
     * 审批荣誉奖励
     */
    @Operation(summary = "审批荣誉奖励")
    @PostMapping("/honorApprove")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse honorApprove(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.honorApprove(dataRequest);
    }

    // ==================== 日常活动管理 ====================

    /**
     * 获取日常活动列表
     */
    @Operation(summary = "获取日常活动列表")
    @PostMapping("/getDailyActivityList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getDailyActivityList(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.getDailyActivityList(dataRequest);
    }

    /**
     * 分页获取日常活动数据
     */
    @Operation(summary = "分页获取日常活动数据")
    @PostMapping("/getDailyActivityPageData")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getDailyActivityPageData(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.getDailyActivityPageData(dataRequest);
    }

    /**
     * 保存日常活动
     */
    @Operation(summary = "保存日常活动")
    @PostMapping("/dailyActivitySave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse dailyActivitySave(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.dailyActivitySave(dataRequest);
    }

    /**
     * 删除日常活动
     */
    @Operation(summary = "删除日常活动")
    @PostMapping("/dailyActivityDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse dailyActivityDelete(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.dailyActivityDelete(dataRequest);
    }

    /**
     * 审批日常活动
     */
    @Operation(summary = "审批日常活动")
    @PostMapping("/dailyActivityApprove")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse dailyActivityApprove(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.dailyActivityApprove(dataRequest);
    }
}
