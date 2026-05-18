package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * CourseTeaching 任课安排实体类
 * 用于管理教师的任课安排信息
 */
@Getter
@Setter
@Entity
@Table(name = "course_teaching")
public class CourseTeaching {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Size(max = 100)
    private String classroom;

    @Size(max = 50)
    private String semester;

    @Size(max = 20)
    private String status = "active";

    @Size(max = 200)
    private String remark;
}
