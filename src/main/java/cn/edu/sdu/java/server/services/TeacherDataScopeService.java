package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.Teacher;
import cn.edu.sdu.java.server.repositorys.CourseSelectionRepository;
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

    public TeacherDataScopeService(TeacherRepository teacherRepository,
                                   ExamScheduleRepository examScheduleRepository,
                                   CourseSelectionRepository courseSelectionRepository) {
        this.teacherRepository = teacherRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.courseSelectionRepository = courseSelectionRepository;
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
        String teacherName = tOp.get().getPerson().getName();
        if (teacherName == null || teacherName.isEmpty()) {
            return Collections.emptySet();
        }

        List<Integer> courseIds = examScheduleRepository.findDistinctCourseIdsByTeacherName(teacherName);
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Integer> studentIds = courseSelectionRepository.findDistinctStudentIdsByCourseIds(courseIds);
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
}
