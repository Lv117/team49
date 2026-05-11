package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.*;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.*;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ResumeService 个人简历生成服务
 */
@Service
public class ResumeService {
    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);
    
    private final StudentRepository studentRepository;
    private final ScoreRepository scoreRepository;
    private final HonorRepository honorRepository;
    private final DevelopmentRepository developmentRepository;
    
    public ResumeService(StudentRepository studentRepository,
                        ScoreRepository scoreRepository,
                        HonorRepository honorRepository,
                        DevelopmentRepository developmentRepository) {
        this.studentRepository = studentRepository;
        this.scoreRepository = scoreRepository;
        this.honorRepository = honorRepository;
        this.developmentRepository = developmentRepository;
    }
    
    /**
     * 预览简历数据（JSON格式）
     */
    public DataResponse previewResumeData(DataRequest dataRequest) {
        Integer studentId = dataRequest.getInteger("studentId");
        
        if (studentId == null || studentId <= 0) {
            studentId = CommonMethod.getPersonId();
        }
        
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return CommonMethod.getReturnMessageError("学生不存在");
        }
        
        Student student = studentOpt.get();
        Person person = student.getPerson();
        
        Map<String, Object> resumeData = new LinkedHashMap<>();
        
        // 基本信息
        Map<String, Object> basicInfo = new LinkedHashMap<>();
        basicInfo.put("name", person.getName());
        basicInfo.put("num", person.getNum());
        basicInfo.put("gender", person.getGender());
        basicInfo.put("birthday", person.getBirthday());
        basicInfo.put("phone", person.getPhone());
        basicInfo.put("email", person.getEmail());
        basicInfo.put("major", student.getMajor());
        resumeData.put("basicInfo", basicInfo);
        
        // 成绩统计
        List<Score> scores = scoreRepository.findByStudentPersonId(studentId);
        Map<String, Object> scoreStats = new LinkedHashMap<>();
        if (!scores.isEmpty()) {
            double avgScore = scores.stream()
                .mapToDouble(s -> s.getMark() != null ? s.getMark().doubleValue() : 0.0)
                .average()
                .orElse(0.0);
            double totalCredit = scores.stream()
                .filter(s -> s.getCourse() != null)
                .mapToDouble(s -> s.getCourse().getCredit() != null ? s.getCourse().getCredit() : 0)
                .sum();
            scoreStats.put("courseCount", scores.size());
            scoreStats.put("averageScore", String.format("%.2f", avgScore));
            scoreStats.put("totalCredit", String.format("%.1f", totalCredit));
        }
        resumeData.put("scoreStats", scoreStats);
        
        // 荣誉奖励
        List<Honor> honors = honorRepository.findByStudentId(studentId);
        List<Map<String, Object>> honorList = new ArrayList<>();
        for (Honor honor : honors) {
            if ("approved".equals(honor.getStatus())) {
                Map<String, Object> honorItem = new LinkedHashMap<>();
                honorItem.put("name", honor.getHonorName());
                honorItem.put("level", honor.getHonorLevel());
                honorItem.put("date", honor.getAwardDate());
                honorList.add(honorItem);
            }
        }
        resumeData.put("honors", honorList);
        
        // 创新创业
        List<StudentDevelopment> developments = developmentRepository.findByStudentId(studentId);
        List<Map<String, Object>> devList = new ArrayList<>();
        for (StudentDevelopment dev : developments) {
            if ("approved".equals(dev.getStatus())) {
                Map<String, Object> devItem = new LinkedHashMap<>();
                devItem.put("title", dev.getTitle());
                devItem.put("type", dev.getDevelopmentType());
                devList.add(devItem);
            }
        }
        resumeData.put("innovations", devList);
        
        // 自我评价
        resumeData.put("selfIntroduction", person.getIntroduce());
        
        return CommonMethod.getReturnData(resumeData);
    }
    
    /**
     * 生成个人简历PDF（简化版，返回JSON数据供前端生成PDF）
     */
    public ResponseEntity<Resource> generateResumePDF(DataRequest dataRequest) {
        // 目前返回预览数据，前端可根据此数据生成PDF
        DataResponse response = previewResumeData(dataRequest);
        
        if (response.getCode() == 1) {
            return ResponseEntity.notFound().build();
        }
        
        // 将数据转为JSON字节流返回
        String json = response.getData().toString();
        byte[] bytes = json.getBytes();
        Resource resource = new ByteArrayResource(bytes);
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resume_data.json\"")
            .contentLength(bytes.length)
            .body(resource);
    }
}
