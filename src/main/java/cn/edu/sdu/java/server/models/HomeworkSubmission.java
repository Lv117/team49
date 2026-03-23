package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * HomeworkSubmission 作业提交表实体类 记录学生提交作业的信息
 * Integer submissionId 提交表主键 submission_id
 * Homework homework 关联作业 homework_id
 * Student student 关联学生 person_id
 * String content 提交内容
 * String attachmentUrl 附件地址
 * LocalDateTime submitTime 提交时间
 * BigDecimal score 得分
 * String comment 评语
 */
@Getter
@Setter
@Entity
@Table(name = "homework_submission")
public class HomeworkSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer submissionId;

    @ManyToOne
    @JoinColumn(name = "homeworkId")
    private Homework homework;

    @ManyToOne
    @JoinColumn(name = "studentId")
    private Student student;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Size(max = 200)
    private String attachmentUrl;

    private LocalDateTime submitTime;

    @Column(precision = 4, scale = 1)
    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Size(max = 20)
    private String status;  // 已提交、已批改、已逾期

    @PrePersist
    protected void onCreate() {
        submitTime = LocalDateTime.now();
    }
}
