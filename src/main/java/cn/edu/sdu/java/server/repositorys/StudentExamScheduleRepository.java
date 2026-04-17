package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.StudentExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentExamScheduleRepository extends JpaRepository<StudentExamSchedule, Integer> {
    List<StudentExamSchedule> findByExamId(String examId);
    List<StudentExamSchedule> findByStudentId(String studentId);
    
    @Query("SELECT MAX(s.seatNumber) FROM StudentExamSchedule s WHERE s.examId = :examId")
    Integer findMaxSeatNumberByExamId(@Param("examId") String examId);
    
    @Query("SELECT MAX(s.examTicket) FROM StudentExamSchedule s WHERE s.examId = :examId")
    String findMaxExamTicketByExamId(@Param("examId") String examId);
    
    List<StudentExamSchedule> findByExamIdAndStudentId(String examId, String studentId);
}