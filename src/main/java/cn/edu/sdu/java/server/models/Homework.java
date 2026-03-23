package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Homework 作业表实体类 记录教师发布的作业信息
 * Integer homeworkId 作业表主键 homework_id
 * Course course 关联课程 course_id
 * String title 作业标题
 * String content 作业内容
 * LocalDateTime deadline 截止时间
 * Integer maxScore 满分分数
 */
@Getter
@Setter
@Entity
@Table(name = "homework")
public class Homework {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer homeworkId;

    @ManyToOne
    @JoinColumn(name = "courseId")
    private Course course;

    @Size(max = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime deadline;

    private Integer maxScore;

    @Size(max = 200)
    private String attachmentUrl;

    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
