package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "honor")
@Getter
@Setter
public class Honor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "student_name", length = 50)
    private String studentName;

    @Column(name = "honor_name", length = 100, nullable = false)
    private String honorName;

    @Column(name = "honor_level", length = 50)
    private String honorLevel;

    @Column(name = "award_date")
    private LocalDate awardDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "certificate_url", length = 200)
    private String certificateUrl;

    @Column(name = "status", length = 20)
    private String status = "draft";

    @Column(name = "approval_opinion", length = 500)
    private String approvalOpinion;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
