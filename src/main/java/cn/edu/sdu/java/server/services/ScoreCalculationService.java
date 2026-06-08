package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Attendance;
import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.DailyActivity;
import cn.edu.sdu.java.server.models.Honor;
import cn.edu.sdu.java.server.models.Score;
import cn.edu.sdu.java.server.models.StudentDevelopment;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.AttendanceRepository;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.DailyActivityRepository;
import cn.edu.sdu.java.server.repositorys.DevelopmentRepository;
import cn.edu.sdu.java.server.repositorys.HonorRepository;
import cn.edu.sdu.java.server.repositorys.ScoreRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * ScoreCalculationService 综合绩分计算引擎
 */
@Service
public class ScoreCalculationService {
    private static final Logger log = LoggerFactory.getLogger(ScoreCalculationService.class);
    
    private final ScoreRepository scoreRepository;
    private final CourseRepository courseRepository;
    private final StudentService studentService;
    private final AttendanceRepository attendanceRepository;
    private final DailyActivityRepository dailyActivityRepository;
    private final HonorRepository honorRepository;
    private final DevelopmentRepository developmentRepository;

    public ScoreCalculationService(ScoreRepository scoreRepository,
                                   CourseRepository courseRepository,
                                   StudentService studentService,
                                   AttendanceRepository attendanceRepository,
                                   DailyActivityRepository dailyActivityRepository,
                                   HonorRepository honorRepository,
                                   DevelopmentRepository developmentRepository) {
        this.scoreRepository = scoreRepository;
        this.courseRepository = courseRepository;
        this.studentService = studentService;
        this.attendanceRepository = attendanceRepository;
        this.dailyActivityRepository = dailyActivityRepository;
        this.honorRepository = honorRepository;
        this.developmentRepository = developmentRepository;
    }
    
    /**
     * 计算单个学生的综合绩分（4维加权计算）
     * 成绩85%+实践5%+创新5%+荣誉5%
     */
    public Map<String, Object> calculateStudentScore(Integer studentId, Map<String, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            Map<String, Double> defaultWeights = new LinkedHashMap<>();
            defaultWeights.put("成绩", 0.85);
            defaultWeights.put("实践", 0.05);
            defaultWeights.put("创新", 0.05);
            defaultWeights.put("荣誉", 0.05);
            weights = defaultWeights;
        }

        // 1. 计算课程成绩（按学分加权）
        double courseScore = calculateCourseScore(studentId);

        // 2. 计算实践得分（社会实践板块）
        double attendanceScore = calculatePracticeScore(studentId);

        // 3. 计算创新得分（创新创业三类）
        double practiceScore = calculateInnovationScore(studentId);

        // 4. 计算荣誉得分
        double honorScore = calculateHonorScore(studentId);

        // 按权重计算综合绩分
        double totalScore = courseScore * weights.getOrDefault("成绩", 0.8)
                + attendanceScore * weights.getOrDefault("实践", 0.05)
                + practiceScore * weights.getOrDefault("创新", 0.05)
                + honorScore * weights.getOrDefault("荣誉", 0.1);

        // 转为GPA（五分制）
        double gpa = convertToGPA(totalScore);

        Map<String, Object> categoryScores = new LinkedHashMap<>();
        categoryScores.put("成绩", courseScore);
        categoryScores.put("实践", attendanceScore);
        categoryScores.put("创新", practiceScore);
        categoryScores.put("荣誉", honorScore);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("categoryScores", categoryScores);
        result.put("totalScore", Math.round(totalScore * 100.0) / 100.0);
        result.put("gpa", Math.round(gpa * 100.0) / 100.0);
        result.put("calculatedTime", java.time.LocalDateTime.now());
        
