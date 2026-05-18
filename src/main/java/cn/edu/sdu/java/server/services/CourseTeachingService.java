package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.CourseTeaching;
import cn.edu.sdu.java.server.models.Teacher;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.CourseTeachingRepository;
import cn.edu.sdu.java.server.repositorys.TeacherRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * CourseTeachingService 任课安排服务层
 * 实现任课安排的增删改查功能
 */
@Service
public class CourseTeachingService {
    private final CourseTeachingRepository courseTeachingRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;

    public CourseTeachingService(CourseTeachingRepository courseTeachingRepository,
                                 TeacherRepository teacherRepository,
                                 CourseRepository courseRepository) {
        this.courseTeachingRepository = courseTeachingRepository;
        this.teacherRepository = teacherRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * 获取任课安排列表
     */
    public DataResponse getCourseTeachingList(DataRequest dataRequest) {
        Integer teacherId = dataRequest.getInteger("teacherId");
        String semester = dataRequest.getString("semester");

        List<CourseTeaching> teachingList;
        if (teacherId != null) {
            teachingList = courseTeachingRepository.findByTeacherId(teacherId);
        } else if (semester != null && !semester.isEmpty()) {
            teachingList = courseTeachingRepository.findBySemester(semester);
        } else {
            teachingList = courseTeachingRepository.findAll();
        }

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (CourseTeaching ct : teachingList) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ct.getId());
            m.put("teacherId", ct.getTeacher().getPersonId());
            m.put("teacherName", ct.getTeacher().getPerson() != null ? ct.getTeacher().getPerson().getName() : "");
            m.put("courseId", ct.getCourse().getCourseId());
            m.put("courseNum", ct.getCourse().getNum());
            m.put("courseName", ct.getCourse().getName());
            m.put("classroom", ct.getClassroom());
            m.put("semester", ct.getSemester());
            m.put("status", ct.getStatus());
            m.put("remark", ct.getRemark());
            dataList.add(m);
        }

        return CommonMethod.getReturnData(dataList);
    }

    /**
     * 保存任课安排
     */
    public DataResponse courseTeachingSave(DataRequest dataRequest) {
        Integer id = dataRequest.getInteger("id");
        Integer teacherId = dataRequest.getInteger("teacherId");
        Integer courseId = dataRequest.getInteger("courseId");
        String classroom = dataRequest.getString("classroom");
        String semester = dataRequest.getString("semester");
        String status = dataRequest.getString("status");
        String remark = dataRequest.getString("remark");

        Optional<CourseTeaching> op;
        CourseTeaching ct = null;

        if (id != null) {
            op = courseTeachingRepository.findById(id);
            if (op.isPresent())
                ct = op.get();
        }

        if (ct == null)
            ct = new CourseTeaching();

        // 查询教师
        if (teacherId != null) {
            Optional<Teacher> teacherOp = teacherRepository.findById(teacherId);
            if (teacherOp.isPresent())
                ct.setTeacher(teacherOp.get());
        }

        // 查询课程
        if (courseId != null) {
            Optional<Course> courseOp = courseRepository.findById(courseId);
            if (courseOp.isPresent())
                ct.setCourse(courseOp.get());
        }

        ct.setClassroom(classroom);
        ct.setSemester(semester);
        if (status != null)
            ct.setStatus(status);
        ct.setRemark(remark);

        courseTeachingRepository.save(ct);
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 删除任课安排
     */
    public DataResponse courseTeachingDelete(DataRequest dataRequest) {
        Integer id = dataRequest.getInteger("id");
        if (id != null) {
            Optional<CourseTeaching> op = courseTeachingRepository.findById(id);
            if (op.isPresent()) {
                courseTeachingRepository.delete(op.get());
            }
        }
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 获取教师任课安排（用于教师端）
     * 直接从 course 表查询该教师教授的课程
     */
    public DataResponse getTeacherCourseList(DataRequest dataRequest) {
        Integer teacherId = dataRequest.getInteger("teacherId");
        String keyword = dataRequest.getString("keyword");
        if (teacherId == null) {
            return CommonMethod.getReturnMessageError("教师ID不能为空");
        }

        System.out.println("[TeacherCourse Debug] 教师ID=" + teacherId + ", keyword=" + keyword);

        List<Course> courseList;
        if (keyword != null && !keyword.isEmpty()) {
            courseList = courseRepository.findByTeacherPersonIdAndKeyword(teacherId, keyword);
        } else {
            courseList = courseRepository.findByTeacherPersonPersonId(teacherId);
        }

        System.out.println("[TeacherCourse Debug] 查询到课程数量=" + courseList.size());
        for (Course c : courseList) {
            System.out.println("[TeacherCourse Debug] 课程: " + c.getNum() + " - " + c.getName());
        }

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Course c : courseList) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getCourseId());
            m.put("courseId", c.getCourseId());
            m.put("courseNum", c.getNum());
            m.put("courseName", c.getName());
            m.put("credit", c.getCredit());
            m.put("classroom", c.getClassroom());
            m.put("schedule", c.getSchedule());
            m.put("semester", "");
            m.put("status", "active");
            dataList.add(m);
        }

        return CommonMethod.getReturnData(dataList);
    }
}
