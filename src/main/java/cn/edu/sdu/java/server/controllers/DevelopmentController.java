package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.DevelopmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/development")
public class DevelopmentController {
    private final DevelopmentService developmentService;

    public DevelopmentController(DevelopmentService developmentService) {
        this.developmentService = developmentService;
    }

    /**
     * 获取发展记录列表(荣誉/创新/竞赛/成果)
     */
    @PostMapping("/getDevelopmentList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getDevelopmentList(@Valid @RequestBody DataRequest dataRequest) {
        return developmentService.getDevelopmentList(dataRequest);
    }

    /**
     * 分页获取发展记录数据
     */
    @PostMapping("/getDevelopmentPageList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getDevelopmentPageList(@Valid @RequestBody DataRequest dataRequest) {
        return developmentService.getDevelopmentPageData(dataRequest);
    }

    /**
     * 保存发展记录
     */
    @PostMapping("/developmentSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse developmentSave(@Valid @RequestBody DataRequest dataRequest) {
        return developmentService.developmentSave(dataRequest);
    }

    /**
     * 删除发展记录
     */
    @PostMapping("/developmentDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse developmentDelete(@Valid @RequestBody DataRequest dataRequest) {
        return developmentService.developmentDelete(dataRequest);
    }

    /**
     * 审批发展记录
     */
    @PostMapping("/developmentApprove")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse developmentApprove(@Valid @RequestBody DataRequest dataRequest) {
        return developmentService.developmentApprove(dataRequest);
    }

    /**
     * 获取类型选项列表
     */
    @PostMapping("/getTypeOptionList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getTypeOptionList(@Valid @RequestBody DataRequest dataRequest) {
        return developmentService.getTypeOptionList(dataRequest);
    }
}
