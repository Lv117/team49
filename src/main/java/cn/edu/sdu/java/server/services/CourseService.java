package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.Teacher;
import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.TeacherRepository;
import cn.edu.sdu.java.server.repositorys.PersonRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final PersonRepository personRepository;
    public CourseService(CourseRepository courseRepository, TeacherRepository teacherRepository, PersonRepository personRepository) {
        this.courseRepository = courseRepository;
        this.teacherRepository = teacherRepository;
        this.personRepository = personRepository;
    }

    public DataResponse getCourseList(DataRequest dataRequest) {
        String numName = dataRequest.getString("numName");
        if(numName == null)
            numName = "";
        List<Course> cList = courseRepository.findCourseListByNumName(numName);  //数据库查询操作
        List<Map<String,Object>> dataList = new ArrayList<>();
        Map<String,Object> m;
        Course pc;
        for (Course c : cList) {
            m = new HashMap<>();
            m.put("courseId", c.getCourseId()+"");
            m.put("num",c.getNum());
            m.put("name",c.getName());
            m.put("credit",c.getCredit()+"");
            m.put("coursePath",c.getCoursePath());
            pc =c.getPreCourse();
            if(pc != null) {
                m.put("preCourse",pc.getName());
                m.put("preCourseId",pc.getCourseId());
            }
            // 添加任课教师信息
            if(c.getTeacher() != null) {
                m.put("teacherId", c.getTeacher().getPersonId());
                m.put("teacherNum", c.getTeacher().getPerson() != null ? c.getTeacher().getPerson().getNum() : "");
                m.put("teacherName", c.getTeacher().getPerson() != null ? c.getTeacher().getPerson().getName() : "");
            }
            // 添加上课地点
            m.put("classroom", c.getClassroom());
            // 添加上课时间
            m.put("schedule", c.getSchedule());
            dataList.add(m);
        }
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse courseSave(DataRequest dataRequest) {
        Integer courseId = dataRequest.getInteger("courseId");
        String num = dataRequest.getString("num");
        String name = dataRequest.getString("name");
        String coursePath = dataRequest.getString("coursePath");
        Integer preCourseId = dataRequest.getInteger("preCourseId");
        Double credit = dataRequest.getDouble("credit");
        String teacherNumOrId = dataRequest.getString("teacherId");
        String classroom = dataRequest.getString("classroom");
        String schedule = dataRequest.getString("schedule");
        
        // 调试日志
        System.out.println("[CourseSave Debug] courseId=" + courseId + ", num=" + num + ", teacherId=" + teacherNumOrId + ", classroom=" + classroom);

        Optional<Course> op;
        Course c = null;

        if (courseId != null) {
            op = courseRepository.findById(courseId);
            if (op.isPresent()) c = op.get();
        }
        if (c == null) c = new Course();

        Course pc = null;
        if (preCourseId != null) {
            op = courseRepository.findById(preCourseId);
            if (op.isPresent()) pc = op.get();
        }

        Teacher teacher = null;
        if (teacherNumOrId != null && !teacherNumOrId.isEmpty()) {
            System.out.println("[CourseSave Debug] 尝试匹配教师: " + teacherNumOrId);
            Optional<Person> personOp = personRepository.findByNum(teacherNumOrId);
            if (personOp.isPresent()) {
                Person p = personOp.get();
                System.out.println("[CourseSave Debug] 找到 Person, ID=" + p.getPersonId());
                Optional<Teacher> teacherOp = teacherRepository.findById(p.getPersonId());
                if (teacherOp.isPresent()) {
                    teacher = teacherOp.get();
                    System.out.println("[CourseSave Debug] 找到 Teacher!");
                } else {
                    System.out.println("[CourseSave Debug] 未找到对应 Teacher 记录 (该人员可能不是教师)");
                }
            } else {
                System.out.println("[CourseSave Debug] 未找到工号为 " + teacherNumOrId + " 的人员");
                try {
                    Integer tid = Integer.parseInt(teacherNumOrId);
                    Optional<Teacher> teacherOp = teacherRepository.findById(tid);
                    if (teacherOp.isPresent()) {
                        teacher = teacherOp.get();
                        System.out.println("[CourseSave Debug] 通过 ID 找到 Teacher!");
                    } else {
                        System.out.println("[CourseSave Debug] 通过 ID " + tid + " 也未找到 Teacher");
                    }
                } catch (Exception e) {
                    System.out.println("[CourseSave Debug] 解析 ID 失败");
                }
            }
        }

        c.setNum(num);
        c.setName(name);
        c.setCredit(credit);
        c.setCoursePath(coursePath);
        c.setPreCourse(pc);
        c.setTeacher(teacher);
        c.setClassroom(classroom);
        c.setSchedule(schedule);

        courseRepository.save(c);
        System.out.println("[CourseSave Debug] 保存成功, 最终 teacher=" + (teacher != null ? teacher.getPersonId() : "null") + ", classroom=" + classroom);
        return CommonMethod.getReturnMessageOK();
    }
    public DataResponse courseDelete(DataRequest dataRequest) {
        Integer courseId = dataRequest.getInteger("courseId");
        Optional<Course> op;
        Course c= null;
        if(courseId != null) {
            op = courseRepository.findById(courseId);
            if(op.isPresent()) {
                c = op.get();
                courseRepository.delete(c);
            }
        }
        return CommonMethod.getReturnMessageOK();
    }

}
