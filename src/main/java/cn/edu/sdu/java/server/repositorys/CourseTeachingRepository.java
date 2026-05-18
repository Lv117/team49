package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.CourseTeaching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CourseTeaching 任课安排数据操作接口
 */
@Repository
public interface CourseTeachingRepository extends JpaRepository<CourseTeaching, Integer> {
    
    /**
     * 根据教师ID查询任课安排
     */
    @Query(value = "from CourseTeaching ct where ct.teacher.personId = ?1")
    List<CourseTeaching> findByTeacherId(Integer teacherId);
    
    /**
     * 根据课程ID查询任课安排
     */
    @Query(value = "from CourseTeaching ct where ct.course.courseId = ?1")
    List<CourseTeaching> findByCourseId(Integer courseId);
    
    /**
     * 根据学期查询任课安排
     */
    @Query(value = "from CourseTeaching ct where ct.semester = ?1")
    List<CourseTeaching> findBySemester(String semester);
}
