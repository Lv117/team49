package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.Score;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.ScoreRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
    
    public ScoreCalculationService(ScoreRepository scoreRepository, 
                                   CourseRepository courseRepository,
                                   StudentService studentService) {
        this.scoreRepository = scoreRepository;
        this.courseRepository = courseRepository;
        this.studentService = studentService;
    }
    
    /**
     * 计算单个学生的综合绩分
     */
    public Map<String, Object> calculateStudentScore(Integer studentId, Map<String, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            Map<String, Double> defaultWeights = new HashMap<>();
            defaultWeights.put("必修", 0.6);
            defaultWeights.put("选修", 0.3);
            defaultWeights.put("实践", 0.1);
            weights = defaultWeights;
        }
        
        List<Score> scores = scoreRepository.findByStudentPersonId(studentId);
        
        // 暂时不分类别，计算总体平均绩分
        if (!scores.isEmpty()) {
            double avgScore = scores.stream()
                .mapToDouble(s -> s.getMark() != null ? s.getMark().doubleValue() : 0.0)
                .average()
                .orElse(0.0);
            
            double gpa = convertToGPA(avgScore);
            
            Map<String, Object> result = new HashMap<>();
            result.put("studentId", studentId);
            result.put("categoryScores", new HashMap<>());
            result.put("totalScore", Math.round(gpa * 100.0) / 100.0);
            result.put("calculatedTime", java.time.LocalDateTime.now());
            
            return result;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("studentId", studentId);
        result.put("categoryScores", new HashMap<>());
        result.put("totalScore", 0.0);
        result.put("calculatedTime", java.time.LocalDateTime.now());
        
        return result;
    }
    
    /**
     * 获取所有学生绩分排名
     */
    public DataResponse getScoreRanking() {
        DataResponse studentListResponse = studentService.getStudentList(new DataRequest());
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
     * 获取默认权重配置
     */
    public DataResponse getDefaultWeights() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("必修", 0.6);
        weights.put("选修", 0.3);
        weights.put("实践", 0.1);
        
        Map<String, Object> result = new HashMap<>();
        result.put("weights", weights);
        result.put("description", "默认权重配置：必修课60%，选修课30%，实践课10%");
        
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
