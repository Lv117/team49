package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.StudentLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentLeaveRepository extends JpaRepository<StudentLeave,Integer> {
    // 参数说明：
    // ?1 = state (状态筛选，-1表示全部)
    // ?2 = search (搜索关键词，匹配学生姓名或学号)
    // ?3 = studentNum (学生学号精确匹配，空字符串表示不限制)
    // ?4 = teacherNum (教师工号精确匹配，空字符串表示不限制)
    // ?5 = teacherId (教师ID精确匹配，null表示不限制)
    @Query(value = "SELECT sl FROM StudentLeave sl WHERE (?1 <0 or sl.state = ?1) and (?2='' or sl.student.person.num like %?2% or sl.student.person.name like %?2%) and (?3='' or sl.student.person.num = ?3) and (?4='' or sl.teacher.person.num =?4) and (?5 is null or sl.teacher.personId = ?5)")
    List<StudentLeave> getStudentLeaveList(Integer state, String search, String studentNum, String teacherNum, Integer teacherId);

    @Query(value="select s.student.personId, count(s.studentLeaveId) from StudentLeave s where s.student.personId in ?1 and s.state in (1, 3) group by s.student.personId" )
    List<?> getStudentStatisticsList(List<Integer> personId);
}
