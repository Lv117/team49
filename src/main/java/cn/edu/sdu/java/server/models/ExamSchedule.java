package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "exam_schedule")
public class ExamSchedule {
    @Id
    private String examId;

    @Column(name = "course_name")
    private String courseName;

    private String teacher;

    @Column(name = "exam_time")
    private String examTime;

    @Column(name = "exam_room")
    private String examRoom;

    @Column(name = "year_semester_id")
    private Integer yearSemesterId;
    
    @Column(name = "course_id")
    private Integer courseId;

    // Getters and setters
    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getExamTime() {
        return examTime;
    }

    public void setExamTime(String examTime) {
        this.examTime = examTime;
    }

    public String getExamRoom() {
        return examRoom;
    }

    public void setExamRoom(String examRoom) {
        this.examRoom = examRoom;
    }

    public Integer getYearSemesterId() {
        return yearSemesterId;
    }

    public void setYearSemesterId(Integer yearSemesterId) {
        this.yearSemesterId = yearSemesterId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }
}