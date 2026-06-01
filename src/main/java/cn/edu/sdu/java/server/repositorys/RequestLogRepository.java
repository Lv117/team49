package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.RequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RequestLogRepository extends JpaRepository<RequestLog, Integer> {
    
    /**
     * 按用户名查询请求日志
     */
    List<RequestLog> findByUsername(String username);
    
    /**
     * 按时间范围查询请求日志
     */
    @Query("SELECT r FROM RequestLog r WHERE r.startTime >= :startTime AND r.startTime <= :endTime ORDER BY r.startTime DESC")
    List<RequestLog> findByTimeRange(@Param("startTime") String startTime, @Param("endTime") String endTime);
    
    /**
     * 按用户名和时间范围查询
     */
    @Query("SELECT r FROM RequestLog r WHERE r.username = :username AND r.startTime >= :startTime AND r.startTime <= :endTime ORDER BY r.startTime DESC")
    List<RequestLog> findByUsernameAndTimeRange(@Param("username") String username, @Param("startTime") String startTime, @Param("endTime") String endTime);
}
