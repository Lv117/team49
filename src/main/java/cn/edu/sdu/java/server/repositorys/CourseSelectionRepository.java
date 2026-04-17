package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.CourseSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CourseSelection 选课数据操作接口
 */
@Repository
public interface CourseSelectionRepository extends JpaRepository<CourseSelection, Integer> {
    
    /**
     * 根据学生 ID 查询选课记录（使用 JOIN FETCH 避免 N+1 查询）
     */
    @Query("SELECT cs FROM CourseSelection cs JOIN FETCH cs.student s JOIN FETCH s.person JOIN FETCH cs.course WHERE cs.student.personId = ?1")
    List<CourseSelection> findByStudentPersonId(Integer studentId);
    
    /**
     * 根据课程 ID 查询选课记录（使用 JOIN FETCH 避免 N+1 查询）
     */
    @Query("SELECT cs FROM CourseSelection cs JOIN FETCH cs.student s JOIN FETCH s.person JOIN FETCH cs.course WHERE cs.course.courseId = ?1")
    List<CourseSelection> findByCourseCourseId(Integer courseId);
    
    /**
     * 根据学生和课程查询选课记录（使用 JOIN FETCH 避免 N+1 查询）
     */
    @Query("SELECT cs FROM CourseSelection cs JOIN FETCH cs.student s JOIN FETCH s.person JOIN FETCH cs.course WHERE cs.student.personId = ?1 AND cs.course.courseId = ?2")
    List<CourseSelection> findByStudentPersonIdAndCourseCourseId(Integer studentId, Integer courseId);
    
    /**
     * 统计某门课的选课人数
     */
    long countByCourseCourseId(Integer courseId);
    
    /**
     * 统计学生的选课数量
     */
    long countByStudentPersonId(Integer studentId);
    
    /**
     * 查询学生的选课（按状态筛选）
     */
    @Query("SELECT cs FROM CourseSelection cs JOIN FETCH cs.student s JOIN FETCH s.person JOIN FETCH cs.course WHERE cs.student.personId = ?1 AND cs.status = ?2")
    List<CourseSelection> findByStudentPersonIdAndStatus(Integer studentId, String status);
    
    /**
     * 查询某门课的选课（按状态筛选）
     */
    @Query("SELECT cs FROM CourseSelection cs JOIN FETCH cs.student s JOIN FETCH s.person JOIN FETCH cs.course WHERE cs.course.courseId = ?1 AND cs.status = ?2")
    List<CourseSelection> findByCourseCourseIdAndStatus(Integer courseId, String status);
}
