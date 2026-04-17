package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Attendance 考勤数据操作接口
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {
    
    /**
     * 根据学生 ID 查询考勤记录（使用 JOIN FETCH 避免 N+1 查询）
     */
    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s JOIN FETCH s.person JOIN FETCH a.course WHERE a.student.personId = ?1")
    List<Attendance> findByStudentPersonId(Integer studentId);
    
    /**
     * 根据课程 ID 查询考勤记录（使用 JOIN FETCH 避免 N+1 查询）
     */
    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s JOIN FETCH s.person JOIN FETCH a.course WHERE a.course.courseId = ?1")
    List<Attendance> findByCourseCourseId(Integer courseId);
    
    /**
     * 根据学生和课程查询考勤记录（使用 JOIN FETCH 避免 N+1 查询）
     */
    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s JOIN FETCH s.person JOIN FETCH a.course WHERE a.student.personId = ?1 AND a.course.courseId = ?2")
    List<Attendance> findByStudentPersonIdAndCourseCourseId(Integer studentId, Integer courseId);
    
    /**
     * 根据日期范围查询考勤记录
     */
    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s JOIN FETCH s.person JOIN FETCH a.course WHERE a.attendanceDate BETWEEN ?1 AND ?2")
    List<Attendance> findByAttendanceDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * 根据学生和日期范围查询考勤记录（使用 JOIN FETCH 避免 N+1 查询）
     */
    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s JOIN FETCH s.person JOIN FETCH a.course WHERE a.student.personId = ?1 AND a.attendanceDate BETWEEN ?2 AND ?3")
    List<Attendance> findByStudentPersonIdAndAttendanceDateBetween(Integer studentId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 统计学生考勤情况
     */
    @Query("SELECT a.status, COUNT(a) FROM Attendance a WHERE a.student.personId = ?1 GROUP BY a.status")
    List<Object[]> countByStatus(Integer studentId);
    
    /**
     * 统计课程考勤情况
     */
    @Query("SELECT a.status, COUNT(a) FROM Attendance a WHERE a.course.courseId = ?1 GROUP BY a.status")
    List<Object[]> countByStatusByCourse(Integer courseId);
}
