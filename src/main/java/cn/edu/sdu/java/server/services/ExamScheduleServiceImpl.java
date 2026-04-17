package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.CourseSelection;
import cn.edu.sdu.java.server.models.ExamSchedule;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.models.StudentExamSchedule;
import cn.edu.sdu.java.server.payload.response.OptionItem;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.CourseSelectionRepository;
import cn.edu.sdu.java.server.repositorys.ExamScheduleRepository;
import cn.edu.sdu.java.server.repositorys.StudentExamScheduleRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Service
public class ExamScheduleServiceImpl implements ExamScheduleService {

    @Autowired
    private ExamScheduleRepository examScheduleRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentExamScheduleRepository studentExamScheduleRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseSelectionRepository courseSelectionRepository;

    @Override
    public List<OptionItem> getYearSemesterOptionList() {
        // 这里可以从数据库中查询学年学期数据，这里为了演示返回一些示例数据
        List<OptionItem> yearSemesterList = new ArrayList<>();
        yearSemesterList.add(new OptionItem(1, "1", "2023-2024学年第一学期"));
        yearSemesterList.add(new OptionItem(2, "2", "2023-2024学年第二学期"));
        yearSemesterList.add(new OptionItem(3, "3", "2024-2025学年第一学期"));
        yearSemesterList.add(new OptionItem(4, "4", "2024-2025学年第二学期"));
        return yearSemesterList;
    }

    @Override
    public List<OptionItem> getCourseOptionList() {
        // 从数据库中查询课程数据
        List<Course> courses = courseRepository.findAll();
        List<OptionItem> courseList = new ArrayList<>();
        for (Course course : courses) {
            courseList.add(new OptionItem(course.getCourseId(), course.getNum(), course.getName()));
        }
        return courseList;
    }

