package cn.edu.sdu.java.server.services;

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
        
        // 过滤项目名称
        if (projectName != null && !projectName.isEmpty()) {
            final String searchName = projectName;
            projectList = projectList.stream()
                    .filter(p -> p.getProjectName().contains(searchName))
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

                // 过滤
                for (InnovationProject project : list) {
                    boolean match = true;
                    if (projectName != null && !projectName.isEmpty()) {
                        if (!project.getProjectName().contains(projectName)) {
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
        // 直接从 dataRequest.data 中获取参数
        Map<String, Object> form = dataRequest.getData();
        if (form == null) {
            form = new HashMap<>();
        }
        
        Integer projectId = CommonMethod.getInteger(form, "projectId");
        if (projectId == null) {
            projectId = CommonMethod.getInteger(form, "id");
        }
        Integer studentId = CommonMethod.getInteger(form, "studentId");
        String studentName = CommonMethod.getString(form, "studentName");
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            Integer currentStudentId = CommonMethod.getPersonId();
            if (currentStudentId == null) {
                return CommonMethod.getReturnMessageError("未识别到当前学生身份，无法保存");
            }
            Optional<Student> sop = studentRepository.findByPersonPersonId(currentStudentId);
            if (sop.isEmpty() || sop.get().getPerson() == null) {
                return CommonMethod.getReturnMessageError("当前学生信息不存在，无法保存");
            }
            studentId = currentStudentId;
            studentName = sop.get().getPerson().getName();
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
        
        if (projectId != null && projectId > 0) {
            Optional<InnovationProject> op = innovationProjectRepository.findById(projectId);
            if (op.isPresent()) {
                InnovationProject project = op.get();
                if (teacherDataScopeService.isCurrentRoleTeacher()
                        && !teacherDataScopeService.canCurrentTeacherAccessStudent(project.getStudentId())) {
                    return CommonMethod.getReturnMessageError("仅可审批本人授课学生提交的数据");
                }
                String currentStatus = project.getStatus();
                
                // 使用状态机工具类检查状态转换是否有效
                if (!ApprovalStateMachine.isValidTransition(currentStatus, status)) {
                    return CommonMethod.getReturnMessageError(
                        ApprovalStateMachine.getTransitionErrorMessage(currentStatus, status));
                }
                
                project.setStatus(status);
                project.setApprovalOpinion(approvalOpinion);
                project.setUpdateTime(LocalDateTime.now());
                innovationProjectRepository.save(project);
                // 保存审批记录
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

            }
        }
        
        return CommonMethod.getReturnMessageOK();
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
