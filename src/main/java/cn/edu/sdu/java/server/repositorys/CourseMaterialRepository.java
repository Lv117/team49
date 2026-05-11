package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.CourseMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CourseMaterial 数据操作接口
 */
@Repository
public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, Integer> {
    
    @Query(value = "from CourseMaterial where (?1=0 or courseId=?1) and (?2 is null or courseType=?2) and (?3 is null or materialName like %?3%)")
    List<CourseMaterial> findByConditions(Integer courseId, String courseType, String keyword);
}
