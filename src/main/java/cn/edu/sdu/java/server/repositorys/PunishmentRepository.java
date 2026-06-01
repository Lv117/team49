package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.Punishment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PunishmentRepository extends JpaRepository<Punishment, Integer> {
    List<Punishment> findByStudentId(Integer studentId);
    List<Punishment> findByStudentNum(String studentNum);
    List<Punishment> findByStatus(String status);
}
