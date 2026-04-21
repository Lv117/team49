package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.ApprovalRecord;
import cn.edu.sdu.java.server.models.StudentDevelopment;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.ApprovalRecordRepository;
import cn.edu.sdu.java.server.repositorys.DevelopmentRepository;
import cn.edu.sdu.java.server.util.ApprovalStateMachine;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DevelopmentService {
    private final DevelopmentRepository developmentRepository;
    private final ApprovalRecordRepository approvalRecordRepository;

    public DevelopmentService(DevelopmentRepository developmentRepository,
                              ApprovalRecordRepository approvalRecordRepository) {
        this.developmentRepository = developmentRepository;
        this.approvalRecordRepository = approvalRecordRepository;
    }

    // ==================== 统一类型路由 ====================

    /**
     * 保存发展记录(荣誉/创新/竞赛/成果)
     */
    public DataResponse developmentSave(DataRequest dataRequest) {
        Map<String, Object> form = dataRequest.getData();
        if (form == null || form.isEmpty()) {
            form = dataRequest.getMap("form");
        }
        if (form == null) {
            form = new HashMap<>();
        }

        Integer id = CommonMethod.getInteger(form, "id");
        String developmentType = CommonMethod.getString(form, "developmentType");

        // 校验类型
        if (developmentType == null || developmentType.isEmpty()) {
            return CommonMethod.getReturnMessageError("类型不能为空");
        }
        if (!isValidType(developmentType)) {
            return CommonMethod.getReturnMessageError("不支持的类型: " + developmentType);
        }

        StudentDevelopment development = null;

        // 更新或新建
        if (id != null && id > 0) {
            Optional<StudentDevelopment> op = developmentRepository.findById(id);
            if (op.isPresent()) {
                development = op.get();
                development.setUpdateTime(LocalDateTime.now());
            }
        }

        if (development == null) {
            development = new StudentDevelopment();
            development.setCreateTime(LocalDateTime.now());
        }

        // 设置基础信息
        development.setStudentId(CommonMethod.getInteger(form, "studentId"));
        development.setStudentName(CommonMethod.getString(form, "studentName"));
        development.setTitle(CommonMethod.getString(form, "title"));
        development.setDescription(CommonMethod.getString(form, "description"));
        development.setDevelopmentType(developmentType);

        // 设置扩展字段(extraInfo)
        Map<String, Object> extraInfo = extractExtraInfo(form, developmentType);
        development.setExtraInfo(extraInfo);

        // 设置时间
        String startDateStr = CommonMethod.getString(form, "startDate");
        if (startDateStr != null && !startDateStr.isEmpty()) {
            development.setStartDate(java.time.LocalDate.parse(startDateStr));
        }

        String endDateStr = CommonMethod.getString(form, "endDate");
        if (endDateStr != null && !endDateStr.isEmpty()) {
            development.setEndDate(java.time.LocalDate.parse(endDateStr));
        }

        // 设置状态
        String statusStr = CommonMethod.getString(form, "status");
        development.setStatus(statusStr != null && !statusStr.isEmpty() ? statusStr : "draft");

        developmentRepository.save(development);

        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 获取发展记录列表
     */
    public DataResponse getDevelopmentList(DataRequest dataRequest) {
        String title = dataRequest.getString("title");
        String status = dataRequest.getString("status");
        String developmentType = dataRequest.getString("developmentType");
        Integer studentId = dataRequest.getInteger("studentId");

        List<StudentDevelopment> developmentList;

        // 根据条件查询
        if (studentId != null && developmentType != null && !developmentType.isEmpty()) {
            developmentList = developmentRepository.findByStudentIdAndDevelopmentType(studentId, developmentType);
        } else if (studentId != null) {
            developmentList = developmentRepository.findByStudentId(studentId);
        } else if (developmentType != null && !developmentType.isEmpty()) {
            developmentList = developmentRepository.findByDevelopmentType(developmentType);
        } else if (status != null && !status.isEmpty()) {
            developmentList = developmentRepository.findByStatus(status);
        } else {
            developmentList = developmentRepository.findAll();
        }

        // 过滤标题
        if (title != null && !title.isEmpty()) {
            final String searchTitle = title;
            developmentList = developmentList.stream()
                    .filter(d -> d.getTitle() != null && d.getTitle().contains(searchTitle))
                    .toList();
        }

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (StudentDevelopment development : developmentList) {
            dataList.add(getMapFromDevelopment(development));
        }

        return CommonMethod.getReturnData(dataList);
    }

    /**
     * 分页获取发展记录
     */
    public DataResponse getDevelopmentPageData(DataRequest dataRequest) {
        String title = dataRequest.getString("title");
        String status = dataRequest.getString("status");
        String developmentType = dataRequest.getString("developmentType");
        Integer cPage = dataRequest.getCurrentPage();
        int size = 20;
        int dataTotal = 0;
        List<Map<String, Object>> dataList = new ArrayList<>();

        Pageable pageable = PageRequest.of(cPage != null ? cPage : 0, size);
        Page<StudentDevelopment> page;

        if (developmentType != null && !developmentType.isEmpty()) {
            page = developmentRepository.findByDevelopmentType(developmentType, pageable);
        } else {
            page = developmentRepository.findAll(pageable);
        }

        if (page != null) {
            dataTotal = (int) page.getTotalElements();
            List<StudentDevelopment> list = page.getContent();

            for (StudentDevelopment development : list) {
                boolean match = true;
                if (title != null && !title.isEmpty()) {
                    if (development.getTitle() == null || !development.getTitle().contains(title)) {
                        match = false;
                    }
                }
                if (status != null && !status.isEmpty()) {
                    if (!status.equals(development.getStatus())) {
                        match = false;
                    }
                }

                if (match) {
                    dataList.add(getMapFromDevelopment(development));
                }
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("dataTotal", dataTotal);
        data.put("dataList", dataList);

        return CommonMethod.getReturnData(data);
    }

    /**
     * 删除发展记录
     */
    public DataResponse developmentDelete(DataRequest dataRequest) {
        Integer id = dataRequest.getInteger("id");

        if (id != null && id > 0) {
            Optional<StudentDevelopment> op = developmentRepository.findById(id);
            op.ifPresent(developmentRepository::delete);
        }

        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 审批发展记录
     */
    public DataResponse developmentApprove(DataRequest dataRequest) {
        Integer id = dataRequest.getInteger("id");
        String status = dataRequest.getString("status");
        String approvalOpinion = dataRequest.getString("approvalOpinion");

        if (id == null || id <= 0) {
            return CommonMethod.getReturnMessageError("ID不能为空");
        }
        if (status == null || status.isEmpty()) {
            return CommonMethod.getReturnMessageError("目标状态不能为空");
        }

        Optional<StudentDevelopment> op = developmentRepository.findById(id);
        if (op.isEmpty()) {
            return CommonMethod.getReturnMessageError("记录不存在");
        }

        StudentDevelopment development = op.get();
        String currentStatus = development.getStatus();

        // 状态机校验
        if (!ApprovalStateMachine.isValidTransition(currentStatus, status)) {
            return CommonMethod.getReturnMessageError(
                    ApprovalStateMachine.getTransitionErrorMessage(currentStatus, status));
        }

        development.setStatus(status);
        development.setApprovalOpinion(approvalOpinion);
        development.setApproverId(CommonMethod.getPersonId());
        development.setApproveTime(LocalDateTime.now());
        development.setUpdateTime(LocalDateTime.now());
        developmentRepository.save(development);

        // 保存审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType("development_" + development.getDevelopmentType());
        record.setBusinessId(development.getId());
        record.setFromStatus(currentStatus);
        record.setToStatus(status);
        record.setApprovalOpinion(approvalOpinion);
        record.setOperatorId(CommonMethod.getPersonId());
        record.setOperatorName(CommonMethod.getUsername());
        record.setOperateTime(LocalDateTime.now());
        approvalRecordRepository.save(record);

        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 获取类型选项列表
     */
    public DataResponse getTypeOptionList(DataRequest dataRequest) {
        List<Map<String, Object>> optionList = new ArrayList<>();

        Map<String, Object> honor = new HashMap<>();
        honor.put("id", 1);
        honor.put("value", "honor");
        honor.put("label", "荣誉奖励");
        optionList.add(honor);

        Map<String, Object> innovation = new HashMap<>();
        innovation.put("id", 2);
        innovation.put("value", "innovation");
        innovation.put("label", "创新实践");
        optionList.add(innovation);

        Map<String, Object> competition = new HashMap<>();
        competition.put("id", 3);
        competition.put("value", "competition");
        competition.put("label", "学科竞赛");
        optionList.add(competition);

        Map<String, Object> achievement = new HashMap<>();
        achievement.put("id", 4);
        achievement.put("value", "achievement");
        achievement.put("label", "科技成果");
        optionList.add(achievement);

        return CommonMethod.getReturnData(optionList);
    }

    // ==================== 辅助方法 ====================

    /**
     * 校验类型是否合法
     */
    private boolean isValidType(String type) {
        return "honor".equals(type) || "innovation".equals(type) || 
               "competition".equals(type) || "achievement".equals(type);
    }

    /**
     * 从表单中提取对应类型的扩展字段
     */
    private Map<String, Object> extractExtraInfo(Map<String, Object> form, String developmentType) {
        Map<String, Object> extraInfo = new HashMap<>();

        switch (developmentType) {
            case "honor":
                // 荣誉: level, issuer
                String level = CommonMethod.getString(form, "level");
                String issuer = CommonMethod.getString(form, "issuer");
                if (level != null) extraInfo.put("level", level);
                if (issuer != null) extraInfo.put("issuer", issuer);
                break;

            case "innovation":
                // 创新: field, outcome
                String field = CommonMethod.getString(form, "field");
                String outcome = CommonMethod.getString(form, "outcome");
                if (field != null) extraInfo.put("field", field);
                if (outcome != null) extraInfo.put("outcome", outcome);
                break;

            case "competition":
                // 竞赛: competitionName, awardLevel
                String competitionName = CommonMethod.getString(form, "competitionName");
                String awardLevel = CommonMethod.getString(form, "awardLevel");
                if (competitionName != null) extraInfo.put("competitionName", competitionName);
                if (awardLevel != null) extraInfo.put("awardLevel", awardLevel);
                break;

            case "achievement":
                // 成果: patentType, status
                String patentType = CommonMethod.getString(form, "patentType");
                String achievementStatus = CommonMethod.getString(form, "achievementStatus");
                if (patentType != null) extraInfo.put("patentType", patentType);
                if (achievementStatus != null) extraInfo.put("status", achievementStatus);
                break;
        }

        return extraInfo.isEmpty() ? null : extraInfo;
    }

    /**
     * 将 StudentDevelopment 转换为 Map
     */
    private Map<String, Object> getMapFromDevelopment(StudentDevelopment development) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", development.getId());
        map.put("studentId", development.getStudentId());
        map.put("studentName", development.getStudentName());
        map.put("title", development.getTitle());
        map.put("description", development.getDescription());
        map.put("developmentType", development.getDevelopmentType());
        map.put("status", development.getStatus());
        map.put("approvalOpinion", development.getApprovalOpinion());
        map.put("approverId", development.getApproverId());
        map.put("approveTime", development.getApproveTime());
        map.put("extraInfo", development.getExtraInfo());
        map.put("startDate", development.getStartDate());
        map.put("endDate", development.getEndDate());
        map.put("createTime", development.getCreateTime());
        map.put("updateTime", development.getUpdateTime());

        // 将extraInfo中的字段展开到顶层,方便前端使用
        if (development.getExtraInfo() != null) {
            map.putAll(development.getExtraInfo());
        }

        return map;
    }
}
