package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.InnovationProjectService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/innovation")
public class InnovationController {
    private final InnovationProjectService innovationProjectService;

    public InnovationController(InnovationProjectService innovationProjectService) {
        this.innovationProjectService = innovationProjectService;
    }

    /**
     * 获取创新实践项目列表
     */
    @PostMapping("/getInnovationProjectList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getInnovationProjectList(@Valid @RequestBody DataRequest dataRequest) {
        return innovationProjectService.getInnovationProjectList(dataRequest);
    }

    /**
     * 分页获取创新实践项目数据
     */
    @PostMapping("/getInnovationProjectPageData")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getInnovationProjectPageData(@Valid @RequestBody DataRequest dataRequest) {
        return innovationProjectService.getInnovationProjectPageData(dataRequest);
    }

    /**
     * 保存创新实践项目
     */
    @PostMapping("/innovationProjectSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse innovationProjectSave(@Valid @RequestBody DataRequest dataRequest) {
        return innovationProjectService.innovationProjectSave(dataRequest);
    }

    /**
     * 删除创新实践项目
     */
    @PostMapping("/innovationProjectDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse innovationProjectDelete(@Valid @RequestBody DataRequest dataRequest) {
        return innovationProjectService.innovationProjectDelete(dataRequest);
    }

    /**
     * 审批创新实践项目
     */
    @PostMapping("/innovationProjectApprove")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse innovationProjectApprove(@Valid @RequestBody DataRequest dataRequest) {
        return innovationProjectService.innovationProjectApprove(dataRequest);
    }
}
