package cn.edu.sdu.java.server.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.edu.sdu.java.server.models.ExamSchedule;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.OptionItem;
import cn.edu.sdu.java.server.payload.response.OptionItemList;
import cn.edu.sdu.java.server.services.ExamScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/exam")
@Tag(name = "Exam", description = "Exam schedule and exam student APIs")
public class ExamController {

    @Autowired
    private ExamScheduleService examScheduleService;

    @Operation(summary = "Get year semester option list")
    @PostMapping("/getYearSemesterOptionList")
    public OptionItemList getYearSemesterOptionList(@RequestBody DataRequest req) {
        try {
            List<OptionItem> yearSemesterList = examScheduleService.getYearSemesterOptionList();
            return new OptionItemList(0, yearSemesterList);
        } catch (Exception e) {
            e.printStackTrace();
            return new OptionItemList(1, null);
        }
    }

    @Operation(summary = "Get course option list")
    @PostMapping("/getCourseOptionList")
    public OptionItemList getCourseOptionList(@RequestBody DataRequest req) {
        try {
            List<OptionItem> courseList = examScheduleService.getCourseOptionList();
            return new OptionItemList(0, courseList);
        } catch (Exception e) {
            e.printStackTrace();
            return new OptionItemList(1, null);
        }
    }

    @Operation(summary = "Get teacher option list")
    @PostMapping("/getTeacherOptionList")
    public OptionItemList getTeacherOptionList(@RequestBody DataRequest req) {
        try {
            List<OptionItem> teacherList = examScheduleService.getTeacherOptionList();
            return new OptionItemList(0, teacherList);
        } catch (Exception e) {
            e.printStackTrace();
            return new OptionItemList(1, null);
        }
    }

    @Operation(summary = "Get exam schedule list")
    @PostMapping("/getExamScheduleList")
    public DataResponse getExamScheduleList(@RequestBody DataRequest req) {
        try {
            Integer yearSemesterId = req.getInteger("yearSemesterId");
            String courseName = req.getString("courseName");
            String keyword = req.getString("keyword");

            List<Map<String, Object>> examScheduleList = examScheduleService.getExamScheduleList(yearSemesterId, courseName, keyword);
            return new DataResponse(0, examScheduleList, "success");
        } catch (Exception e) {
            e.printStackTrace();
            return new DataResponse(1, null, "获取考试安排列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "Save exam schedule")
    @PostMapping("/saveExamSchedule")
    public DataResponse saveExamSchedule(@RequestBody DataRequest req) {
        try {
            String examId = req.getString("examId");
            String originExamId = req.getString("originExamId");
            String courseName = req.getString("courseName");
            String teacher = req.getString("teacher");
            String examTimeStr = req.getString("examTime");
            String examRoom = req.getString("examRoom");
            Integer yearSemesterId = req.getInteger("yearSemesterId");
            Integer courseId = req.getInteger("courseId");

            ExamSchedule examSchedule;
            if (examId != null) {
                examSchedule = new ExamSchedule();
                examSchedule.setExamId(examId);
            } else {
                examSchedule = new ExamSchedule();
            }

            examSchedule.setCourseName(courseName);
            examSchedule.setTeacher(teacher);
            examSchedule.setExamTime(LocalDate.parse(examTimeStr));
            examSchedule.setExamRoom(examRoom);
            examSchedule.setYearSemesterId(yearSemesterId);
            examSchedule.setCourseId(courseId);

            ExamSchedule savedExamSchedule = examScheduleService.saveExamSchedule(originExamId, examSchedule);
            return new DataResponse(0, savedExamSchedule, "success");
        } catch (Exception e) {
            e.printStackTrace();
            return new DataResponse(1, null, "保存失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除考试安排")
    @PostMapping("/deleteExamSchedule")
    public DataResponse deleteExamSchedule(@RequestBody DataRequest req) {
        try {
            String examId = req.getString("examId");
            examScheduleService.deleteExamSchedule(examId);
            return new DataResponse(0, null, "success");
        } catch (Exception e) {
            e.printStackTrace();
            return new DataResponse(1, null, "删除考试安排失败: " + e.getMessage());
        }
    }

    @Operation(summary = "Get student exam schedule list")
    @PostMapping("/getStudentExamScheduleList")
    public DataResponse getStudentExamScheduleList(@RequestBody DataRequest req) {
        try {
            String studentId = req.getString("studentId");
            Integer yearSemesterId = req.getInteger("yearSemesterId");

            List<Map<String, Object>> examScheduleList = examScheduleService.getStudentExamScheduleList(studentId, yearSemesterId);
            return new DataResponse(0, examScheduleList, "success");
        } catch (Exception e) {
            e.printStackTrace();
            return new DataResponse(1, null, "获取学生考试安排列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "Get exam student list")
    @PostMapping("/getExamStudentList")
    public DataResponse getExamStudentList(@RequestBody DataRequest req) {
        try {
            String examId = req.getString("examId");
            List<Map<String, Object>> studentList = examScheduleService.getExamStudentList(examId);
            return new DataResponse(0, studentList, "success");
        } catch (Exception e) {
            e.printStackTrace();
            return new DataResponse(1, null, "获取课程选课学生列表失败: " + e.getMessage());
        }
    }
}
