package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.ModifyLog;
import cn.edu.sdu.java.server.models.RequestLog;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.ModifyLogRepository;
import cn.edu.sdu.java.server.repositorys.RequestLogRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * LogService 日志与监控服务类
 */
@Service
public class LogService {
    private final RequestLogRepository requestLogRepository;
    private final ModifyLogRepository modifyLogRepository;
    
    public LogService(RequestLogRepository requestLogRepository, ModifyLogRepository modifyLogRepository) {
        this.requestLogRepository = requestLogRepository;
        this.modifyLogRepository = modifyLogRepository;
    }
    
    /**
     * 获取请求日志列表
     * 支持按时间范围、操作人、操作类型筛选
     */
    public DataResponse getRequestLogList(DataRequest dataRequest) {
        String startTime = dataRequest.getString("startTime");
        String endTime = dataRequest.getString("endTime");
        String username = dataRequest.getString("username");
        
        // 默认查询最近7天的日志
        if (startTime == null || startTime.isEmpty()) {
            LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
            startTime = sevenDaysAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00";
        }
        if (endTime == null || endTime.isEmpty()) {
            endTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 23:59:59";
        }
        
        List<RequestLog> logList;
        if (username != null && !username.isEmpty()) {
            logList = requestLogRepository.findByUsernameAndTimeRange(username, startTime, endTime);
        } else {
            logList = requestLogRepository.findByTimeRange(startTime, endTime);
        }
        
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (RequestLog log : logList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("url", log.getUrl());
            map.put("username", log.getUsername());
            map.put("startTime", log.getStartTime());
            map.put("requestTime", log.getRequestTime());
            dataList.add(map);
        }
        
        return CommonMethod.getReturnData(dataList);
    }
    
    /**
     * 获取修改日志列表
     * 支持按时间范围、操作人、操作类型筛选
     */
    public DataResponse getModifyLogList(DataRequest dataRequest) {
        String startTime = dataRequest.getString("startTime");
        String endTime = dataRequest.getString("endTime");
        String operatorId = dataRequest.getString("operatorId");
        String operationType = dataRequest.getString("operationType");
        
        // 默认查询最近7天的日志
        if (startTime == null || startTime.isEmpty()) {
            LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
            startTime = sevenDaysAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00";
        }
        if (endTime == null || endTime.isEmpty()) {
            endTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 23:59:59";
        }
        
        // 获取所有修改日志，然后在内存中过滤
        List<ModifyLog> allLogs = modifyLogRepository.findAll();
        List<ModifyLog> filteredLogs = new ArrayList<>();
        
        for (ModifyLog log : allLogs) {
            // 按时间范围过滤
            if (log.getOperateTime() != null && 
                log.getOperateTime().compareTo(startTime) >= 0 && 
                log.getOperateTime().compareTo(endTime) <= 0) {
                
                // 按操作人过滤
                if (operatorId != null && !operatorId.isEmpty()) {
                    if (log.getOperatorId() != null && log.getOperatorId().toString().equals(operatorId)) {
                        // 按操作类型过滤
                        if (operationType != null && !operationType.isEmpty()) {
                            if (operationType.equals(log.getType())) {
                                filteredLogs.add(log);
                            }
                        } else {
                            filteredLogs.add(log);
                        }
                    }
                } else {
                    // 按操作类型过滤
                    if (operationType != null && !operationType.isEmpty()) {
                        if (operationType.equals(log.getType())) {
                            filteredLogs.add(log);
                        }
                    } else {
                        filteredLogs.add(log);
                    }
                }
            }
        }
        
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (ModifyLog log : filteredLogs) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("type", log.getType());
            map.put("tableName", log.getTableName());
            map.put("info", log.getInfo());
            map.put("operateTime", log.getOperateTime());
            map.put("operatorId", log.getOperatorId());
            dataList.add(map);
        }
        
        return CommonMethod.getReturnData(dataList);
    }
    
    /**
     * 获取系统统计数据
     * 返回系统访问量、活跃用户、接口调用频次等统计数据
     */
    public DataResponse getSystemStatistics(DataRequest dataRequest) {
        String startTime = dataRequest.getString("startTime");
        String endTime = dataRequest.getString("endTime");
        
        // 默认查询最近7天的数据
        if (startTime == null || startTime.isEmpty()) {
            LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
            startTime = sevenDaysAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00";
        }
        if (endTime == null || endTime.isEmpty()) {
            endTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 23:59:59";
        }
        
        // 获取请求日志
        List<RequestLog> requestLogs = requestLogRepository.findByTimeRange(startTime, endTime);
        
        // 统计数据
        Map<String, Object> statistics = new HashMap<>();
        
        // 1. 系统访问量
        statistics.put("totalRequests", requestLogs.size());
        
        // 2. 活跃用户数
        Set<String> activeUsers = new HashSet<>();
        for (RequestLog log : requestLogs) {
            if (log.getUsername() != null) {
                activeUsers.add(log.getUsername());
            }
        }
        statistics.put("activeUsers", activeUsers.size());
        
        // 3. 接口调用频次统计（TOP 10）
        Map<String, Integer> urlFrequency = new HashMap<>();
        for (RequestLog log : requestLogs) {
            if (log.getUrl() != null) {
                urlFrequency.put(log.getUrl(), urlFrequency.getOrDefault(log.getUrl(), 0) + 1);
            }
        }
        
        // 排序并取TOP 10
        List<Map.Entry<String, Integer>> sortedUrls = new ArrayList<>(urlFrequency.entrySet());
        sortedUrls.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        List<Map<String, Object>> topUrls = new ArrayList<>();
        for (int i = 0; i < Math.min(10, sortedUrls.size()); i++) {
            Map<String, Object> urlMap = new HashMap<>();
            urlMap.put("url", sortedUrls.get(i).getKey());
            urlMap.put("count", sortedUrls.get(i).getValue());
            topUrls.add(urlMap);
        }
        statistics.put("topUrls", topUrls);
        
        // 4. 平均响应时间
        double totalTime = 0;
        for (RequestLog log : requestLogs) {
            if (log.getRequestTime() != null) {
                totalTime += log.getRequestTime();
            }
        }
        double avgTime = requestLogs.isEmpty() ? 0 : totalTime / requestLogs.size();
        statistics.put("averageResponseTime", String.format("%.2f", avgTime) + "ms");
        
        // 5. 修改操作统计
        List<ModifyLog> modifyLogs = modifyLogRepository.findAll();
        Map<String, Integer> operationTypes = new HashMap<>();
        for (ModifyLog log : modifyLogs) {
            if (log.getType() != null) {
                operationTypes.put(log.getType(), operationTypes.getOrDefault(log.getType(), 0) + 1);
            }
        }
        statistics.put("operationTypes", operationTypes);
        
        Map<String, Object> result = new HashMap<>();
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        result.put("statistics", statistics);
        
        return CommonMethod.getReturnData(result);
    }
}
