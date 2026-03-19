package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.InnovationProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InnovationProjectRepository extends JpaRepository<InnovationProject, Integer> {
    List<InnovationProject> findByStudentId(Integer studentId);
    List<InnovationProject> findByStatus(String status);
}
