package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.models.StudentStatistics;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.ScoreRepository;
import cn.edu.sdu.java.server.repositorys.StudentLeaveRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.repositorys.StudentStatisticsRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

@Service
public class StudentStatisticsService {
    private final StudentRepository studentRepository;
    private final ScoreRepository scoreRepository;
    private final StudentLeaveRepository studentLeaveRepository;
    private final StudentStatisticsRepository studentStatisticsRepository;
    private final TeacherDataScopeService teacherDataScopeService;
    private final ScoreCalculationService scoreCalculationService;
    
    public StudentStatisticsService(StudentRepository studentRepository, 
                                   ScoreRepository scoreRepository, 
                                   StudentLeaveRepository studentLeaveRepository, 
                                   StudentStatisticsRepository studentStatisticsRepository,
                                   TeacherDataScopeService teacherDataScopeService,
                                   ScoreCalculationService scoreCalculationService) {
        this.studentRepository = studentRepository;
        this.scoreRepository = scoreRepository;
        this.studentLeaveRepository = studentLeaveRepository;
        this.studentStatisticsRepository = studentStatisticsRepository;
        this.teacherDataScopeService = teacherDataScopeService;
        this.scoreCalculationService = scoreCalculationService;
    }
    public DataResponse getStudentStatisticsList(DataRequest dataRequest) {
        List<StudentStatistics> sList = studentStatisticsRepository.findAll();
        
        // 教师权限：只显示其作为指导老师负责的学生的数据
        if (teacherDataScopeService.isCurrentRoleTeacher()) {
            Set<Integer> allowedStudentIds = teacherDataScopeService.getCurrentTeacherStudentIds();
            sList = sList.stream()
                .filter(ss -> ss.getStudent() != null && allowedStudentIds.contains(ss.getStudent().getPersonId()))
                .collect(java.util.stream.Collectors.toList());
        }
        
        // 从绩分计算面板获取排名数据（包含正确的权重配置计算的totalScore和排名）
        DataResponse rankingResponse = scoreCalculationService.getScoreRanking();
        Map<Integer, Map<String, Object>> rankingMap = new HashMap<>();
        if (rankingResponse != null && rankingResponse.getCode() == 0 && rankingResponse.getData() instanceof List) {
            List<Map<String, Object>> rankingList = (List<Map<String, Object>>) rankingResponse.getData();
            for (Map<String, Object> ranking : rankingList) {
                Object studentIdObj = ranking.get("studentId");
                if (studentIdObj instanceof Number) {
                    rankingMap.put(((Number) studentIdObj).intValue(), ranking);
                }
            }
        }
        
        // 实时查询请假次数（不依赖数据库缓存）
        List<Integer> personIdList = new ArrayList<>();
        for (StudentStatistics ss : sList) {
            personIdList.add(ss.getStudent().getPersonId());
        }
        Map<Integer, Integer> leaveCountMap = new HashMap<>();
        if (!personIdList.isEmpty()) {
            List<?> leaveList = studentLeaveRepository.getStudentStatisticsList(personIdList);
            if (leaveList != null && !leaveList.isEmpty()) {
                for (Object item : leaveList) {
                    Object[] as = (Object[]) item;
                    Integer personId = (Integer) as[0];
                    Long count = (Long) as[1];
                    leaveCountMap.put(personId, count.intValue());
                }
            }
        }
        
        // 构建数据列表
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (StudentStatistics ss : sList) {
            Map<String, Object> m = new HashMap<>();
            Person p = ss.getStudent().getPerson();
            m.put("studentNum", p.getNum());
            m.put("studentName", p.getName());
            m.put("courseCount", ss.getCourseCount()+"");
            
            // 从绩分计算面板的排名数据中获取综合绩分、成绩和排名
            Map<String, Object> ranking = rankingMap.get(ss.getStudent().getPersonId());
            Integer rank = null;
            if (ranking != null) {
                // 综合绩分：保留两位小数
                Object totalScoreObj = ranking.get("totalScore");
                double totalScore = totalScoreObj instanceof Number ? ((Number) totalScoreObj).doubleValue() : 0.0;
                m.put("totalScore", String.format("%.2f", totalScore));
                
                // 成绩：从categoryScores中获取"成绩"维度得分，保留两位小数
                Object categoryScoresObj = ranking.get("categoryScores");
                double avgScore = ss.getAvgScore(); // 默认值
                if (categoryScoresObj instanceof Map) {
                    Map<?, ?> categoryScores = (Map<?, ?>) categoryScoresObj;
                    Object scoreObj = categoryScores.get("成绩");
                    if (scoreObj instanceof Number) {
                        avgScore = ((Number) scoreObj).doubleValue();
                    }
                }
                m.put("avgScore", String.format("%.2f", avgScore));
                
                // 排名：从绩分计算面板获取（基于综合绩分排序）
                Object rankObj = ranking.get("rank");
                if (rankObj instanceof Number) {
                    rank = ((Number) rankObj).intValue();
                    m.put("no", rankObj.toString());
                } else {
                    m.put("no", "");
                }
            } else {
                // 如果排名数据中没有，使用旧数据作为兜底
                double totalScore = ss.getGpa() != null ? ss.getGpa() : 0.0;
                m.put("totalScore", String.format("%.2f", totalScore));
                double avgScore = ss.getAvgScore() != null ? ss.getAvgScore() : 0.0;
                m.put("avgScore", String.format("%.2f", avgScore));
                m.put("no", ss.getNo() != null ? ss.getNo().toString() : "");
                rank = ss.getNo();
            }
            
            // 请假次数：使用实时查询的数据
            Integer leaveCount = leaveCountMap.get(ss.getStudent().getPersonId());
            m.put("leaveCount", leaveCount != null ? leaveCount.toString() : "0");
            
            // 保存排名用于后续排序
            m.put("_rank", rank != null ? rank : 999999);
            
            dataList.add(m);
        }
        
        // 按排名排序（排名小的在前）
        dataList.sort((a, b) -> {
            Integer rankA = (Integer) a.get("_rank");
            Integer rankB = (Integer) b.get("_rank");
            return Integer.compare(rankA, rankB);
        });
        
        // 移除临时排序字段
        dataList.forEach(m -> m.remove("_rank"));
        
        return CommonMethod.getReturnData(dataList);
    };
    public DataResponse doStudentStatistics(DataRequest dataRequest) {
        String year = "2025";
        List<Student> sList = studentRepository.findAll();
        int i,j;
        Student s;
        Integer personId;
        Object[] as;
        Long l;
        int c;
        double creditSum;
        BigDecimal sum;
        BigDecimal creditMark;
        List<Integer> idList = new ArrayList<>();
        Map<Integer,StudentStatistics> sMap = new HashMap<>();
        StudentStatistics ss;
        for(i = 0; i < sList.size(); i++) {
            s= sList.get(i);
            ss = new StudentStatistics();
            ss.setStudent(s);
            ss.setYear(year);
            ss.setCourseCount(0);
            ss.setGpa(0d);
            ss.setAvgScore(0d);
            ss.setLeaveCount(0);
            personId = s.getPerson().getPersonId();
            sMap.put(personId, ss);
            idList.add(personId);
        }
        List<StudentStatistics> ssList = studentStatisticsRepository.findByYear(year, idList);
        if(ssList!= null && !ssList.isEmpty()) {
            for (i = 0; i < ssList.size();i++) {
                ss = ssList.get(i);
                personId = ss.getStudent().getPersonId();
                sMap.put(personId, ss);
            }
        }
        List<?> list = scoreRepository.getStudentStatisticsList(idList);
        if(list != null && !list.isEmpty()) {
            for(i = 0;i<list.size();i++) {
                as = (Object[]) list.get(i);
                personId = (Integer)as[0];
                ss = sMap.get(personId);
                if(ss == null)
                    continue;
                l = (Long)as[1];
                if(l != null)
                    c = l.intValue();
                else
                    c = 0;
                if(c == 0)
                    continue;
                sum = toBigDecimal(as[2]);
                ss.setCourseCount(c);
                ss.setAvgScore(CommonMethod.getDouble2(sum.doubleValue() / c));
                creditSum = toBigDecimal(as[3]).doubleValue();
                creditMark = toBigDecimal(as[4]);
                if (creditSum > 0) {
                    ss.setGpa(CommonMethod.getDouble2(creditMark.doubleValue() / creditSum));
                }
            }
        }
        list = studentLeaveRepository.getStudentStatisticsList(idList);
        if(list != null && !list.isEmpty()) {
            for(i = 0;i<list.size();i++) {
                as = (Object[]) list.get(i);
                personId = (Integer)as[0];
                ss = sMap.get(personId);
                if(ss == null)
                    continue;
                l = (Long)as[1];
                if(l != null)
                    c = l.intValue();
                else
                    c = 0;
                if(c == 0)
                    continue;
                ss.setLeaveCount(c);
            }
        }
        StudentStatistics[] ssArray = new StudentStatistics[sMap.size()];
        sMap.values().toArray(ssArray);
        Arrays.sort(ssArray);
        for(i= 0; i < ssArray.length; i++) {
            ss = ssArray[i];
            ss.setNo(i+1);
            studentStatisticsRepository.save(ss);
        }
        return CommonMethod.getReturnMessageOK();
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) {
            return BigDecimal.ZERO;
        }
        if (obj instanceof BigDecimal bd) {
            return bd;
        }
        if (obj instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
