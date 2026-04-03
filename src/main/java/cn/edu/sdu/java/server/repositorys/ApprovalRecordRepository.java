package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.ApprovalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Integer> {

    /**
     * 根据业务类型和业务 ID 查询审批记录
     */
    List<ApprovalRecord> findByBusinessTypeAndBusinessIdOrderByOperateTimeDesc(String businessType, Integer businessId);

    /**
     * 根据操作人 ID 查询审批记录
     */
    List<ApprovalRecord> findByOperatorIdOrderByOperateTimeDesc(Integer operatorId);
}
