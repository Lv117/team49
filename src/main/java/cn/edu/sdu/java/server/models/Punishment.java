package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "punishment")
@Getter
@Setter
public class Punishment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer punishmentId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "student_name", length = 50)
    private String studentName;

    @Column(name = "student_num", length = 20)
    private String studentNum;

    @Column(name = "punishment_type", length = 30, nullable = false)
    private String punishmentType;

    @Column(name = "punishment_date")
    private LocalDate punishmentDate;

    @Column(name = "status", length = 20)
    private String status = "pending";

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
