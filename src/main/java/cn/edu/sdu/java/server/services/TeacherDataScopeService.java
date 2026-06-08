package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.CourseTeaching;
import cn.edu.sdu.java.server.models.Teacher;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.CourseSelectionRepository;
import cn.edu.sdu.java.server.repositorys.CourseTeachingRepository;
import cn.edu.sdu.java.server.repositorys.ExamScheduleRepository;
import cn.edu.sdu.java.server.repositorys.TeacherRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TeacherDataScopeService {
    private final TeacherRepository teacherRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final CourseSelectionRepository courseSelectionRepository;
    private final CourseRepository courseRepository;
    private final CourseTeachingRepository courseTeachingRepository;

    public TeacherDataScopeService(TeacherRepository teacherRepository,
                                   ExamScheduleRepository examScheduleRepository,
                                   CourseSelectionRepository courseSelectionRepository,
                                   CourseRepository courseRepository,
                                   CourseTeachingRepository courseTeachingRepository) {
        this.teacherRepository = teacherRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.courseSelectionRepository = courseSelectionRepository;
        this.courseRepository = courseRepository;
        this.courseTeachingRepository = courseTeachingRepository;
    }

    public boolean isCurrentRoleTeacher() {
        return "ROLE_TEACHER".equals(CommonMethod.getRoleName());
    }

    public Set<Integer> getCurrentTeacherStudentIds() {
        if (!isCurrentRoleTeacher()) {
            return Collections.emptySet();
        }
        String username = CommonMethod.getUsername();
        if (username == null || username.isEmpty()) {
            return Collections.emptySet();
        }
        Optional<Teacher> tOp = teacherRepository.findByPersonNum(username);
        if (tOp.isEmpty() || tOp.get().getPerson() == null) {
            return Collections.emptySet();
        }
        Integer personId = tOp.get().getPerson().getPersonId();
        
        // 通过课程的任课教师字段获取该教师授课的所有课程
        List<Course> courses = courseRepository.findByTeacherPersonPersonId(personId);
        if (courses == null || courses.isEmpty()) {
            return Collections.emptySet();
        }
        
        Set<Integer> courseIds = new HashSet<>();
        for (Course course : courses) {
            if (course != null && course.getCourseId() != null) {
                courseIds.add(course.getCourseId());
            }
        }
        
        // 获取这些课程的学生ID列表
        Set<Integer> studentIds = courseSelectionRepository.findDistinctStudentIdsByCourseIds(new java.util.ArrayList<>(courseIds));
        return studentIds == null ? Collections.emptySet() : new HashSet<>(studentIds);
    }

    public boolean canCurrentTeacherAccessStudent(Integer studentId) {
        if (!isCurrentRoleTeacher()) {
            return true;
        }
        if (studentId == null) {
            return false;
        }
        return getCurrentTeacherStudentIds().contains(studentId);
    }

    public Integer restrictStudentIdForTeacher(Integer studentId) {
        if (studentId == null || !isCurrentRoleTeacher()) {
            return studentId;
        }
        assertCurrentTeacherAccessStudent(studentId);
        return studentId;
    }

    public void assertCurrentTeacherAccessStudent(Integer studentId) {
        assertCurrentTeacherAccessStudent(studentId, "仅可查看或操作本人授课学生的数据");
    }

    public void assertCurrentTeacherAccessStudent(Integer studentId, String message) {
        if (isCurrentRoleTeacher() && !canCurrentTeacherAccessStudent(studentId)) {
            throw new BusinessException(ErrorCodes.ACCESS_DENIED, message);
        }
    }

    /**
     * 获取当前教师授课的课程ID列表
     * 同时从 course_teaching 表和 course 表的 teacher_id 字段查询
     */
    public Set<Integer> getCurrentTeacherCourseIds() {
        if (!isCurrentRoleTeacher()) {
            return Collections.emptySet();
        }
        String username = CommonMethod.getUsername();
        if (username == null || username.isEmpty()) {
            return Collections.emptySet();
        }
        Optional<Teacher> tOp = teacherRepository.findByPersonNum(username);
        if (tOp.isEmpty() || tOp.get().getPerson() == null) {
            return Collections.emptySet();
        }
        Integer personId = tOp.get().getPerson().getPersonId();
        
        Set<Integer> courseIds = new HashSet<>();
        
        // 1. 从 course_teaching 表查询
        List<CourseTeaching> teachingList = courseTeachingRepository.findByTeacherId(personId);
        System.out.println("[DEBUG] course_teaching表查询到记录数: " + (teachingList == null ? 0 : teachingList.size()));
        if (teachingList != null) {
            for (CourseTeaching ct : teachingList) {
                if (ct != null && ct.getCourse() != null && ct.getCourse().getCourseId() != null) {
                    courseIds.add(ct.getCourse().getCourseId());
                }
            }
        }
        
        // 2. 从 course 表的 teacher_id 字段查询
        List<Course> courseList = courseRepository.findByTeacherPersonPersonId(personId);
        System.out.println("[DEBUG] course表查询到记录数: " + (courseList == null ? 0 : courseList.size()));
        if (courseList != null) {
            for (Course course : courseList) {
                if (course != null && course.getCourseId() != null) {
                    courseIds.add(course.getCourseId());
                }
            }
        }
        
        System.out.println("[DEBUG] 教师查询课程 - personId: " + personId + ", 教师姓名: " + tOp.get().getPerson().getName());
        System.out.println("[DEBUG] 最终返回的课程IDs: " + courseIds);
        return courseIds;
    }

    /**
     * 验证当前教师是否可以访问指定课程
     */
    public boolean canCurrentTeacherAccessCourse(Integer courseId) {
        if (!isCurrentRoleTeacher()) {
            return true; // 非教师角色不做限制
        }
        if (courseId == null) {
            return false;
        }
        return getCurrentTeacherCourseIds().contains(courseId);
    }

    /**
     * 断言当前教师可以访问指定课程，否则抛出异常
     */
    public void assertCurrentTeacherAccessCourse(Integer courseId) {
        assertCurrentTeacherAccessCourse(courseId, "仅可操作本人授课课程的作业");
    }

    public void assertCurrentTeacherAccessCourse(Integer courseId, String message) {
        if (isCurrentRoleTeacher() && !canCurrentTeacherAccessCourse(courseId)) {
            throw new BusinessException(ErrorCodes.ACCESS_DENIED, message);
        }
    }
}
