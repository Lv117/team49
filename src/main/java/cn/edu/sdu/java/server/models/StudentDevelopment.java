package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "student_development")
@Getter
@Setter
public class StudentDevelopment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 基础信息
    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "student_name", length = 50)
    private String studentName;

    @Column(name = "title", length = 200, nullable = false)
    private String title;  // 标题(项目名称/荣誉名称/竞赛名称/成果名称)

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;  // 描述

    // 类型字段(核心)
    @Column(name = "development_type", length = 20, nullable = false)
    private String developmentType;  // honor-荣誉奖励, innovation-创新实践, competition-学科竞赛, achievement-科技成果

    // 审批相关
    @Column(name = "status", length = 20)
    private String status = "draft";  // draft-草稿, submitted-已提交, approved-已通过, rejected-已驳回

    @Column(name = "approval_opinion", columnDefinition = "TEXT")
    private String approvalOpinion;  // 审批意见

    @Column(name = "approver_id")
    private Integer approverId;  // 审批人ID

    @Column(name = "approve_time")
    private LocalDateTime approveTime;  // 审批时间

    // 扩展字段(JSON格式存储不同类型特有信息)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_info", columnDefinition = "json")
    private Map<String, Object> extraInfo;
    /*
     * honor: {"level":"国家级","issuer":"教育部"}
     * innovation: {"field":"人工智能","outcome":"论文"}
     * competition: {"competitionName":"ACM","awardLevel":"一等奖"}
     * achievement: {"patentType":"发明专利","status":"已授权"}
     */

    // 时间信息
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (status == null) {
            status = "draft";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
