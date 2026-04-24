package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.HonorActivityService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 社会实践板块 Controller
 * 包含: 日常活动、培训讲座、校外实习、志愿服务
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/social-practice")
public class SocialPracticeController {
    private final HonorActivityService honorActivityService;

    public SocialPracticeController(HonorActivityService honorActivityService) {
        this.honorActivityService = honorActivityService;
    }

    // ==================== 社会实践管理(日常活动/培训讲座/校外实习/志愿服务) ====================

    /**
     * 获取社会实践列表
     */
    @PostMapping("/getPracticeList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getPracticeList(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.getDailyActivityList(dataRequest);
    }

    /**
     * 分页获取社会实践数据
     */
    @PostMapping("/getPracticePageData")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getPracticePageData(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.getDailyActivityPageData(dataRequest);
    }

    /**
     * 保存社会实践
     */
    @PostMapping("/practiceSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse practiceSave(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.dailyActivitySave(dataRequest);
    }

    /**
     * 删除社会实践
     */
    @PostMapping("/practiceDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse practiceDelete(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.dailyActivityDelete(dataRequest);
    }

    /**
     * 审批社会实践
     */
    @PostMapping("/practiceApprove")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse practiceApprove(@Valid @RequestBody DataRequest dataRequest) {
        return honorActivityService.dailyActivityApprove(dataRequest);
    }
}
