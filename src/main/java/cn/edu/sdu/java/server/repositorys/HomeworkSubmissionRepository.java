package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.HomeworkSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * HomeworkSubmission 作业提交数据操作接口
 */
@Repository
public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, Integer> {
    
    /**
     * 根据作业 ID 查询提交记录
     */
    List<HomeworkSubmission> findByHomeworkHomeworkId(Integer homeworkId);
    
    /**
     * 根据学生 ID 查询提交记录
     */
    List<HomeworkSubmission> findByStudentPersonId(Integer studentId);
    
    /**
     * 根据作业和学生查询提交记录
     */
    List<HomeworkSubmission> findByHomeworkHomeworkIdAndStudentPersonId(Integer homeworkId, Integer studentId);
    
    /**
     * 统计作业提交情况
     */
    long countByHomeworkHomeworkId(Integer homeworkId);
    
    /**
     * 统计已批改的作业数量
     */
    long countByHomeworkHomeworkIdAndStatus(Integer homeworkId, String status);
    
    /**
     * 查询学生的所有提交记录
     */
    @Query("SELECT hs FROM HomeworkSubmission hs WHERE hs.student.personId = ?1 ORDER BY hs.submitTime DESC")
    List<HomeworkSubmission> findByStudentPersonIdOrderBySubmitTimeDesc(Integer studentId);
}
