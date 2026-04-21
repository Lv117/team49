package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.StudentDevelopment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DevelopmentRepository extends JpaRepository<StudentDevelopment, Integer> {
    
    /**
     * 根据学生ID查询
     */
    List<StudentDevelopment> findByStudentId(Integer studentId);
    
    /**
     * 根据状态查询
     */
    List<StudentDevelopment> findByStatus(String status);
    
    /**
     * 根据类型查询
     */
    List<StudentDevelopment> findByDevelopmentType(String developmentType);
    
    /**
     * 根据学生ID和类型查询
     */
    List<StudentDevelopment> findByStudentIdAndDevelopmentType(Integer studentId, String developmentType);
    
    /**
     * 分页查询所有
     */
    Page<StudentDevelopment> findAll(Pageable pageable);
    
    /**
     * 根据类型分页查询
     */
    Page<StudentDevelopment> findByDevelopmentType(String developmentType, Pageable pageable);
}
