package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.ExamSchedule;
import cn.edu.sdu.java.server.payload.response.OptionItem;
import java.util.List;
import java.util.Map;

public interface ExamScheduleService {
    List<OptionItem> getYearSemesterOptionList();
    List<OptionItem> getCourseOptionList();
    List<Map<String, Object>> getExamScheduleList(Integer yearSemesterId, String courseName, String keyword);
    List<Map<String, Object>> getStudentExamScheduleList(String studentId, Integer yearSemesterId);
    List<Map<String, Object>> getExamStudentList(String examId);
    ExamSchedule saveExamSchedule(String originExamId, ExamSchedule examSchedule);
    void deleteExamSchedule(String examId);
    void generateStudentExamSchedules(String examId);
}
