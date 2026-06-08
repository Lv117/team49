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
        
        // 如果传入了 teacherId，只返回该教师授课的课程
        Integer teacherId = dataRequest.getInteger("teacherId");
        List<Course> cList;
        if (teacherId != null) {
            System.out.println("[COURSE DEBUG] 查询教师授课课程 - teacherId: " + teacherId);
            cList = courseRepository.findByTeacherPersonPersonId(teacherId);
            // 如果有搜索关键词，在内存中过滤
            if (!numName.isEmpty()) {
                final String keyword = numName.toLowerCase();
                cList = cList.stream()
                    .filter(c -> (c.getNum() != null && c.getNum().toLowerCase().contains(keyword)) ||
                                 (c.getName() != null && c.getName().toLowerCase().contains(keyword)))
                    .collect(java.util.stream.Collectors.toList());
            }
        } else {
            // 没有 teacherId，使用原有的模糊查询逻辑
            cList = courseRepository.findCourseListByNumName(numName);
        }
        
        List<Map<String,Object>> dataList = new ArrayList<>();
        Map<String,Object> m;
        for (Course c : cList) {
            m = new HashMap<>();
            m.put("courseId", c.getCourseId()+"");
            m.put("num",c.getNum());
            m.put("name",c.getName());
            m.put("credit",c.getCredit()+"");
            m.put("coursePath",c.getCoursePath());
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
        System.out.println("[COURSE DEBUG] 返回课程数量: " + dataList.size());
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse courseSave(DataRequest dataRequest) {
        Integer courseId = dataRequest.getInteger("courseId");
        String num = dataRequest.getString("num");
        String name = dataRequest.getString("name");
        String coursePath = dataRequest.getString("coursePath");
        Double credit = dataRequest.getDouble("credit");
        String teacherNumOrId = dataRequest.getString("teacherId");
        String classroom = dataRequest.getString("classroom");
        String schedule = dataRequest.getString("schedule");

        Optional<Course> op;
        Course c = null;

        if (courseId != null) {
            op = courseRepository.findById(courseId);
            if (op.isPresent()) c = op.get();
        }
        if (c == null) c = new Course();

        Teacher teacher = null;
        if (teacherNumOrId != null && !teacherNumOrId.isEmpty()) {
            Optional<Person> personOp = personRepository.findByNum(teacherNumOrId);
            if (personOp.isPresent()) {
                Person p = personOp.get();
                Optional<Teacher> teacherOp = teacherRepository.findById(p.getPersonId());
                if (teacherOp.isPresent()) {
                    teacher = teacherOp.get();
                }
            } else {
                try {
                    Integer tid = Integer.parseInt(teacherNumOrId);
                    Optional<Teacher> teacherOp = teacherRepository.findById(tid);
                    if (teacherOp.isPresent()) {
                        teacher = teacherOp.get();
                    }
                } catch (Exception ignored) {
                }
            }
        }

        c.setNum(num);
        c.setName(name);
        c.setCredit(credit);
        c.setCoursePath(coursePath);
        c.setTeacher(teacher);
        c.setClassroom(classroom);
        c.setSchedule(schedule);

         courseRepository.save(c);
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
