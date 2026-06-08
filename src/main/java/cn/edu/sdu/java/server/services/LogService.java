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
     * 权限：ADMIN可查看所有日志，TEACHER/STUDENT只能查看自己的操作日志
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
        
        // 权限控制：非管理员只能查看自己的日志
        String currentRole = CommonMethod.getRoleName();
        if (!"ROLE_ADMIN".equals(currentRole)) {
            // 非管理员，强制使用当前登录用户名
            username = CommonMethod.getUsername();
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
     * 权限：ADMIN可查看所有日志，TEACHER/STUDENT只能查看自己的操作日志
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
        
        // 权限控制：非管理员只能查看自己的日志
        String currentRole = CommonMethod.getRoleName();
        Integer currentPersonId = CommonMethod.getPersonId();
        if (!"ROLE_ADMIN".equals(currentRole)) {
            // 非管理员，强制使用当前登录用户的personId
            operatorId = currentPersonId != null ? currentPersonId.toString() : null;
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
            String originalUrl = sortedUrls.get(i).getKey();
            urlMap.put("url", getUrlDisplayName(originalUrl)); // 返回中文名称
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
        
        // 5. 面板操作统计（按控制器/面板分组）
        List<ModifyLog> modifyLogs = modifyLogRepository.findAll();
        Map<String, Integer> panelMap = new LinkedHashMap<>();
        for (ModifyLog log : modifyLogs) {
            if (log.getTableName() != null && !log.getTableName().isEmpty()) {
                String panelName = getPanelDisplayName(log.getTableName());
                panelMap.put(panelName, panelMap.getOrDefault(panelName, 0) + 1);
            }
        }
        List<Map<String, Object>> panelStats = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : panelMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("count", entry.getValue());
            panelStats.add(item);
        }
        panelStats.sort((a, b) -> ((Integer) b.get("count")).compareTo((Integer) a.get("count")));
        statistics.put("panelStats", panelStats);

        // 6. 面板趋势数据（按日/按月分组）
        String panelTrendGranularity = dataRequest.getString("panelTrendGranularity");
        String panelFilter = dataRequest.getString("panelName");
        if (panelTrendGranularity == null || panelTrendGranularity.isEmpty()) {
            panelTrendGranularity = "day";
        }

        List<ModifyLog> filteredModifyLogs = new ArrayList<>();
        for (ModifyLog log : modifyLogs) {
            if (log.getOperateTime() != null &&
                log.getOperateTime().compareTo(startTime) >= 0 &&
                log.getOperateTime().compareTo(endTime) <= 0) {
                if (panelFilter != null && !panelFilter.isEmpty()) {
                    String displayName = getPanelDisplayName(log.getTableName());
                    if (displayName.equals(panelFilter)) {
                        filteredModifyLogs.add(log);
                    }
                } else {
                    filteredModifyLogs.add(log);
                }
            }
        }

        Map<String, Integer> trendMap = new TreeMap<>();
        DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter outputFmt = "month".equals(panelTrendGranularity)
                ? DateTimeFormatter.ofPattern("yyyy-MM")
                : DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (ModifyLog log : filteredModifyLogs) {
            try {
                String dateKey = java.time.LocalDateTime.parse(log.getOperateTime(), inputFmt).format(outputFmt);
                trendMap.put(dateKey, trendMap.getOrDefault(dateKey, 0) + 1);
            } catch (Exception ignored) {}
        }
        List<Map<String, Object>> panelTrend = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : trendMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", entry.getKey());
            item.put("count", entry.getValue());
            panelTrend.add(item);
        }
        statistics.put("panelTrend", panelTrend);

        // 7. 可用面板列表（与图表排序一致）
        List<String> panelList = new ArrayList<>();
        for (Map<String, Object> item : panelStats) {
            panelList.add((String) item.get("name"));
        }
        statistics.put("panelList", panelList);
        
        Map<String, Object> result = new HashMap<>();
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        result.put("statistics", statistics);
        
        return CommonMethod.getReturnData(result);
    }

    /**
     * 将接口URL映射为中文显示名称
     */
    private String getUrlDisplayName(String url) {
        if (url == null || url.isEmpty()) return "未知接口";
        
        // 提取接口路径中的关键部分进行映射
        return switch (url) {
            // 认证相关
            case "/api/auth/login" -> "用户登录";
            case "/api/auth/logout" -> "用户登出";
            case "/api/auth/getUserInfo" -> "获取用户信息";
            case "/api/auth/updatePassword" -> "修改密码";
            
            // 学生管理
            case "/api/student/getStudentList" -> "学生列表查询";
            case "/api/student/studentEditSave" -> "学生信息编辑";
            case "/api/student/deleteStudent" -> "删除学生";
            case "/api/student/studentInfo" -> "学生信息查询";
            case "/api/student/mpHonList" -> "学生荣誉列表";
            case "/api/student_pHonTrend" -> "学生荣誉趋势";
            case "/api/student_pHonStats" -> "学生荣誉统计";
            
            // 教师管理
            case "/api/teacher/getTeacherList" -> "教师列表查询";
            case "/api/teacher/teacherEditSave" -> "教师信息编辑";
            case "/api/teacher/teachIn" -> "教师任课";
            
            // 课程管理
            case "/api/course/getCourseList" -> "课程列表查询";
            case "/api/course/courseEditSave" -> "课程信息编辑";
            case "/api/courseSelection/getSelectionList" -> "选课列表查询";
            case "/api/courseSelection/saveSelection" -> "保存选课";
            case "/api/course/teachIn" -> "课程任教";
            
            // 成绩管理
            case "/api/score/getScoreList" -> "成绩列表查询";
            case "/api/score/scoreEditSave" -> "成绩编辑";
            
            // 作业管理
            case "/api/homework/getHomeworkList" -> "作业列表查询";
            case "/api/homework/homeworkEditSave" -> "作业编辑";
            case "/api/homework/submitHomework" -> "提交作业";
            
            // 考勤管理
            case "/api/attendance/getAttendanceList" -> "考勤列表查询";
            case "/api/attendance/attendanceEditSave" -> "考勤编辑";
            
            // 荣誉与处分
            case "/api/honor/getHonorList" -> "荣誉列表查询";
            case "/api/honor/honorSave" -> "荣誉编辑";
            case "/api/honor/ge::tHonorList" -> "荣誉列表查询";
            case "/api/punishment/getPunishmentList" -> "处分列表查询";
            case "/api/punishment/punishmentSave" -> "处分编辑";
            
            // 创新创业
            case "/api/innovation/getInnovationList" -> "创新项目列表";
            case "/api/innovation/innovationSave" -> "创新项目编辑";
            
            // 日常活动
            case "/api/activity/getActivityList" -> "活动列表查询";
            case "/api/activity/activitySave" -> "活动编辑";
            
            // 社会实践
            case "/api/social-practice/getPracticeList" -> "实践列表查询";
            case "/api/social-practice/practiceSave" -> "实践编辑";
            
            // 请假管理
            case "/api/studentLeave/getLeaveList" -> "请假列表查询";
            case "/api/studentLeave/leaveSave" -> "请假申请";
            
            // 考试管理
            case "/api/exam/getExamList" -> "考试列表查询";
            case "/api/exam/examSave" -> "考试编辑";
            
            // 消费管理
            case "/api/consumption/getConsumptionList" -> "消费记录查询";
            
            // 系统统计与监控
            case "/api/statistics/getMainPageData" -> "数据看板加载";
            case "/api/statistics::InPageData" -> "数据看板加载";
            case "/api/log/getSystemStatistics" -> "系统统计查询";
            case "/api/log/get_Statistics" -> "系统统计查询";
            case "/api/log/getRequestLogList" -> "请求日志查询";
            case "/api/log/getModifyLogList" -> "修改日志查询";
            
            // 用户管理
            case "/api/user/getUserList" -> "用户列表查询";
            case "/api/user/userEditSave" -> "用户编辑";
            
            // 简历管理
            case "/api/resume/getResumeList" -> "简历列表查询";
            case "/api/resume/resumeSave" -> "简历编辑";
            
            // 基础数据管理
            case "/api/base/getMenuList" -> "菜单列表查询";
            case "/api/base/getDatabaseUserName" -> "获取数据库用户名";
            case "/api/base/getDataBaseUserName" -> "获取数据库用户名";
            
            // 班级与选项管理
            case "/api/class/getClassList" -> "班级列表查询";
            case "/api/base/getOptionList" -> "选项列表查询";
            
            // 荣誉与活动（含异常格式）
            case "/api/honor/getActivityList", "/api/honor/g::tivityList", "/api/honor::tivityList" -> "活动列表查询";
            
            // 学生管理
            case "/api/student/getStudentInfo" -> "学生详情查询";
            case "/api/student/getCampOptionList" -> "营地选项查询";
            case "/api/student/getMptionList" -> "选项查询";
            case "/api/student/getMptionStati" -> "选项统计";
            
            // 截图中的接口路径（可能含有异常格式）
            case "/api/student/getM..ptionList" -> "选项列表查询";
            case "/api/student/getM..ptionStati" -> "选项统计查询";
            case "/api/student/getM..ptionTrend" -> "选项趋势查询";
            case "/api/student/getM..ptionStats" -> "选项统计";
            case "/api/honor/getDal..tivityList" -> "活动列表查询";
            
            // 默认处理：智能翻译引擎 - 自动将英文接口路径翻译为中文
            default -> {
                String simplified = url.replace("/api/", "");
                // 处理含有 :: 或 .. 的异常格式
                simplified = simplified.replace("::", "").replace("..", "");
                
                if (simplified.contains("/")) {
                    String[] parts = simplified.split("/");
                    if (parts.length >= 2) {
                        String module = parts[0].toLowerCase();
                        String action = parts[1];
                        
                        // 使用翻译方法
                        String moduleCN = translateModule(module);
                        String actionCN = translateAction(action);
                        
                        yield moduleCN + actionCN;
                    }
                }
                // 如果无法解析，返回原始简化路径
                yield simplified;
            }
        };
    }
    
    /**
     * 翻译模块名称
     */
    private String translateModule(String module) {
        return switch (module.toLowerCase()) {
            case "student" -> "学生";
            case "teacher" -> "教师";
            case "course" -> "课程";
            case "homework" -> "作业";
            case "exam" -> "考试";
            case "attendance" -> "考勤";
            case "honor" -> "荣誉";
            case "punishment" -> "处分";
            case "activity" -> "活动";
            case "innovation" -> "创新";
            case "practice", "social" -> "实践";
            case "score" -> "成绩";
            case "consumption" -> "消费";
            case "leave" -> "请假";
            case "resume" -> "简历";
            case "development" -> "发展";
            case "base" -> "基础";
            case "menu" -> "菜单";
            case "user" -> "用户";
            case "statistics" -> "统计";
            case "log" -> "日志";
            case "auth" -> "认证";
            case "class" -> "班级";
            case "option" -> "选项";
            case "camp" -> "营地";
            default -> module;
        };
    }
    
    /**
     * 翻译操作名称（支持驼峰命名）
     */
    private String translateAction(String action) {
        String result = action;
        
        // 清理异常字符
        result = result.replace("::", "").replace("..", "");
        
        // 第一步：先翻译复合词汇（必须在简单词汇之前）
        result = result
            .replace("DailyActivity", "日常活动")
            .replace("dailyActivity", "日常活动")
            .replace("MonthlyConsu", "月度消费")
            .replace("monthlyConsu", "月度消费")
            .replace("CampOption", "营地选项")
            .replace("campOption", "营地选项")
            .replace("Consu", "消费")
            .replace("consu", "消费");
        
        // 第二步：翻译常见操作词汇
        result = result
            .replace("get", "查询")
            .replace("Get", "查询")
            .replace("save", "保存")
            .replace("Save", "保存")
            .replace("edit", "编辑")
            .replace("Edit", "编辑")
            .replace("delete", "删除")
            .replace("Delete", "删除")
            .replace("add", "添加")
            .replace("Add", "添加")
            .replace("submit", "提交")
            .replace("Submit", "提交")
            .replace("List", "列表")
            .replace("list", "列表")
            .replace("Info", "详情")
            .replace("info", "详情")
            .replace("Trend", "趋势")
            .replace("trend", "趋势")
            .replace("Stati", "统计")
            .replace("stati", "统计")
            .replace("Stats", "统计")
            .replace("stats", "统计")
            .replace("Dal", "日常")
            .replace("dal", "日常")
            .replace("Mption", "选项")
            .replace("mption", "选项");
        
        // 第三步：移除驼峰分隔，合并中文
        result = result.replaceAll("([\u4e00-\u9fa5])([A-Z])", "$1$2");
        result = result.replaceAll("([a-z])([\u4e00-\u9fa5])", "$1$2");
        
        return result;
    }
    
    /**
     * 翻译控制器名称
     */
    private String translateControllerName(String name) {
        String result = name;
        
        // 第一步：先移除所有 Control 相关后缀（必须在最前面，按长度从长到短）
        result = result
            .replace("Controller", "")
            .replace("Controll", "")
            .replace("Controlle", "")
            .replace("Controll", "")
            .replace("ntroller", "")
            .replace("ntrolle", "")
            .replace("ntrol", "")
            .replace("Control", "")
            .replace("Contro", "")
            .replace("Cont", "")
            .replace("Con", "")
            .replace("Co", "");
        
        // 第二步：翻译复合词汇（必须在简单词汇之前）
        result = result
            .replace("SocialPractice", "社会实践")
            .replace("CourseTeaching", "课程任课")
            .replace("CourseSelection", "选课")
            .replace("DailyActivity", "日常活动")
            .replace("HonorActivity", "荣誉活动")
            .replace("StudentLeave", "请假")
            .replace("StudentStats", "学生统计")
            .replace("DevelopmentCo", "个人发展");
        
        // 第三步：翻译常见控制器名称
        result = result
            .replace("Student", "学生")
            .replace("Teacher", "教师")
            .replace("Course", "课程")
            .replace("Homework", "作业")
            .replace("Exam", "考试")
            .replace("Attendance", "考勤")
            .replace("Honor", "荣誉")
            .replace("Activity", "活动")
            .replace("Innovation", "创新")
            .replace("Practice", "实践")
            .replace("Score", "成绩")
            .replace("Consumption", "消费")
            .replace("Leave", "请假")
            .replace("Resume", "简历")
            .replace("Development", "发展")
            .replace("Base", "基础")
            .replace("Menu", "菜单")
            .replace("User", "用户")
            .replace("Statistics", "统计")
            .replace("Log", "日志")
            .replace("Auth", "认证")
            .replace("Class", "班级")
            .replace("Selection", "选课")
            .replace("Teaching", "任课")
            .replace("Punishment", "处分")
            .replace("Social", "社会")
            .replace("Legacy", "传统")
            .replace("Test", "测试")
            .replace("Register", "注册");
        
        return result;
    }
    
    /**
     * 将控制器类名映射为中文面板名称
     */
    private String getPanelDisplayName(String controllerName) {
        if (controllerName == null) return "其他";
        return switch (controllerName) {
            // 基础控制器
            case "BaseController" -> "基础数据面板";
            case "ConsumptionController", "ConsumptionControlle" -> "消费面板";
            
            // 日志与监控
            case "LogController" -> "日志管理";
            case "StatisticsController", "StatisticsControll" -> "数据看板";
            
            // 学生与教师管理
            case "StudentController" -> "学生管理";
            case "TeacherController" -> "教师管理";
            case "StudentLeaveController" -> "请假管理";
            case "StudentStatisticsController" -> "学生统计";
            
            // 课程与成绩
            case "CourseController" -> "课程管理";
            case "CourseSelectionController" -> "选课管理";
            case "CourseTeachingController" -> "任课管理";
            case "ScoreController" -> "成绩管理";
            
            // 作业与考试
            case "HomeworkController", "HomeworkControll" -> "作业管理";
            case "ExamController" -> "考试管理";
            
            // 考勤管理
            case "AttendanceController" -> "考勤管理";
            
            // 荣誉与处分
            case "HonorController", "HonorActivityController" -> "荣誉处分";
            case "PunishmentController" -> "处分管理";
            
            // 创新创业与活动
            case "InnovationController", "InnovationContro" -> "创新创业";
            case "ActivityController", "ActivityControll" -> "日常活动";
            
            // 社会实践
            case "PracticeController", "PracticeControll", "SocialPracticeController" -> "社会实践";
            
            // 系统管理
            case "UserController" -> "账号管理";
            case "AuthController" -> "认证面板";
            case "RegisterController" -> "注册面板";
            
            // 简历与个人发展
            case "ResumeController" -> "简历管理";
            case "DevelopmentController" -> "个人发展";
            
            // 其他（处理名称截断的情况）
            case "Base面板", "Base" -> "基础数据面板";
            case "HonorActivity面板", "HonorActivity.." -> "荣誉处分面板";
            case "LegacyAuth面板", "LegacyAuth" -> "认证面板";
            case "StudentLeave面板", "StudentLeav.." -> "请假管理面板";
            case "Attendance面板" -> "考勤管理面板";
            case "DevelopmentCo..", "DevelopmentCo" -> "个人发展面板";
            case "CourseSelecti..", "CourseSelecti" -> "选课管理面板";
            case "Teacher面板" -> "教师管理面板";
            case "StudentStats..", "StudentStats" -> "学生统计面板";
            case "SocialPractic..", "SocialPractic" -> "社会实践面板";
            case "Test面板", "Test" -> "测试面板";
            case "Punishment面板" -> "处分管理面板";
            case "CourseTeachin..", "CourseTeachin" -> "任课管理面板";
            case "Homework面板" -> "作业管理面板";
            case "Activity面板" -> "活动管理面板";
            case "Innovation面板" -> "创新创业面板";
            case "Practice面板" -> "社会实践面板";
            case "Resume面板" -> "简历管理面板";
            
            default -> {
                String name = controllerName;
                // 清理后缀
                if (name.endsWith("Controller")) name = name.substring(0, name.length() - 10);
                else if (name.endsWith("Controll")) name = name.substring(0, name.length() - 8);
                else if (name.endsWith("面板")) name = name.substring(0, name.length() - 2);
                else if (name.endsWith("..")) name = name.substring(0, name.length() - 2);
                
                // 智能翻译控制器名称
                yield translateControllerName(name) + "面板";
            }
        };
    }
}
