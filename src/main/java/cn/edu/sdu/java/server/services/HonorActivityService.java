package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.DailyActivity;
import cn.edu.sdu.java.server.models.Honor;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.DailyActivityRepository;
import cn.edu.sdu.java.server.repositorys.HonorRepository;
import cn.edu.sdu.java.server.util.ApprovalStateMachine;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class HonorActivityService {
    private final HonorRepository honorRepository;
    private final DailyActivityRepository dailyActivityRepository;

    public HonorActivityService(HonorRepository honorRepository, DailyActivityRepository dailyActivityRepository) {
        this.honorRepository = honorRepository;
        this.dailyActivityRepository = dailyActivityRepository;
    }

    // ==================== 荣誉奖励管理 ====================

    /**
     * 获取荣誉奖励列表
     */
    public DataResponse getHonorList(DataRequest dataRequest) {
        String honorName = dataRequest.getString("honorName");
        String status = dataRequest.getString("status");
        Integer studentId = dataRequest.getInteger("studentId");
        
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
        int size = 20;
        int dataTotal = 0;
        List<Map<String, Object>> dataList = new ArrayList<>();
        
        Pageable pageable = PageRequest.of(cPage != null ? cPage : 0, size);
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
        honor.setStudentName(CommonMethod.getString(form, "studentName"));
        honor.setHonorName(CommonMethod.getString(form, "honorName"));
        honor.setHonorLevel(CommonMethod.getString(form, "honorLevel"));
        
        String awardDateStr = CommonMethod.getString(form, "awardDate");
        if (awardDateStr != null && !awardDateStr.isEmpty()) {
            honor.setAwardDate(java.time.LocalDate.parse(awardDateStr));
        }
        
        honor.setDescription(CommonMethod.getString(form, "description"));
        honor.setCertificateUrl(CommonMethod.getString(form, "certificateUrl"));
        
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

        if (honorId == null || honorId <= 0) {
            return CommonMethod.getReturnMessageError("荣誉ID不能为空");
        }
        if (status == null || status.isEmpty()) {
            return CommonMethod.getReturnMessageError("目标状态不能为空");
        }

        Optional<Honor> op = honorRepository.findById(honorId);
        if (op.isEmpty()) {
            return CommonMethod.getReturnMessageError("荣誉记录不存在");
        }

        Honor honor = op.get();
        String currentStatus = honor.getStatus();
        if (!ApprovalStateMachine.isValidTransition(currentStatus, status)) {
            return CommonMethod.getReturnMessageError(
                    ApprovalStateMachine.getTransitionErrorMessage(currentStatus, status));
        }

        honor.setStatus(status);
        honor.setUpdateTime(LocalDateTime.now());
        honorRepository.save(honor);
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
        
        List<DailyActivity> activityList;
        
        if (studentId != null) {
            activityList = dailyActivityRepository.findByStudentId(studentId);
        } else if (status != null && !status.isEmpty()) {
            activityList = dailyActivityRepository.findByStatus(status);
        } else {
            activityList = dailyActivityRepository.findAll();
        }
        
        // 过滤活动名称
        if (activityName != null && !activityName.isEmpty()) {
            final String searchName = activityName;
            activityList = activityList.stream()
                    .filter(a -> a.getActivityName().contains(searchName))
                    .toList();
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
        int size = 20;
        int dataTotal = 0;
        List<Map<String, Object>> dataList = new ArrayList<>();
        
        Pageable pageable = PageRequest.of(cPage != null ? cPage : 0, size);
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
        activity.setStudentName(CommonMethod.getString(form, "studentName"));
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
