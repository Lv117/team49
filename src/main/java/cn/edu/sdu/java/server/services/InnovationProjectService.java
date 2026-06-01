package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.InnovationProject;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.InnovationProjectRepository;
import cn.edu.sdu.java.server.util.ApprovalStateMachine;
import cn.edu.sdu.java.server.util.CommonMethod;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import cn.edu.sdu.java.server.models.ApprovalRecord;
import cn.edu.sdu.java.server.repositorys.ApprovalRecordRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class InnovationProjectService {
    private final InnovationProjectRepository innovationProjectRepository;
    private final ApprovalRecordRepository approvalRecordRepository;
    private final StudentRepository studentRepository;
    private final TeacherDataScopeService teacherDataScopeService;

    public InnovationProjectService(InnovationProjectRepository innovationProjectRepository,
                                    ApprovalRecordRepository approvalRecordRepository,
                                    StudentRepository studentRepository,
                                    TeacherDataScopeService teacherDataScopeService) {
        this.innovationProjectRepository = innovationProjectRepository;
        this.approvalRecordRepository = approvalRecordRepository;
        this.studentRepository = studentRepository;
        this.teacherDataScopeService = teacherDataScopeService;
    }

    /**
     * 获取创新实践项目列表
     */
    public DataResponse getInnovationProjectList(DataRequest dataRequest) {
        String projectName = dataRequest.getString("projectName");
        String status = dataRequest.getString("status");
        Integer studentId = dataRequest.getInteger("studentId");
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            studentId = CommonMethod.getPersonId();
        }
        
        List<InnovationProject> projectList;
        
        if (studentId != null) {
            projectList = innovationProjectRepository.findByStudentId(studentId);
        } else if (status != null && !status.isEmpty()) {
            projectList = innovationProjectRepository.findByStatus(status);
        } else {
            projectList = innovationProjectRepository.findAll();
        }
        
        if (projectName != null && !projectName.isEmpty()) {
            final String searchName = projectName;
            projectList = projectList.stream()
                    .filter(p -> containsKeyword(p.getProjectName(), searchName))
                    .toList();
        }
        if (teacherDataScopeService.isCurrentRoleTeacher()) {
            Set<Integer> allowedStudentIds = teacherDataScopeService.getCurrentTeacherStudentIds();
            if (allowedStudentIds.isEmpty()) {
                projectList = new ArrayList<>();
            } else {
                projectList = projectList.stream()
                        .filter(p -> p.getStudentId() != null && allowedStudentIds.contains(p.getStudentId()))
                        .toList();
            }
        }
        
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (InnovationProject project : projectList) {
            dataList.add(getMapFromProject(project));
        }
        
        return CommonMethod.getReturnData(dataList);
    }

    /**
     * 分页获取创新实践项目数据
     */
    public DataResponse getInnovationProjectPageData(DataRequest dataRequest) {
        String projectName = dataRequest.getString("projectName");
        String status = dataRequest.getString("status");
        Integer cPage = dataRequest.getCurrentPage();
        int pageIndex = cPage != null ? cPage : 0;
        int size = 20;
        int dataTotal = 0;
        List<Map<String, Object>> dataList = new ArrayList<>();

        if (teacherDataScopeService.isCurrentRoleTeacher()) {
            Set<Integer> allowedStudentIds = teacherDataScopeService.getCurrentTeacherStudentIds();
            List<InnovationProject> filteredList;
            if (allowedStudentIds.isEmpty()) {
                filteredList = new ArrayList<>();
            } else {
                filteredList = innovationProjectRepository.findAll().stream()
                        .filter(p -> p.getStudentId() != null && allowedStudentIds.contains(p.getStudentId()))
                        .filter(p -> projectName == null || projectName.isEmpty()
                                || (p.getProjectName() != null && p.getProjectName().contains(projectName)))
                        .filter(p -> status == null || status.isEmpty()
                                || status.equals(p.getStatus()))
                        .toList();
            }
            dataTotal = filteredList.size();
            int fromIndex = Math.min(pageIndex * size, dataTotal);
            int toIndex = Math.min(fromIndex + size, dataTotal);
            for (InnovationProject project : filteredList.subList(fromIndex, toIndex)) {
                dataList.add(getMapFromProject(project));
            }
        } else {
            Pageable pageable = PageRequest.of(pageIndex, size);
            Page<InnovationProject> page = innovationProjectRepository.findAll(pageable);
            if (page != null) {
                dataTotal = (int) page.getTotalElements();
                List<InnovationProject> list = page.getContent();

                for (InnovationProject project : list) {
                    boolean match = true;
                    if (projectName != null && !projectName.isEmpty()) {
                        if (!containsKeyword(project.getProjectName(), projectName)) {
                            match = false;
                        }
                    }
                    if (status != null && !status.isEmpty()) {
                        if (!status.equals(project.getStatus())) {
                            match = false;
                        }
                    }

                    if (match) {
                        dataList.add(getMapFromProject(project));
                    }
                }
            }
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("dataTotal", dataTotal);
        data.put("dataList", dataList);
        
        return CommonMethod.getReturnData(data);
    }

    /**
     * 保存创新实践项目
     */
    public DataResponse innovationProjectSave(DataRequest dataRequest) {
        Map<String, Object> form = dataRequest.getData();
        if (form == null || form.isEmpty()) {
            form = dataRequest.getMap("form");
        }
        if (form == null) {
            form = new HashMap<>();
        }
        
        Integer projectId = CommonMethod.getInteger(form, "projectId");
        if (projectId == null) {
            projectId = CommonMethod.getInteger(form, "id");
        }
        Integer studentId = CommonMethod.getInteger(form, "studentId");
        String studentName = CommonMethod.getString(form, "studentName");
        // 学生端提交时，后端必须覆盖前端传入的学生信息，避免越权替别人新增或修改项目。
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            Integer currentStudentId = CommonMethod.getPersonId();
            if (currentStudentId == null) {
                throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "未识别到当前学生身份，无法保存");
            }
            Student student = studentRepository.findByPersonPersonId(currentStudentId)
                    .filter(s -> s.getPerson() != null)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "当前学生信息不存在，无法保存"));
            studentId = currentStudentId;
            studentName = student.getPerson().getName();
        }
        
        InnovationProject project = null;
        
        if (projectId != null && projectId > 0) {
            Optional<InnovationProject> op = innovationProjectRepository.findById(projectId);
            if (op.isPresent()) {
                project = op.get();
                project.setUpdateTime(LocalDateTime.now());
            }
        }
        
        if (project == null) {
            project = new InnovationProject();
            project.setCreateTime(LocalDateTime.now());
        }
        
        project.setProjectName(CommonMethod.getString(form, "projectName"));
        project.setStudentId(studentId);
        project.setStudentName(studentName);
        project.setProjectType(CommonMethod.getString(form, "projectType"));
        project.setDescription(CommonMethod.getString(form, "description"));
        
        String startDateStr = CommonMethod.getString(form, "startDate");
        if (startDateStr != null && !startDateStr.isEmpty()) {
            project.setStartDate(java.time.LocalDate.parse(startDateStr));
        }
        
        String endDateStr = CommonMethod.getString(form, "endDate");
        if (endDateStr != null && !endDateStr.isEmpty()) {
            project.setEndDate(java.time.LocalDate.parse(endDateStr));
        }
        
        String statusStr = CommonMethod.getString(form, "status");
        project.setStatus(statusStr != null && !statusStr.isEmpty() ? statusStr : "draft");
        
        project.setApprovalOpinion(CommonMethod.getString(form, "approvalOpinion"));
        
        innovationProjectRepository.save(project);
        
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 删除创新实践项目
     */
    public DataResponse innovationProjectDelete(DataRequest dataRequest) {
        Integer projectId = dataRequest.getInteger("projectId");
        if (projectId == null) {
            projectId = dataRequest.getInteger("id");
        }
        
        if (projectId != null && projectId > 0) {
            Optional<InnovationProject> op = innovationProjectRepository.findById(projectId);
            op.ifPresent(innovationProjectRepository::delete);
        }
        
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 审批创新实践项目
     */
    public DataResponse innovationProjectApprove(DataRequest dataRequest) {
        Integer projectId = dataRequest.getInteger("projectId");
        if (projectId == null) {
            projectId = dataRequest.getInteger("id");
        }
        String status = dataRequest.getString("status");
        String approvalOpinion = dataRequest.getString("approvalOpinion");

        if (projectId == null || projectId <= 0) {
            throw new BusinessException(ErrorCodes.INNOVATION_NOT_FOUND, "创新实践项目ID不能为空");
        }
        if (status == null || status.isBlank()) {
            throw new BusinessException(ErrorCodes.INNOVATION_STATUS_INVALID, "目标状态不能为空");
        }

        InnovationProject project = innovationProjectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.INNOVATION_NOT_FOUND, "创新实践项目不存在"));

        if (teacherDataScopeService.isCurrentRoleTeacher()
                && !teacherDataScopeService.canCurrentTeacherAccessStudent(project.getStudentId())) {
            throw new BusinessException(ErrorCodes.ACCESS_DENIED, "仅可审批本人授课学生提交的数据");
        }

        String currentStatus = project.getStatus();
        // 创新实践审批与荣誉/活动审批共用同一状态机，避免跳过中间审批节点。
        // 审批状态必须符合状态机定义，避免前端跳过教师/管理员审批步骤。
        if (!ApprovalStateMachine.isValidTransition(currentStatus, status)) {
            throw new BusinessException(
                    ErrorCodes.INNOVATION_STATUS_INVALID,
                    ApprovalStateMachine.getTransitionErrorMessage(currentStatus, status));
        }

        project.setStatus(status);
        project.setApprovalOpinion(approvalOpinion);
        project.setUpdateTime(LocalDateTime.now());
        innovationProjectRepository.save(project);

        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType("innovation");
        record.setBusinessId(project.getId());
        record.setFromStatus(currentStatus);
        record.setToStatus(status);
        record.setApprovalOpinion(approvalOpinion);
        record.setOperatorId(CommonMethod.getPersonId());
        record.setOperatorName(CommonMethod.getUsername());
        record.setOperateTime(LocalDateTime.now());
        approvalRecordRepository.save(record);

        return CommonMethod.getReturnMessageOK();
    }

    private boolean containsKeyword(String source, String keyword) {
        return source != null && keyword != null && source.contains(keyword);
    }

    /**
     * 将 InnovationProject 转换为 Map
     */
    private Map<String, Object> getMapFromProject(InnovationProject project) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", project.getId());
        map.put("projectName", project.getProjectName());
        map.put("studentId", project.getStudentId());
        map.put("studentName", project.getStudentName());
        map.put("projectType", project.getProjectType());
        map.put("description", project.getDescription());
        map.put("startDate", project.getStartDate());
        map.put("endDate", project.getEndDate());
        map.put("status", project.getStatus());
        map.put("approvalOpinion", project.getApprovalOpinion());
        map.put("createTime", project.getCreateTime());
        map.put("updateTime", project.getUpdateTime());
        return map;
    }
}
