package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, String> {

    @Query("SELECT e FROM ExamSchedule e WHERE (:yearSemesterId = 0 OR e.yearSemesterId = :yearSemesterId) AND e.courseName LIKE %:courseName%")
    List<ExamSchedule> findByYearSemesterIdAndCourseNameContaining(
            @Param("yearSemesterId") Integer yearSemesterId,
            @Param("courseName") String courseName
    );

    @Query("SELECT e FROM ExamSchedule e WHERE (:yearSemesterId = 0 OR e.yearSemesterId = :yearSemesterId) AND e.courseName LIKE %:courseName% AND (e.teacher LIKE %:keyword% OR e.examRoom LIKE %:keyword%)")
    List<ExamSchedule> findByYearSemesterIdAndCourseNameContainingAndKeyword(
            @Param("yearSemesterId") Integer yearSemesterId,
            @Param("courseName") String courseName,
            @Param("keyword") String keyword
    );

    @Query("SELECT DISTINCT e.courseId FROM ExamSchedule e WHERE e.courseId IS NOT NULL AND (e.teacher = :teacherName OR e.teacher LIKE %:teacherName%)")
    List<Integer> findDistinctCourseIdsByTeacherName(@Param("teacherName") String teacherName);
}
