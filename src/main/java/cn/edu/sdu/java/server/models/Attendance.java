package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Attendance 考勤表实体类 记录学生上课考勤信息
 * Integer attendanceId 考勤表主键 attendance_id
 * Student student 关联学生 person_id
 * Course course 关联课程 course_id
 * LocalDate attendanceDate 考勤日期
 * String status 考勤状态（出勤/缺勤/迟到/早退）
 * String type 考勤类型（正常/病假/事假/旷课）
 */
@Getter
@Setter
@Entity
@Table(name = "attendance")
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer attendanceId;

    @ManyToOne
    @JoinColumn(name = "studentId")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "courseId")
    private Course course;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Size(max = 20)
    private String status;  // 出勤、缺勤、迟到、早退

    @Size(max = 20)
    private String type;    // 正常、病假、事假、旷课

    @Size(max = 200)
    private String remark;

    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
