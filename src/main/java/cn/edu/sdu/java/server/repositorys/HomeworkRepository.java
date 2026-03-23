package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.Homework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Homework 作业数据操作接口
 */
@Repository
public interface HomeworkRepository extends JpaRepository<Homework, Integer> {
    
    /**
     * 根据课程 ID 查询作业
     */
    List<Homework> findByCourseCourseId(Integer courseId);
    
    /**
     * 查询某门课的所有作业
     */
    List<Homework> findByCourseCourseIdOrderByCreateTimeDesc(Integer courseId);
    
    /**
     * 统计作业数量
     */
    long countByCourseCourseId(Integer courseId);
}
