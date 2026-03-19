package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.Honor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HonorRepository extends JpaRepository<Honor, Integer> {
    List<Honor> findByStudentId(Integer studentId);
    List<Honor> findByStatus(String status);
}
