package cn.edu.sdu.java.server.services;

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
    private final ApprovalRecordRepository approvalRecordRepository;  // 添加这行
    private final StudentRepository studentRepository;
    private final TeacherDataScopeService teacherDataScopeService;

    // 修改构造函数
    public HonorActivityService(HonorRepository honorRepository,
                                DailyActivityRepository dailyActivityRepository,
                                ApprovalRecordRepository approvalRecordRepository,
                                StudentRepository studentRepository,
                                TeacherDataScopeService teacherDataScopeService) {  // 添加这个参数
        this.honorRepository = honorRepository;
        this.dailyActivityRepository = dailyActivityRepository;
        this.approvalRecordRepository = approvalRecordRepository;  // 添加这行赋值
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
        
        // 过滤荣誉名称
        if (honorName != null && !honorName.isEmpty()) {
            final String searchName = honorName;
            honorList = honorList.stream()
                    .filter(h -> h.getHonorName().contains(searchName))
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
                        if (!honor.getHonorName().contains(honorName)) {
                            match = false;
                        }
                    }
                    if (status != null && !status.isEmpty()) {
                        if (!status.equals(honor.getStatus())) {
                            match = false;
                        }
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
        Integer honorId = CommonMethod.getInteger(form, "honorId");
        if (honorId == null) {
            honorId = CommonMethod.getInteger(form, "id");
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
        
        Honor honor = null;
        
        if (honorId != null && honorId > 0) {
            Optional<Honor> op = honorRepository.findById(honorId);
            if (op.isPresent()) {
                honor = op.get();
                honor.setUpdateTime(LocalDateTime.now());
            }
        }
        
        if (honor == null) {
            honor = new Honor();
            honor.setCreateTime(LocalDateTime.now());
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
        honor.setApprovalOpinion(CommonMethod.getString(form, "approvalOpinion"));
        
        String statusStr = CommonMethod.getString(form, "status");
        honor.setStatus(statusStr != null && !statusStr.isEmpty() ? statusStr : "draft");
        
        honorRepository.save(honor);
        
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 删除荣誉奖励
     */
    public DataResponse honorDelete(DataRequest dataRequest) {
        Integer honorId = dataRequest.getInteger("honorId");
        if (honorId == null) {
            honorId = dataRequest.getInteger("id");
        }
        
        if (honorId != null && honorId > 0) {
            Optional<Honor> op = honorRepository.findById(honorId);
            op.ifPresent(honorRepository::delete);
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
            return CommonMethod.getReturnMessageError("荣誉 ID 不能为空");
        }
        if (status == null || status.isEmpty()) {
            return CommonMethod.getReturnMessageError("目标状态不能为空");
        }
    
        Optional<Honor> op = honorRepository.findById(honorId);
        if (op.isEmpty()) {
            return CommonMethod.getReturnMessageError("荣誉记录不存在");
        }
    
        Honor honor = op.get();
        if (teacherDataScopeService.isCurrentRoleTeacher()
                && !teacherDataScopeService.canCurrentTeacherAccessStudent(honor.getStudentId())) {
            return CommonMethod.getReturnMessageError("仅可审批本人授课学生提交的数据");
        }
        String currentStatus = honor.getStatus();
        if (!ApprovalStateMachine.isValidTransition(currentStatus, status)) {
            return CommonMethod.getReturnMessageError(
                    ApprovalStateMachine.getTransitionErrorMessage(currentStatus, status));
        }
    
        honor.setStatus(status);
        honor.setApprovalOpinion(approvalOpinion);
        honor.setUpdateTime(LocalDateTime.now());
        honorRepository.save(honor);
        // 保存审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType("honor");
        record.setBusinessId(honor.getId());
        record.setFromStatus(currentStatus);
        record.setToStatus(status);
        record.setApprovalOpinion(approvalOpinion);
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

        // 按活动类型过滤
        if (activityType != null && !activityType.isEmpty()) {
            final String typeFilter = activityType;
            activityList = activityList.stream()
                    .filter(a -> typeFilter.equals(a.getActivityType()))
                    .toList();
        }

        // 按活动名称过滤
        if (activityName != null && !activityName.isEmpty()) {
            final String searchName = activityName;
            activityList = activityList.stream()
                    .filter(a -> a.getActivityName().contains(searchName))
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
                        if (!activity.getActivityName().contains(activityName)) {
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
        Integer activityId = CommonMethod.getInteger(form, "activityId");
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
            return CommonMethod.getReturnMessageError("活动 ID 不能为空");
        }
        if (status == null || status.isEmpty()) {
            return CommonMethod.getReturnMessageError("目标状态不能为空");
        }

        Optional<DailyActivity> op = dailyActivityRepository.findById(activityId);
        if (op.isEmpty()) {
            return CommonMethod.getReturnMessageError("活动记录不存在");
        }

        DailyActivity activity = op.get();
        if (teacherDataScopeService.isCurrentRoleTeacher()
                && !teacherDataScopeService.canCurrentTeacherAccessStudent(activity.getStudentId())) {
            return CommonMethod.getReturnMessageError("仅可审批本人授课学生提交的数据");
        }
        String currentStatus = activity.getStatus();
        if (!ApprovalStateMachine.isValidTransition(currentStatus, status)) {
            return CommonMethod.getReturnMessageError(
                    ApprovalStateMachine.getTransitionErrorMessage(currentStatus, status));
        }

        activity.setStatus(status);
        activity.setApprovalOpinion(approvalOpinion);
        activity.setUpdateTime(LocalDateTime.now());
        dailyActivityRepository.save(activity);
        // 保存审批记录
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