        return result;
    }
    
    /**
     * 计算课程成绩（按学分加权平均）
     * 公式：Σ(课程成绩×学分)/Σ学分
     */
    private double calculateCourseScore(Integer studentId) {
        List<Score> scores = scoreRepository.findByStudentPersonId(studentId);
        
        if (scores.isEmpty()) {
            return 0.0;
        }
        
        double totalScore = 0.0;
        double totalCredit = 0.0;
        
        for (Score score : scores) {
            if (score.getMark() != null && score.getCourse() != null && score.getCourse().getCredit() != null) {
                BigDecimal mark = score.getMark();
                double credit = score.getCourse().getCredit();
                totalScore += mark.doubleValue() * credit;
                totalCredit += credit;
            }
        }
        
        if (totalCredit == 0) {
            return 0.0;
        }
        
        return totalScore / totalCredit;
    }
    
    /**
     * 计算实践得分（社会实践四类：日常活动 daily_activity、培训讲座 training、校外实习 internship、志愿服务 volunteer）
     * 每个已通过的记录得20分，最高100分
     */
    private double calculatePracticeScore(Integer studentId) {
        List<DailyActivity> activities = dailyActivityRepository.findByStudentId(studentId);
        
        // 调试日志：输出查询到的所有活动
        log.info("[calculatePracticeScore] studentId={}, 查询到 {} 条活动记录", studentId, activities.size());
        for (DailyActivity a : activities) {
            log.info("[calculatePracticeScore] activityId={}, activityType={}, status={}, activityName={}", 
                    a.getId(), a.getActivityType(), a.getStatus(), a.getActivityName());
        }

        long approvedCount = activities.stream()
                .filter(a -> "approved".equals(a.getStatus()))
                .filter(a -> {
                    String type = a.getActivityType();
                    return "daily_activity".equals(type)
                            || "training".equals(type)
                            || "internship".equals(type)
                            || "volunteer".equals(type);
                })
                .count();
        
        log.info("[calculatePracticeScore] studentId={}, approvedCount={}, score={}", studentId, approvedCount, Math.min(approvedCount * 20.0, 100.0));

        // 每条记录20分，最高100分
        return Math.min(approvedCount * 20.0, 100.0);
    }
    
    /**
     * 计算创新得分（创新创业三类：创业实践 innovation、学科竞赛 competition、科研成果 achievement）
     * 每个已通过的记录得20分，最高100分
     */
    private double calculateInnovationScore(Integer studentId) {
        List<StudentDevelopment> developments = developmentRepository.findByStudentId(studentId);

        long approvedCount = developments.stream()
                .filter(d -> "approved".equals(d.getStatus()))
                .filter(d -> {
                    String type = d.getDevelopmentType();
                    return "innovation".equals(type)
                            || "competition".equals(type)
                            || "achievement".equals(type);
                })
                .count();

        // 每条记录20分，最高100分
        return Math.min(approvedCount * 20.0, 100.0);
    }
    
    /**
     * 计算荣誉得分
     * 根据荣誉等级加分：国家级50分、省级30分、校级20分、院级10分，最高100分
     */
    private double calculateHonorScore(Integer studentId) {
        List<Honor> honors = honorRepository.findByStudentId(studentId);
        
        double honorScore = 0.0;
        
        for (Honor honor : honors) {
            if ("approved".equals(honor.getStatus())) {
                String level = honor.getHonorLevel();
                if (level != null) {
                    switch (level) {
                        case "国家级":
                            honorScore += 50;
                            break;
                        case "省级":
                            honorScore += 30;
                            break;
                        case "校级":
                            honorScore += 20;
                            break;
                        case "院级":
                            honorScore += 10;
                            break;
                        // 其他级别（如村级等）不给分
                    }
                }
            }
        }
        
        // 最高100分
        return Math.min(honorScore, 100.0);
    }
    
    /**
     * 获取所有学生绩分排名
     */
    public DataResponse getScoreRanking() {
        DataRequest req = new DataRequest();
        req.add("numName", "");
        DataResponse studentListResponse = studentService.getStudentList(req);
        List<Map<String, Object>> studentList = (List<Map<String, Object>>) studentListResponse.getData();
        
        if (studentList == null || studentList.isEmpty()) {
            return CommonMethod.getReturnData(new ArrayList<>());
        }
        
        List<Map<String, Object>> rankingList = new ArrayList<>();
        for (Map<String, Object> student : studentList) {
            Integer studentId = CommonMethod.getInteger(student, "personId");
            String studentName = CommonMethod.getString(student, "name");
            String studentNum = CommonMethod.getString(student, "num");
            
            if (studentId != null) {
                Map<String, Object> scoreResult = calculateStudentScore(studentId, null);
                double totalScore = (Double) scoreResult.get("totalScore");
                
                Map<String, Object> rankingItem = new HashMap<>();
                rankingItem.put("studentId", studentId);
                rankingItem.put("studentName", studentName);
                rankingItem.put("studentNum", studentNum);
                rankingItem.put("className", CommonMethod.getString(student, "className"));
                rankingItem.put("totalScore", totalScore);
                rankingItem.put("categoryScores", scoreResult.get("categoryScores"));
                
                rankingList.add(rankingItem);
            }
        }
        
        rankingList.sort((a, b) -> Double.compare((Double) b.get("totalScore"), (Double) a.get("totalScore")));
        
        for (int i = 0; i < rankingList.size(); i++) {
            rankingList.get(i).put("rank", i + 1);
        }
        
        return CommonMethod.getReturnData(rankingList);
    }
    
    /**
     * 获取默认权重配置（4维加权）
     */
    public DataResponse getDefaultWeights() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("成绩", 0.85);
        weights.put("实践", 0.05);
        weights.put("创新", 0.05);
        weights.put("荣誉", 0.05);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("weights", weights);
        result.put("description", "默认权重配置：成绩85%，实践5%，创新5%，荣誉5%");
        
        return CommonMethod.getReturnData(result);
    }
    
    /**
     * 保存自定义权重配置
     */
    public DataResponse saveWeightConfig(DataRequest dataRequest) {
        Map<String, Object> form = dataRequest.getData();
        if (form == null || form.isEmpty()) {
            form = dataRequest.getMap("form");
        }
        
        Map<String, Double> weights = new LinkedHashMap<>();
        double totalWeight = 0.0;
        
        for (Map.Entry<String, Object> entry : form.entrySet()) {
            if (entry.getValue() instanceof Number) {
                weights.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                totalWeight += ((Number) entry.getValue()).doubleValue();
            }
        }
        
        if (Math.abs(totalWeight - 1.0) > 0.001) {
            return CommonMethod.getReturnMessageError("权重总和必须等于1.0，当前总和：" + totalWeight);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("weights", weights);
        result.put("message", "权重配置保存成功");
        
        log.info("保存权重配置成功: {}", weights);
        return CommonMethod.getReturnData(result);
    }
    
    /**
     * 批量计算所有学生绩分
     */
    public DataResponse batchCalculateAllScores() {
        DataResponse studentListResponse = studentService.getStudentList(new DataRequest());
        List<Map<String, Object>> studentList = (List<Map<String, Object>>) studentListResponse.getData();
        
        if (studentList == null || studentList.isEmpty()) {
            return CommonMethod.getReturnMessageError("没有学生数据");
        }
        
        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (Map<String, Object> student : studentList) {
            Integer studentId = CommonMethod.getInteger(student, "personId");
            String studentName = CommonMethod.getString(student, "name");
            
            if (studentId != null) {
                try {
                    Map<String, Object> scoreResult = calculateStudentScore(studentId, null);
                    Map<String, Object> item = new HashMap<>();
                    item.put("studentId", studentId);
                    item.put("studentName", studentName);
                    item.put("totalScore", scoreResult.get("totalScore"));
                    item.put("status", "success");
                    results.add(item);
                    successCount++;
                } catch (Exception e) {
                    log.error("计算学生{}绩分失败", studentName, e);
                    Map<String, Object> item = new HashMap<>();
                    item.put("studentId", studentId);
                    item.put("studentName", studentName);
                    item.put("status", "failed");
                    item.put("error", e.getMessage());
                    results.add(item);
                    failCount++;
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalStudents", studentList.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("details", results);
        
        return CommonMethod.getReturnData(result);
    }
    
    /**
     * 百分制成绩转绩点（五分制，0.1分间隔）
     */
    private double convertToGPA(double score) {
        if (score >= 100) return 5.0;
        if (score >= 99) return 4.9;
        if (score >= 98) return 4.8;
        if (score >= 97) return 4.7;
        if (score >= 96) return 4.6;
        if (score >= 95) return 4.5;
        if (score >= 94) return 4.4;
        if (score >= 93) return 4.3;
        if (score >= 92) return 4.2;
        if (score >= 91) return 4.1;
        if (score >= 90) return 4.0;
        if (score >= 89) return 3.9;
        if (score >= 88) return 3.8;
        if (score >= 87) return 3.7;
        if (score >= 86) return 3.6;
        if (score >= 85) return 3.5;
        if (score >= 84) return 3.4;
        if (score >= 83) return 3.3;
        if (score >= 82) return 3.2;
        if (score >= 81) return 3.1;
        if (score >= 80) return 3.0;
        if (score >= 79) return 2.9;
        if (score >= 78) return 2.8;
        if (score >= 77) return 2.7;
        if (score >= 76) return 2.6;
        if (score >= 75) return 2.5;
        if (score >= 74) return 2.4;
        if (score >= 73) return 2.3;
        if (score >= 72) return 2.2;
        if (score >= 71) return 2.1;
        if (score >= 70) return 2.0;
        if (score >= 69) return 1.9;
        if (score >= 68) return 1.8;
        if (score >= 67) return 1.7;
        if (score >= 66) return 1.6;
        if (score >= 65) return 1.5;
        if (score >= 64) return 1.4;
        if (score >= 63) return 1.3;
        if (score >= 62) return 1.2;
        if (score >= 61) return 1.1;
        if (score >= 60) return 1.0;
        return 0.0;
    }
}
