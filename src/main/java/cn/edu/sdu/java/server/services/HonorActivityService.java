package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.DailyActivity;
import cn.edu.sdu.java.server.models.Honor;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.DailyActivityRepository;
import cn.edu.sdu.java.server.repositorys.HonorRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.util.ApprovalStateMachine;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import cn.edu.sdu.java.server.models.ApprovalRecord;
import cn.edu.sdu.java.server.repositorys.ApprovalRecordRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class HonorActivityService {
    private final HonorRepository honorRepository;
    private final DailyActivityRepository dailyActivityRepository;
    private final ApprovalRecordRepository approvalRecordRepository;
    private final StudentRepository studentRepository;
    private final TeacherDataScopeService teacherDataScopeService;

    public HonorActivityService(HonorRepository honorRepository,
                                DailyActivityRepository dailyActivityRepository,
                                ApprovalRecordRepository approvalRecordRepository,
                                StudentRepository studentRepository,
                                TeacherDataScopeService teacherDataScopeService) {
        this.honorRepository = honorRepository;
        this.dailyActivityRepository = dailyActivityRepository;
        this.approvalRecordRepository = approvalRecordRepository;
        this.studentRepository = studentRepository;
        this.teacherDataScopeService = teacherDataScopeService;
    }


    // ==================== 荣誉奖励管理 ====================

    /**
     * 获取荣誉奖励列表
     */
    public DataResponse getHonorList(DataRequest dataRequest) {
        String honorName = dataRequest.getString("honorName");
        String status = dataRequest.getString("status");
        Integer studentId = dataRequest.getInteger("studentId");
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            studentId = CommonMethod.getPersonId();
        }
        
        List<Honor> honorList;
        
        if (studentId != null) {
            honorList = honorRepository.findByStudentId(studentId);
        } else if (status != null && !status.isEmpty()) {
            honorList = honorRepository.findByStatus(status);
        } else {
            honorList = honorRepository.findAll();
        }
        
        if (honorName != null && !honorName.isEmpty()) {
            final String searchName = honorName;
            honorList = honorList.stream()
                    .filter(h -> containsKeyword(h.getHonorName(), searchName))
                    .toList();
        }
        if (teacherDataScopeService.isCurrentRoleTeacher()) {
            Set<Integer> allowedStudentIds = teacherDataScopeService.getCurrentTeacherStudentIds();
            if (allowedStudentIds.isEmpty()) {
                honorList = new ArrayList<>();
            } else {
                honorList = honorList.stream()
                        .filter(h -> h.getStudentId() != null && allowedStudentIds.contains(h.getStudentId()))
                        .toList();
            }
        }
        
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Honor honor : honorList) {
            dataList.add(getMapFromHonor(honor));
        }
        
        return CommonMethod.getReturnData(dataList);
    }

    /**
     * 分页获取荣誉奖励数据
     */
    public DataResponse getHonorPageData(DataRequest dataRequest) {
        String honorName = dataRequest.getString("honorName");
        String status = dataRequest.getString("status");
        Integer studentId = dataRequest.getInteger("studentId");
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            studentId = CommonMethod.getPersonId();
        }
        Integer cPage = dataRequest.getCurrentPage();
        int pageIndex = cPage != null ? cPage : 0;
        int size = 20;
        int dataTotal = 0;
        List<Map<String, Object>> dataList = new ArrayList<>();

        if (teacherDataScopeService.isCurrentRoleTeacher()) {
            Set<Integer> allowedStudentIds = teacherDataScopeService.getCurrentTeacherStudentIds();
            List<Honor> filteredList;
            if (allowedStudentIds.isEmpty()) {
                filteredList = new ArrayList<>();
            } else {
                filteredList = honorRepository.findAll().stream()
                        .filter(h -> h.getStudentId() != null && allowedStudentIds.contains(h.getStudentId()))
                        .filter(h -> honorName == null || honorName.isEmpty()
                                || (h.getHonorName() != null && h.getHonorName().contains(honorName)))
                        .filter(h -> status == null || status.isEmpty() || status.equals(h.getStatus()))
                        .toList();
            }
            dataTotal = filteredList.size();
            int fromIndex = Math.min(pageIndex * size, dataTotal);
            int toIndex = Math.min(fromIndex + size, dataTotal);
            for (Honor honor : filteredList.subList(fromIndex, toIndex)) {
                dataList.add(getMapFromHonor(honor));
            }
        } else {
            Pageable pageable = PageRequest.of(pageIndex, size);
            Page<Honor> page = honorRepository.findAll(pageable);
            if (page != null) {
                dataTotal = (int) page.getTotalElements();
                List<Honor> list = page.getContent();

                for (Honor honor : list) {
                    boolean match = true;
                    if (honorName != null && !honorName.isEmpty()) {
                        if (!containsKeyword(honor.getHonorName(), honorName)) {
                            match = false;
                        }
                    }
                    if (status != null && !status.isEmpty()) {
                        if (!status.equals(honor.getStatus())) {
                            match = false;
                        }
                    }
                    if (studentId != null && !Objects.equals(studentId, honor.getStudentId())) {
                        match = false;
                    }

                    if (match) {
                        dataList.add(getMapFromHonor(honor));
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
     * 保存荣誉奖励
     */
    public DataResponse honorSave(DataRequest dataRequest) {
        Map<String, Object> form = dataRequest.getMap("form");
        if (form == null || form.isEmpty()) {
            form = dataRequest.getData() != null ? dataRequest.getData() : new HashMap<>();
        }
        String roleName = CommonMethod.getRoleName();
        if (!"ROLE_STUDENT".equals(roleName) && !"ROLE_ADMIN".equals(roleName)) {
            throw new BusinessException(ErrorCodes.ACCESS_DENIED, "当前角色无权新增或编辑荣誉记录");
        }
        Integer honorId = CommonMethod.getInteger(form, "honorId");
        if (honorId == null) {
            honorId = CommonMethod.getInteger(form, "id");
        }
        Integer studentId = CommonMethod.getInteger(form, "studentId");
        String studentName = CommonMethod.getString(form, "studentName");
        Integer currentPersonId = CommonMethod.getPersonId();
        if ("ROLE_STUDENT".equals(roleName)) {
            if (currentPersonId == null) {
                throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "未识别到当前学生身份，无法保存");
            }
            Student student = studentRepository.findByPersonPersonId(currentPersonId)
                    .filter(s -> s.getPerson() != null)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "当前学生信息不存在，无法保存"));
            studentId = currentPersonId;
            studentName = student.getPerson().getName();
        }
        
        Honor honor = null;
        String currentStatus = "draft";
        
        if (honorId != null && honorId > 0) {
            Optional<Honor> op = honorRepository.findById(honorId);
            if (op.isPresent()) {
                honor = op.get();
                if ("ROLE_STUDENT".equals(roleName)) {
                    validateStudentHonorOwnership(honor, currentPersonId);
                }
                currentStatus = honor.getStatus() != null ? honor.getStatus() : "draft";
                if ("ROLE_STUDENT".equals(roleName)
                        && ("submitted".equals(currentStatus) || "approved".equals(currentStatus))) {
                    throw new BusinessException(ErrorCodes.ACCESS_DENIED, "已提交或已审批的荣誉记录不允许再次编辑");
                }
                honor.setUpdateTime(LocalDateTime.now());
            }
        }
        
        if (honor == null) {
            honor = new Honor();
            honor.setCreateTime(LocalDateTime.now());
        }

        String targetStatus = CommonMethod.getString(form, "status");
        if (targetStatus == null || targetStatus.isEmpty()) {
            targetStatus = "draft";
        }
        if ("ROLE_STUDENT".equals(roleName)) {
            if (!"draft".equals(targetStatus) && !"submitted".equals(targetStatus)) {
                throw new BusinessException(ErrorCodes.HONOR_STATUS_INVALID, "学生仅可保存草稿或提交待审批");
            }
            if (honorId != null && honorId > 0
                    && !currentStatus.equals(targetStatus)
                    && !ApprovalStateMachine.isValidTransition(currentStatus, targetStatus)) {
                throw new BusinessException(
                        ErrorCodes.HONOR_STATUS_INVALID,
                        ApprovalStateMachine.getTransitionErrorMessage(currentStatus, targetStatus));
            }
        } else {
            if (honorId == null || honorId <= 0) {
                if (!"draft".equals(targetStatus)) {
                    throw new BusinessException(ErrorCodes.ACCESS_DENIED, "管理员新增荣誉记录时不能直接提交");
                }
            } else if (!Objects.equals(currentStatus, targetStatus)) {
                throw new BusinessException(ErrorCodes.ACCESS_DENIED, "管理员编辑荣誉记录时不能变更提交状态");
            }
        }
        
        honor.setStudentId(studentId);
        honor.setStudentName(studentName);
        honor.setHonorName(CommonMethod.getString(form, "honorName"));
        honor.setHonorLevel(CommonMethod.getString(form, "honorLevel"));
        
        String awardDateStr = CommonMethod.getString(form, "awardDate");
        if (awardDateStr != null && !awardDateStr.isEmpty()) {
            honor.setAwardDate(java.time.LocalDate.parse(awardDateStr));
        }
        
        honor.setDescription(CommonMethod.getString(form, "description"));
        honor.setCertificateUrl(CommonMethod.getString(form, "certificateUrl"));
        honor.setStatus(targetStatus);
        
        honorRepository.save(honor);
        
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 删除荣誉奖励
     */
    public DataResponse honorDelete(DataRequest dataRequest) {
        String roleName = CommonMethod.getRoleName();
        if (!"ROLE_STUDENT".equals(roleName) && !"ROLE_ADMIN".equals(roleName)) {
            throw new BusinessException(ErrorCodes.ACCESS_DENIED, "当前角色无权删除荣誉记录");
        }
        Integer honorId = dataRequest.getInteger("honorId");
        if (honorId == null) {
            honorId = dataRequest.getInteger("id");
        }
        
        if (honorId != null && honorId > 0) {
            Honor honor = honorRepository.findById(honorId)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.HONOR_NOT_FOUND, "荣誉记录不存在"));
            if ("ROLE_STUDENT".equals(roleName)) {
                validateStudentHonorOwnership(honor, CommonMethod.getPersonId());
                String currentStatus = honor.getStatus();
                if (!"draft".equals(currentStatus) && !"rejected".equals(currentStatus)) {
                    throw new BusinessException(ErrorCodes.ACCESS_DENIED, "仅草稿或已驳回的荣誉记录允许删除");
                }
            }
            honorRepository.delete(honor);
        }
        
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 审批荣誉奖励
     */
    public DataResponse honorApprove(DataRequest dataRequest) {
        Integer honorId = dataRequest.getInteger("honorId");
        if (honorId == null) {
            honorId = dataRequest.getInteger("id");
        }
        String status = dataRequest.getString("status");
        String approvalOpinion = dataRequest.getString("approvalOpinion");
    
        if (honorId == null || honorId <= 0) {
            throw new BusinessException(ErrorCodes.HONOR_NOT_FOUND, "荣誉ID不能为空");
        }
        if (status == null || status.isEmpty()) {
            throw new BusinessException(ErrorCodes.HONOR_STATUS_INVALID, "目标状态不能为空");
        }

        Honor honor = honorRepository.findById(honorId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.HONOR_NOT_FOUND, "荣誉记录不存在"));
        String roleName = CommonMethod.getRoleName();
        String currentStatus = honor.getStatus();

        if ("ROLE_STUDENT".equals(roleName)) {
            validateStudentHonorOwnership(honor, CommonMethod.getPersonId());
            if (!"submitted".equals(status)) {
                throw new BusinessException(ErrorCodes.ACCESS_DENIED, "学生仅可提交荣誉记录，不能执行审批");
            }
        } else if ("ROLE_ADMIN".equals(roleName)) {
            if (!"approved".equals(status) && !"rejected".equals(status)) {
                throw new BusinessException(ErrorCodes.ACCESS_DENIED, "管理员仅可审批通过或驳回荣誉记录");
            }
        } else {
            throw new BusinessException(ErrorCodes.ACCESS_DENIED, "当前角色无权操作荣誉审批流程");
        }

        if (!ApprovalStateMachine.isValidTransition(currentStatus, status)) {
            throw new BusinessException(
                    ErrorCodes.HONOR_STATUS_INVALID,
                    ApprovalStateMachine.getTransitionErrorMessage(currentStatus, status));
        }

        honor.setStatus(status);
        if ("ROLE_ADMIN".equals(roleName)) {
            honor.setApprovalOpinion(approvalOpinion);
        }
        honor.setUpdateTime(LocalDateTime.now());
        honorRepository.save(honor);

        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType("honor");
        record.setBusinessId(honor.getId());
        record.setFromStatus(currentStatus);
        record.setToStatus(status);
        record.setApprovalOpinion("ROLE_ADMIN".equals(roleName) ? approvalOpinion : null);
        record.setOperatorId(CommonMethod.getPersonId());
        record.setOperatorName(CommonMethod.getUsername());
        record.setOperateTime(LocalDateTime.now());
        approvalRecordRepository.save(record);

        return CommonMethod.getReturnMessageOK();
    }

    // ==================== 日常活动管理 ====================

    /**
     * 获取日常活动列表
     */
    public DataResponse getDailyActivityList(DataRequest dataRequest) {
        String activityName = dataRequest.getString("activityName");
        String status = dataRequest.getString("status");
        Integer studentId = dataRequest.getInteger("studentId");
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            studentId = CommonMethod.getPersonId();
        }
        String activityType = dataRequest.getString("activityType");

        List<DailyActivity> activityList;

        if (studentId != null) {
            activityList = dailyActivityRepository.findByStudentId(studentId);
        } else if (status != null && !status.isEmpty()) {
            activityList = dailyActivityRepository.findByStatus(status);
        } else {
            activityList = dailyActivityRepository.findAll();
        }

        if (activityType != null && !activityType.isEmpty()) {
            final String typeFilter = activityType;
            activityList = activityList.stream()
                    .filter(a -> typeFilter.equals(a.getActivityType()))
                    .toList();
        }

        if (activityName != null && !activityName.isEmpty()) {
            final String searchName = activityName;
            activityList = activityList.stream()
                    .filter(a -> containsKeyword(a.getActivityName(), searchName))
                    .toList();
        }
        if (teacherDataScopeService.isCurrentRoleTeacher()) {
            Set<Integer> allowedStudentIds = teacherDataScopeService.getCurrentTeacherStudentIds();
            if (allowedStudentIds.isEmpty()) {
                activityList = new ArrayList<>();
            } else {
                activityList = activityList.stream()
                        .filter(a -> a.getStudentId() != null && allowedStudentIds.contains(a.getStudentId()))
                        .toList();
            }
        }

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (DailyActivity activity : activityList) {
            dataList.add(getMapFromActivity(activity));
        }

        return CommonMethod.getReturnData(dataList);
    }

    /**
     * 分页获取日常活动数据
     */
    public DataResponse getDailyActivityPageData(DataRequest dataRequest) {
        String activityName = dataRequest.getString("activityName");
        String status = dataRequest.getString("status");
        Integer cPage = dataRequest.getCurrentPage();
        int pageIndex = cPage != null ? cPage : 0;
        int size = 20;
        int dataTotal = 0;
        List<Map<String, Object>> dataList = new ArrayList<>();

        if (teacherDataScopeService.isCurrentRoleTeacher()) {
            Set<Integer> allowedStudentIds = teacherDataScopeService.getCurrentTeacherStudentIds();
            List<DailyActivity> filteredList;
            if (allowedStudentIds.isEmpty()) {
                filteredList = new ArrayList<>();
            } else {
                filteredList = dailyActivityRepository.findAll().stream()
                        .filter(a -> a.getStudentId() != null && allowedStudentIds.contains(a.getStudentId()))
                        .filter(a -> activityName == null || activityName.isEmpty()
                                || (a.getActivityName() != null && a.getActivityName().contains(activityName)))
                        .filter(a -> status == null || status.isEmpty() || status.equals(a.getStatus()))
                        .toList();
            }
            dataTotal = filteredList.size();
            int fromIndex = Math.min(pageIndex * size, dataTotal);
            int toIndex = Math.min(fromIndex + size, dataTotal);
            for (DailyActivity activity : filteredList.subList(fromIndex, toIndex)) {
                dataList.add(getMapFromActivity(activity));
            }
        } else {
            Pageable pageable = PageRequest.of(pageIndex, size);
            Page<DailyActivity> page = dailyActivityRepository.findAll(pageable);
            if (page != null) {
                dataTotal = (int) page.getTotalElements();
                List<DailyActivity> list = page.getContent();

                for (DailyActivity activity : list) {
                    boolean match = true;
                    if (activityName != null && !activityName.isEmpty()) {
                        if (!containsKeyword(activity.getActivityName(), activityName)) {
                            match = false;
                        }
                    }
                    if (status != null && !status.isEmpty()) {
                        if (!status.equals(activity.getStatus())) {
                            match = false;
                        }
                    }

                    if (match) {
                        dataList.add(getMapFromActivity(activity));
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
     * 保存日常活动
     */
    public DataResponse dailyActivitySave(DataRequest dataRequest) {
        Map<String, Object> form = dataRequest.getMap("form");
        if (form == null || form.isEmpty()) {
            form = dataRequest.getData();
        }
        if (form == null) {
            form = new HashMap<>();
        }
        Integer activityId = CommonMethod.getInteger(form, "activityId");
        Integer studentId = CommonMethod.getInteger(form, "studentId");
        String studentName = CommonMethod.getString(form, "studentName");
        // 日常活动与荣誉模块一样，学生端提交时只允许写入本人数据。
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
        
        DailyActivity activity = null;
        
        if (activityId != null && activityId > 0) {
            Optional<DailyActivity> op = dailyActivityRepository.findById(activityId);
            if (op.isPresent()) {
                activity = op.get();
                activity.setUpdateTime(LocalDateTime.now());
            }
        }
        
        if (activity == null) {
            activity = new DailyActivity();
            activity.setCreateTime(LocalDateTime.now());
        }
        
        activity.setStudentId(studentId);
        activity.setStudentName(studentName);
        activity.setActivityName(CommonMethod.getString(form, "activityName"));
        activity.setActivityType(CommonMethod.getString(form, "activityType"));
        
        String activityDateStr = CommonMethod.getString(form, "activityDate");
        if (activityDateStr != null && !activityDateStr.isEmpty()) {
            activity.setActivityDate(java.time.LocalDate.parse(activityDateStr));
        }
        
        activity.setDescription(CommonMethod.getString(form, "description"));
        activity.setHours(CommonMethod.getInteger(form, "hours"));
        
        String statusStr = CommonMethod.getString(form, "status");
        activity.setStatus(statusStr != null && !statusStr.isEmpty() ? statusStr : "draft");
        
        dailyActivityRepository.save(activity);
        
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 删除日常活动
     */
    public DataResponse dailyActivityDelete(DataRequest dataRequest) {
        Integer activityId = dataRequest.getInteger("activityId");
        
        if (activityId != null && activityId > 0) {
            Optional<DailyActivity> op = dailyActivityRepository.findById(activityId);
            op.ifPresent(dailyActivityRepository::delete);
        }
        
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 审批日常活动
     */
    public DataResponse dailyActivityApprove(DataRequest dataRequest) {
        Integer activityId = dataRequest.getInteger("activityId");
        if (activityId == null) {
            activityId = dataRequest.getInteger("id");
        }
        String status = dataRequest.getString("status");
        String approvalOpinion = dataRequest.getString("approvalOpinion");

        if (activityId == null || activityId <= 0) {
            throw new BusinessException(ErrorCodes.DAILY_ACTIVITY_NOT_FOUND, "活动ID不能为空");
        }
        if (status == null || status.isEmpty()) {
            throw new BusinessException(ErrorCodes.DAILY_ACTIVITY_STATUS_INVALID, "目标状态不能为空");
        }

        DailyActivity activity = dailyActivityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.DAILY_ACTIVITY_NOT_FOUND, "活动记录不存在"));
        if (teacherDataScopeService.isCurrentRoleTeacher()
                && !teacherDataScopeService.canCurrentTeacherAccessStudent(activity.getStudentId())) {
            throw new BusinessException(ErrorCodes.ACCESS_DENIED, "仅可审批本人授课学生提交的数据");
        }
        String currentStatus = activity.getStatus();
        // 日常活动审批也沿用统一状态机，确保教师审批和管理员终审顺序稳定。
        if (!ApprovalStateMachine.isValidTransition(currentStatus, status)) {
            throw new BusinessException(
                    ErrorCodes.DAILY_ACTIVITY_STATUS_INVALID,
                    ApprovalStateMachine.getTransitionErrorMessage(currentStatus, status));
        }

        activity.setStatus(status);
        activity.setApprovalOpinion(approvalOpinion);
        activity.setUpdateTime(LocalDateTime.now());
        dailyActivityRepository.save(activity);

        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType("activity");
        record.setBusinessId(activity.getId());
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

    private void validateStudentHonorOwnership(Honor honor, Integer currentStudentId) {
        if (currentStudentId == null || !Objects.equals(currentStudentId, honor.getStudentId())) {
            throw new BusinessException(ErrorCodes.ACCESS_DENIED, "仅可操作本人荣誉记录");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 将 Honor 转换为 Map
     */
    private Map<String, Object> getMapFromHonor(Honor honor) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", honor.getId());
        map.put("studentId", honor.getStudentId());
        map.put("studentName", honor.getStudentName());
        map.put("honorName", honor.getHonorName());
        map.put("honorLevel", honor.getHonorLevel());
        map.put("awardDate", honor.getAwardDate());
        map.put("description", honor.getDescription());
        map.put("certificateUrl", honor.getCertificateUrl());
        map.put("status", honor.getStatus());
        map.put("approvalOpinion", honor.getApprovalOpinion());
        map.put("createTime", honor.getCreateTime());
        map.put("updateTime", honor.getUpdateTime());
        return map;
    }

    /**
     * 将 DailyActivity 转换为 Map
     */
    private Map<String, Object> getMapFromActivity(DailyActivity activity) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", activity.getId());
        map.put("studentId", activity.getStudentId());
        map.put("studentName", activity.getStudentName());
        map.put("activityName", activity.getActivityName());
        map.put("activityType", activity.getActivityType());
        map.put("activityDate", activity.getActivityDate());
        map.put("description", activity.getDescription());
        map.put("hours", activity.getHours());
        map.put("status", activity.getStatus());
        map.put("createTime", activity.getCreateTime());
        map.put("updateTime", activity.getUpdateTime());
        return map;
    }
}
