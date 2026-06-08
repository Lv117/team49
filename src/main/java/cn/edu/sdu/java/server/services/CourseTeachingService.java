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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * CourseTeachingService 任课安排服务层
 * 实现任课安排的增删改查功能
 */
@Service
@Slf4j
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
     * 或者获取所有课程（用于管理员端）
     * 同时从 course_teaching 表和 course 表的 teacher_id 字段查询
     */
    public DataResponse getTeacherCourseList(DataRequest dataRequest) {
        Integer teacherId = dataRequest.getInteger("teacherId");
        String keyword = dataRequest.getString("keyword");
        
        log.info("[getTeacherCourseList] 接收到请求，teacherId={}, keyword={}", teacherId, keyword);
        
        // 如果没有 teacherId，说明是管理员，返回所有课程
        if (teacherId == null) {
            log.info("[getTeacherCourseList] 管理员请求，返回所有课程");
            List<Course> allCourses = courseRepository.findAll();
            
            // 如果有搜索关键词，在内存中过滤
            List<Course> filteredCourses = allCourses;
            if (keyword != null && !keyword.isEmpty()) {
                final String kw = keyword.toLowerCase();
                filteredCourses = allCourses.stream()
                    .filter(c -> (c.getNum() != null && c.getNum().toLowerCase().contains(kw)) ||
                                  (c.getName() != null && c.getName().toLowerCase().contains(kw)))
                    .toList();
                log.info("[getTeacherCourseList] 关键词过滤后剩余记录数量: {}", filteredCourses.size());
            }
            
            List<Map<String, Object>> dataList = new ArrayList<>();
            for (Course course : filteredCourses) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", course.getCourseId());
                m.put("courseId", course.getCourseId());
                m.put("courseNum", course.getNum());
                m.put("courseName", course.getName());
                m.put("credit", course.getCredit());
                m.put("classroom", course.getClassroom());
                m.put("schedule", course.getSchedule());
                m.put("semester", "");
                m.put("status", "active");
                dataList.add(m);
                log.info("[getTeacherCourseList]   课程ID: {}, 课程名: {}", course.getCourseId(), course.getName());
            }
            
            log.info("[getTeacherCourseList] 返回数据条数: {}", dataList.size());
            return CommonMethod.getReturnData(dataList);
        }

        // 以下是教师查询自己授课课程的逻辑
        // 方案：合并 course_teaching 表和 course 表的 teacher_id 数据
        Set<Integer> courseIds = new LinkedHashSet<>();
        Map<Integer, CourseTeaching> teachingMap = new LinkedHashMap<>();
        
        // 1. 从 course_teaching 表查询
        List<CourseTeaching> teachingList = courseTeachingRepository.findByTeacherId(teacherId);
        log.info("[getTeacherCourseList] course_teaching表查询到记录数: {}", teachingList == null ? 0 : teachingList.size());
        if (teachingList != null) {
            for (CourseTeaching ct : teachingList) {
                Integer courseId = ct.getCourse().getCourseId();
                if (!courseIds.contains(courseId)) {
                    courseIds.add(courseId);
                    teachingMap.put(courseId, ct);
                }
            }
        }
        
        // 2. 从 course 表的 teacher_id 字段查询（补充 course_teaching 没有的记录）
        List<Course> courseList = courseRepository.findByTeacherPersonPersonId(teacherId);
        log.info("[getTeacherCourseList] course表查询到记录数: {}", courseList == null ? 0 : courseList.size());
        if (courseList != null) {
            for (Course course : courseList) {
                Integer courseId = course.getCourseId();
                if (!courseIds.contains(courseId)) {
                    courseIds.add(courseId);
                    // 为 course 表的记录创建一个虚拟的 CourseTeaching 对象
                    CourseTeaching virtualCt = new CourseTeaching();
                    virtualCt.setCourse(course);
                    virtualCt.setTeacher(course.getTeacher());
                    virtualCt.setClassroom(course.getClassroom());
                    virtualCt.setSemester("current");
                    virtualCt.setStatus("active");
                    teachingMap.put(courseId, virtualCt);
                }
            }
        }
        
        log.info("[getTeacherCourseList] 去重后课程总数: {}", courseIds.size());
        
        // 如果有搜索关键词，在内存中过滤
        List<CourseTeaching> filteredList = new ArrayList<>(teachingMap.values());
        if (keyword != null && !keyword.isEmpty()) {
            final String kw = keyword.toLowerCase();
            filteredList = filteredList.stream()
                .filter(ct -> (ct.getCourse().getNum() != null && ct.getCourse().getNum().toLowerCase().contains(kw)) ||
                              (ct.getCourse().getName() != null && ct.getCourse().getName().toLowerCase().contains(kw)))
                .toList();
            log.info("[getTeacherCourseList] 关键词过滤后剩余记录数量: {}", filteredList.size());
        }

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (CourseTeaching ct : filteredList) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ct.getCourse().getCourseId());
            m.put("courseId", ct.getCourse().getCourseId());
            m.put("courseNum", ct.getCourse().getNum());
            m.put("courseName", ct.getCourse().getName());
            m.put("credit", ct.getCourse().getCredit());
            m.put("classroom", ct.getClassroom());
            m.put("schedule", ct.getCourse().getSchedule());
            m.put("semester", ct.getSemester());
            m.put("status", ct.getStatus());
            dataList.add(m);
            log.info("[getTeacherCourseList]   课程ID: {}, 课程名: {}", ct.getCourse().getCourseId(), ct.getCourse().getName());
        }

        log.info("[getTeacherCourseList] 返回数据条数: {}", dataList.size());
        return CommonMethod.getReturnData(dataList);
    }
}
