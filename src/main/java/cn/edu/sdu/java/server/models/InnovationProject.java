package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "innovation_project")
@Getter
@Setter
public class InnovationProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "project_name", length = 100, nullable = false)
    private String projectName;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "student_name", length = 50)
    private String studentName;

    @Column(name = "project_type", length = 50)
    private String projectType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", length = 20)
    private String status = "draft";

    @Column(name = "approval_opinion", columnDefinition = "TEXT")
    private String approvalOpinion;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