    @Override
    public List<Map<String, Object>> getExamScheduleList(Integer yearSemesterId, String courseName, String keyword) {
        List<ExamSchedule> examSchedules;
        if (keyword != null && !keyword.isEmpty()) {
            examSchedules = examScheduleRepository.findByYearSemesterIdAndCourseNameContainingAndKeyword(
                    yearSemesterId, courseName != null ? courseName : "", keyword);
        } else {
            examSchedules = examScheduleRepository.findByYearSemesterIdAndCourseNameContaining(
                    yearSemesterId, courseName != null ? courseName : "");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamSchedule examSchedule : examSchedules) {
            Map<String, Object> map = new HashMap<>();
            map.put("examId", examSchedule.getExamId());
            map.put("courseNum", ""); // 课程编号，这里可以根据实际情况从课程表中查询
            map.put("courseName", examSchedule.getCourseName());
            map.put("teacher", examSchedule.getTeacher());
            map.put("examTime", examSchedule.getExamTime());
            map.put("examRoom", examSchedule.getExamRoom());
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getStudentExamScheduleList(String studentId, Integer yearSemesterId) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (studentId == null || studentId.isEmpty()) {
            return result;
        }

        Optional<Student> studentOp = studentRepository.findByPersonNum(studentId);
        if (studentOp.isEmpty()) {
            return result;
        }

        Student student = studentOp.get();
        List<CourseSelection> courseSelections = courseSelectionRepository.findByStudentPersonId(student.getPersonId());
        if (courseSelections == null || courseSelections.isEmpty()) {
            return result;
        }

        Set<Integer> selectedCourseIds = new HashSet<>();
        Set<String> selectedCourseNames = new HashSet<>();
        for (CourseSelection cs : courseSelections) {
            String status = cs.getStatus();
            if ("已退课".equals(status)) {
                continue;
            }
            if (cs.getCourse() != null && cs.getCourse().getCourseId() != null) {
                selectedCourseIds.add(cs.getCourse().getCourseId());
                if (cs.getCourse().getName() != null && !cs.getCourse().getName().isEmpty()) {
                    selectedCourseNames.add(cs.getCourse().getName());
                }
            }
        }
        if (selectedCourseIds.isEmpty() && selectedCourseNames.isEmpty()) {
            return result;
        }

        Integer queryYearSemesterId = yearSemesterId == null ? 0 : yearSemesterId;
        List<ExamSchedule> examSchedules = examScheduleRepository.findByYearSemesterIdAndCourseNameContaining(queryYearSemesterId, "");
        for (ExamSchedule examSchedule : examSchedules) {
            Integer examCourseId = examSchedule.getCourseId();
            String examCourseName = examSchedule.getCourseName();
            boolean matchByCourseId = examCourseId != null && selectedCourseIds.contains(examCourseId);
            boolean matchByCourseName = (examCourseId == null) && examCourseName != null && selectedCourseNames.contains(examCourseName);
            if (!matchByCourseId && !matchByCourseName) {
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            map.put("examId", examSchedule.getExamId());
            map.put("courseNum", "");
            map.put("courseName", examSchedule.getCourseName());
            map.put("teacher", examSchedule.getTeacher());
            map.put("examTime", examSchedule.getExamTime());
            map.put("examRoom", examSchedule.getExamRoom());

            List<StudentExamSchedule> studentExamSchedules = studentExamScheduleRepository.findByExamIdAndStudentId(examSchedule.getExamId(), studentId);
            if (studentExamSchedules != null && !studentExamSchedules.isEmpty()) {
                StudentExamSchedule ses = studentExamSchedules.get(0);
                map.put("seatNumber", ses.getSeatNumber());
                map.put("examTicket", ses.getExamTicket());
            } else {
                map.put("seatNumber", "");
                map.put("examTicket", "");
            }
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getExamStudentList(String examId) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (examId == null || examId.isEmpty()) {
            return result;
        }
        ExamSchedule examSchedule = examScheduleRepository.findById(examId).orElse(null);
        if (examSchedule == null) {
            return result;
        }

        List<CourseSelection> courseSelections = new ArrayList<>();
        if (examSchedule.getCourseId() != null) {
            courseSelections.addAll(courseSelectionRepository.findByCourseCourseId(examSchedule.getCourseId()));
        } else if (examSchedule.getCourseName() != null && !examSchedule.getCourseName().isEmpty()) {
            List<Course> nameMatchedCourses = courseRepository.findByName(examSchedule.getCourseName());
            Set<Integer> mergedCourseIds = new HashSet<>();
            for (Course c : nameMatchedCourses) {
                if (c != null && c.getCourseId() != null && mergedCourseIds.add(c.getCourseId())) {
                    courseSelections.addAll(courseSelectionRepository.findByCourseCourseId(c.getCourseId()));
                }
            }
        }

        Set<Integer> personIdSet = new HashSet<>();
        int index = 1;
        for (CourseSelection cs : courseSelections) {
            if ("已退课".equals(cs.getStatus()) || cs.getStudent() == null || cs.getStudent().getPerson() == null) {
                continue;
            }
            Student student = cs.getStudent();
            if (!personIdSet.add(student.getPersonId())) {
                continue;
            }
            String studentNum = student.getPerson().getNum();
            String studentName = student.getPerson().getName();

            String examTicket = "";
            String seatNumber = "";
            List<StudentExamSchedule> sesList = studentExamScheduleRepository.findByExamIdAndStudentId(examId, studentNum);
            if (sesList != null && !sesList.isEmpty()) {
                StudentExamSchedule ses = sesList.get(0);
                examTicket = ses.getExamTicket() == null ? "" : ses.getExamTicket();
                seatNumber = ses.getSeatNumber() == null ? "" : String.valueOf(ses.getSeatNumber());
            }

            Map<String, Object> row = new HashMap<>();
            row.put("index", index++);
            row.put("name", studentName == null ? "" : studentName);
            row.put("num", studentNum == null ? "" : studentNum);
            row.put("examTicket", examTicket);
            row.put("seatNumber", seatNumber);
            result.add(row);
        }
        return result;
    }

    @Override
    public ExamSchedule saveExamSchedule(String originExamId, ExamSchedule examSchedule) {
        String newExamId = examSchedule.getExamId();
        if (newExamId == null || newExamId.isEmpty()) {
            throw new RuntimeException("考试序号不能为空");
        }

        if (originExamId != null && !originExamId.isEmpty()) {
            Optional<ExamSchedule> originOp = examScheduleRepository.findById(originExamId);
            if (originOp.isPresent()) {
                if (!originExamId.equals(newExamId)) {
                    if (examScheduleRepository.existsById(newExamId)) {
                        throw new RuntimeException("考试序号已存在，不能修改为重复序号");
                    }
                    ExamSchedule renamedExam = new ExamSchedule();
                    renamedExam.setExamId(newExamId);
                    renamedExam.setCourseName(examSchedule.getCourseName());
                    renamedExam.setTeacher(examSchedule.getTeacher());
                    renamedExam.setExamTime(examSchedule.getExamTime());
                    renamedExam.setExamRoom(examSchedule.getExamRoom());
                    renamedExam.setYearSemesterId(examSchedule.getYearSemesterId());
                    renamedExam.setCourseId(examSchedule.getCourseId());
                    ExamSchedule saved = examScheduleRepository.save(renamedExam);

                    List<StudentExamSchedule> studentExamSchedules = studentExamScheduleRepository.findByExamId(originExamId);
                    for (StudentExamSchedule ses : studentExamSchedules) {
                        ses.setExamId(newExamId);
                    }
                    studentExamScheduleRepository.saveAll(studentExamSchedules);
                    examScheduleRepository.deleteById(originExamId);
                    generateStudentExamSchedules(saved.getExamId());
                    return saved;
                }

                ExamSchedule originExam = originOp.get();
                originExam.setCourseName(examSchedule.getCourseName());
                originExam.setTeacher(examSchedule.getTeacher());
                originExam.setExamTime(examSchedule.getExamTime());
                originExam.setExamRoom(examSchedule.getExamRoom());
                originExam.setYearSemesterId(examSchedule.getYearSemesterId());
                originExam.setCourseId(examSchedule.getCourseId());
                ExamSchedule saved = examScheduleRepository.save(originExam);
                generateStudentExamSchedules(saved.getExamId());
                return saved;
            }
        }

        ExamSchedule savedExamSchedule = examScheduleRepository.save(examSchedule);
        generateStudentExamSchedules(savedExamSchedule.getExamId());
        return savedExamSchedule;
    }

    @Override
    public void deleteExamSchedule(String examId) {
        // 先删除学生考试安排
        List<StudentExamSchedule> studentExamSchedules = studentExamScheduleRepository.findByExamId(examId);
        studentExamScheduleRepository.deleteAll(studentExamSchedules);
        // 再删除考试安排
        examScheduleRepository.deleteById(examId);
    }

    @Override
    public void generateStudentExamSchedules(String examId) {
        // 获取考试信息，包括课程ID
        ExamSchedule examSchedule = examScheduleRepository.findById(examId).orElse(null);
        if (examSchedule == null) {
            return;
        }
        
        // 获取已选择该课程的学生（优先 courseId，缺失时回退 courseName）
        List<CourseSelection> courseSelections = new ArrayList<>();
        Integer examCourseId = examSchedule.getCourseId();
        if (examCourseId != null) {
            courseSelections.addAll(courseSelectionRepository.findByCourseCourseId(examCourseId));
        } else if (examSchedule.getCourseName() != null && !examSchedule.getCourseName().isEmpty()) {
            List<Course> nameMatchedCourses = courseRepository.findByName(examSchedule.getCourseName());
            Set<Integer> mergedCourseIds = new HashSet<>();
            for (Course c : nameMatchedCourses) {
                if (c != null && c.getCourseId() != null && mergedCourseIds.add(c.getCourseId())) {
                    courseSelections.addAll(courseSelectionRepository.findByCourseCourseId(c.getCourseId()));
                }
            }
        }
        if (courseSelections.isEmpty()) {
            return;
        }
        // 为每个已选课学生生成考试安排
        Random random = new Random();
        
        for (CourseSelection courseSelection : courseSelections) {
            if ("已退课".equals(courseSelection.getStatus())) {
                continue;
            }
            // 检查student和person是否为null
            Student student = courseSelection.getStudent();
            if (student == null || student.getPerson() == null) {
                continue;
            }
            // 检查是否已经为该学生生成了考试安排
            List<StudentExamSchedule> existingSchedules = studentExamScheduleRepository.findByExamIdAndStudentId(examId, student.getPerson().getNum());
            if (!existingSchedules.isEmpty()) {
                continue;
            }
            
            StudentExamSchedule ses = new StudentExamSchedule();
            ses.setExamId(examId);
            ses.setStudentId(student.getPerson().getNum());
            
            // 生成1-100之间的随机座位号
            Integer seatNumber;
            boolean seatNumberExists;
            do {
                final Integer tempSeatNumber = random.nextInt(100) + 1;
                seatNumber = tempSeatNumber;
                List<StudentExamSchedule> existingSchedulesForExam = studentExamScheduleRepository.findByExamId(examId);
                seatNumberExists = false;
                for (StudentExamSchedule s : existingSchedulesForExam) {
                    if (s.getSeatNumber() != null && s.getSeatNumber().equals(tempSeatNumber)) {
                        seatNumberExists = true;
                        break;
                    }
                }
            } while (seatNumberExists);
            ses.setSeatNumber(seatNumber);
            
            // 生成8位随机准考证号
            String examTicket;
            boolean examTicketExists;
            do {
                // 生成10000000-99999999之间的随机数
                final int tempExamTicket = 10000000 + random.nextInt(90000000);
                examTicket = String.valueOf(tempExamTicket);
                List<StudentExamSchedule> existingSchedulesForExam = studentExamScheduleRepository.findByExamId(examId);
                examTicketExists = false;
                for (StudentExamSchedule s : existingSchedulesForExam) {
                    if (s.getExamTicket() != null && s.getExamTicket().equals(examTicket)) {
                        examTicketExists = true;
                        break;
                    }
                }
            } while (examTicketExists);
            ses.setExamTicket(examTicket);
            
            studentExamScheduleRepository.save(ses);
        }
    }
}
