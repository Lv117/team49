package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;

@Entity
@Table(name = "student_exam_schedule")
public class StudentExamSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "exam_id")
    private String examId;

    @Column(name = "student_id")
    private String studentId;

    @Column(name = "seat_number")
    private Integer seatNumber;

    @Column(name = "exam_ticket")
    private String examTicket;

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getExamTicket() {
        return examTicket;
    }

    public void setExamTicket(String examTicket) {
        this.examTicket = examTicket;
    }
}