package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 审批记录实体类
 * 用于记录荣誉奖励、创新项目、日常活动等的审批历史
 */
@Entity
@Table(name = "approval_record")
@Getter
@Setter
public class ApprovalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "business_type", length = 50, nullable = false)
    private String businessType;

    @Column(name = "business_id", nullable = false)
    private Integer businessId;

    @Column(name = "from_status", length = 50)
    private String fromStatus;

    @Column(name = "to_status", length = 50, nullable = false)
    private String toStatus;

    @Column(name = "approval_opinion", length = 500)
    private String approvalOpinion;

    @Column(name = "operator_id", nullable = false)
    private Integer operatorId;

    @Column(name = "operator_name", length = 50)
    private String operatorName;

    @Column(name = "operate_time", nullable = false)
    private LocalDateTime operateTime;

    @Column(name = "remark", length = 200)
    private String remark;
}
