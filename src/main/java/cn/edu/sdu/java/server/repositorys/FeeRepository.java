package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.Fee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Fee 消费数据操作接口
 */
@Repository
public interface FeeRepository extends JpaRepository<Fee, Integer> {
    
    @Query(value = "from Fee where student.personId = :personId order by createTime desc")
    List<Fee> findListByStudent(@Param("personId") Integer personId);
    
    @Query(value = "from Fee where (?1=0 or student.personId=?1) and (?2 is null or consumptionType=?2) and (?3 is null or day like %?3%)")
    List<Fee> findByConditions(@Param("personId") Integer personId, @Param("consumptionType") String consumptionType, @Param("keyword") String keyword);
    
    /**
     * 查询学生指定月份的消费记录
     */
    @Query(value = "from Fee where student.personId = :personId and consumptionType = :consumptionType and day >= :startDate and day <= :endDate order by day asc")
    List<Fee> findByStudentAndTypeAndDateRange(@Param("personId") Integer personId, 
                                                @Param("consumptionType") String consumptionType,
                                                @Param("startDate") String startDate,
                                                @Param("endDate") String endDate);
    
    /**
     * 根据学生ID和日期查询消费记录(旧版方法,保持向后兼容)
     */
    @Query(value = "from Fee where student.personId = ?1 and day = ?2")
    Optional<Fee> findByStudentPersonIdAndDay(Integer personId, String day);
    
    /**
     * 查询学生指定日期范围内的消费记录
     */
    @Query(value = "from Fee where student.personId = :personId and day >= :startDate and day <= :endDate order by day asc")
    List<Fee> findByStudentAndDateRange(@Param("personId") Integer personId,
                                        @Param("startDate") String startDate,
                                        @Param("endDate") String endDate);
    
    /**
     * 查询学生所有消费记录
     */
    @Query(value = "from Fee where student.personId = :personId order by day desc")
    List<Fee> findByStudentId(@Param("personId") Integer personId);

    /**
     * 查询指定学生指定日期范围之后的消费记录(用于月度趋势)
     */
    @Query(value = "from Fee where student.personId = :personId and day >= :startDate order by day asc")
    List<Fee> findByStudentAndStartDate(@Param("personId") Integer personId, @Param("startDate") String startDate);

    /**
     * 查询指定日期范围之后的所有消费记录(用于管理员/教师查看全量月度趋势)
     */
    @Query(value = "from Fee where day >= :startDate order by day asc")
    List<Fee> findAllFromStartDate(@Param("startDate") String startDate);
}
