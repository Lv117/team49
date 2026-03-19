package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_activity")
@Getter
@Setter
public class DailyActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "student_name", length = 50)
    private String studentName;

    @Column(name = "activity_name", length = 100, nullable = false)
    private String activityName;

    @Column(name = "activity_type", length = 50)
    private String activityType;

    @Column(name = "activity_date")
    private LocalDate activityDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "hours")
    private Integer hours;

    @Column(name = "status", length = 20)
    private String status = "draft";

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
