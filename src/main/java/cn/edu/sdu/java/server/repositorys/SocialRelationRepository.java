package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.SocialRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SocialRelationRepository extends JpaRepository<SocialRelation, Integer> {
    List<SocialRelation> findByStudentPersonId(Integer personId);
}
