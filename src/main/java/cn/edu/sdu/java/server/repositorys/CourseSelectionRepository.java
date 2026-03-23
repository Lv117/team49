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
     * 根据学生 ID 查询选课记录
     */
    List<CourseSelection> findByStudentPersonId(Integer studentId);
    
    /**
     * 根据课程 ID 查询选课记录
     */
    List<CourseSelection> findByCourseCourseId(Integer courseId);
    
    /**
     * 根据学生和课程查询选课记录
     */
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
    List<CourseSelection> findByStudentPersonIdAndStatus(Integer studentId, String status);
    
    /**
     * 查询某门课的选课（按状态筛选）
     */
    List<CourseSelection> findByCourseCourseIdAndStatus(Integer courseId, String status);
}
