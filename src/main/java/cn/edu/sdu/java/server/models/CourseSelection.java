package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * CourseSelection 选课表实体类 记录学生选课信息
 * Integer selectionId 选课表主键 selection_id
 * Student student 关联学生 person_id
 * Course course 关联课程 course_id
 * String status 选课状态（已选/已退课/已完成）
 * BigDecimal score 最终成绩
 */
@Getter
@Setter
@Entity
@Table(name = "course_selection")
public class CourseSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer selectionId;

    @ManyToOne
    @JoinColumn(name = "studentId")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "courseId")
    private Course course;

    @Size(max = 20)
    private String status;  // 已选、已退课、已完成

    @Column(precision = 4, scale = 1)
    private java.math.BigDecimal score;

    private Integer ranking;

    private LocalDateTime selectionTime;

    @Size(max = 200)
    private String remark;

    @PrePersist
    protected void onCreate() {
        selectionTime = LocalDateTime.now();
    }
}
