package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.DailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DailyActivityRepository extends JpaRepository<DailyActivity, Integer> {
    List<DailyActivity> findByStudentId(Integer studentId);
    List<DailyActivity> findByStatus(String status);
}
