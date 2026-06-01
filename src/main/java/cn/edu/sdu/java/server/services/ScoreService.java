package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.CourseSelection;
import cn.edu.sdu.java.server.models.Score;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.OptionItem;
import cn.edu.sdu.java.server.payload.response.OptionItemList;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.CourseSelectionRepository;
import cn.edu.sdu.java.server.repositorys.ScoreRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ScoreService {
    private static final Logger log = LoggerFactory.getLogger(ScoreService.class);

    private final CourseRepository courseRepository;
    private final CourseSelectionRepository courseSelectionRepository;
    private final ScoreRepository scoreRepository;
    private final StudentRepository studentRepository;
    private final TeacherDataScopeService teacherDataScopeService;

    public ScoreService(CourseRepository courseRepository,
                        CourseSelectionRepository courseSelectionRepository,
                        ScoreRepository scoreRepository,
                        StudentRepository studentRepository,
                        TeacherDataScopeService teacherDataScopeService) {
        this.courseRepository = courseRepository;
        this.courseSelectionRepository = courseSelectionRepository;
        this.scoreRepository = scoreRepository;
        this.studentRepository = studentRepository;
        this.teacherDataScopeService = teacherDataScopeService;
    }
    public OptionItemList getStudentItemOptionList( DataRequest dataRequest) {
        List<Student> sList = studentRepository.findStudentListByNumName("");  //数据库查询操作
        List<OptionItem> itemList = new ArrayList<>();
        Set<Integer> teacherStudentIds = getTeacherStudentIds();
        Integer currentStudentId = getCurrentStudentIdOrNull();
        for (Student s : sList) {
            Integer studentId = s.getPersonId();
            if (currentStudentId != null && !currentStudentId.equals(studentId)) {
                continue;
            }
            if (teacherDataScopeService.isCurrentRoleTeacher() && !teacherStudentIds.contains(studentId)) {
                continue;
            }
            itemList.add(new OptionItem( s.getPersonId(),s.getPersonId()+"", s.getPerson().getNum()+"-"+s.getPerson().getName()));
        }
        return new OptionItemList(0, itemList);
    }

    public OptionItemList getCourseItemOptionList(DataRequest dataRequest) {
        List<Course> sList = courseRepository.findAll();  //数据库查询操作
        List<OptionItem> itemList = new ArrayList<>();
        for (Course c : sList) {
            itemList.add(new OptionItem(c.getCourseId(),c.getCourseId()+"", c.getNum()+"-"+c.getName()));
        }
        return new OptionItemList(0, itemList);
    }

    public DataResponse getScoreList(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        personId = resolveStudentIdForQuery(personId, "教师仅可查看本人授课学生的成绩");
        if(personId == null)
            personId = 0;
        Integer courseId = dataRequest.getInteger("courseId");
        if(courseId == null)
            courseId = 0;
        List<Score> sList = scoreRepository.findByStudentCourse(personId, courseId);  //数据库查询操作
        sList = filterScoresForTeacher(sList);
        List<Map<String,Object>> dataList = new ArrayList<>();
        Map<String,Object> m;
        for (Score s : sList) {
            m = new HashMap<>();
            m.put("scoreId", s.getScoreId()+"");
            m.put("personId",s.getStudent().getPersonId()+"");
            m.put("courseId",s.getCourse().getCourseId()+"");
            m.put("studentNum",s.getStudent().getPerson().getNum());
            m.put("studentName",s.getStudent().getPerson().getName());
            m.put("className",s.getStudent().getClassName());
            m.put("courseNum",s.getCourse().getNum());
            m.put("courseName",s.getCourse().getName());
            m.put("credit",""+s.getCourse().getCredit());
            m.put("mark",""+s.getMark());
            dataList.add(m);
        }
        return CommonMethod.getReturnData(dataList);
    }
    public DataResponse scoreSave(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        Integer courseId = dataRequest.getInteger("courseId");
        String rawMark = dataRequest.getString("mark");
        Integer scoreId = dataRequest.getInteger("scoreId");
        Score s = null;
        if (scoreId != null) {
            s = requireScore(scoreId);
        }
        if (s != null) {
            assertStudentAccessible(getStudentId(s), "教师仅可维护本人授课学生的成绩");
            personId = getStudentId(s);
            courseId = s.getCourse() == null ? courseId : s.getCourse().getCourseId();
        } else {
            personId = resolveRequiredStudentId(personId, ErrorCodes.SCORE_STUDENT_REQUIRED, "添加失败，学生不能为空", "教师仅可维护本人授课学生的成绩");
        }
        BigDecimal originalMark = s == null ? null : s.getMark();
        BigDecimal mark = ScoreMarkValidator.parseOrNull(rawMark);
        if (mark == null) {
            Integer userId = CommonMethod.getPersonId();
            log.warn("scoreSave invalid mark userId={} scoreId={} originalMark={} illegalValue={} time={}",
                    userId, scoreId, originalMark, rawMark, new Date());
            throw new BusinessException(ErrorCodes.SCORE_MARK_INVALID, ScoreMarkValidator.INVALID_MSG);
        }
        if (courseId == null) {
            throw new BusinessException(ErrorCodes.SCORE_COURSE_REQUIRED, "添加失败，课程不能为空");
        }
        Student student = requireStudent(personId);
        Course course = requireCourse(courseId);
        List<CourseSelection> selections = courseSelectionRepository.findByStudentPersonIdAndCourseCourseId(personId, courseId);
        boolean selected = false;
        for (CourseSelection cs : selections) {
            if (!"已退课".equals(cs.getStatus())) {
                selected = true;
                break;
            }
        }
        if (!selected) {
            throw new BusinessException(ErrorCodes.SCORE_NOT_SELECTED, "添加失败，该学生未选择相应课程");
        }
        if (s == null) {
            s = new Score();
            s.setStudent(student);
            s.setCourse(course);
        }
        s.setMark(mark);
        scoreRepository.save(s);
        return CommonMethod.getReturnMessageOK();
    }
    public DataResponse scoreDelete(DataRequest dataRequest) {
        Integer scoreId = dataRequest.getInteger("scoreId");
        if (scoreId == null || scoreId <= 0) {
            throw new BusinessException(ErrorCodes.SCORE_ID_REQUIRED, "成绩记录ID不能为空");
        }
        Score s = requireScore(scoreId);
        assertStudentAccessible(getStudentId(s), "教师仅可删除本人授课学生的成绩");
        scoreRepository.delete(s);
        return CommonMethod.getReturnMessageOK();
    }

    private Integer resolveStudentIdForQuery(Integer studentId, String teacherMessage) {
        Integer currentStudentId = getCurrentStudentIdOrNull();
        if (currentStudentId != null) {
            return currentStudentId;
        }
        if (studentId != null && studentId > 0) {
            teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, teacherMessage);
        }
        return studentId;
    }

    private Integer resolveRequiredStudentId(Integer studentId, String errorCode, String emptyMessage, String teacherMessage) {
        Integer resolvedStudentId = resolveStudentIdForQuery(studentId, teacherMessage);
        if (resolvedStudentId == null || resolvedStudentId <= 0) {
            throw new BusinessException(errorCode, emptyMessage);
        }
        return resolvedStudentId;
    }

    private void assertStudentAccessible(Integer studentId, String teacherMessage) {
        Integer currentStudentId = getCurrentStudentIdOrNull();
        if (currentStudentId != null && !currentStudentId.equals(studentId)) {
            throw new BusinessException(ErrorCodes.ACCESS_DENIED, "学生只能查看自己的成绩");
        }
        teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, teacherMessage);
    }

    private Integer getCurrentStudentIdOrNull() {
        if (!"ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            return null;
        }
        Integer currentPersonId = CommonMethod.getPersonId();
        if (currentPersonId == null || currentPersonId <= 0) {
            throw new BusinessException(ErrorCodes.AUTH_FAILED, "当前登录状态无效，请重新登录");
        }
        return currentPersonId;
    }

    private Integer getStudentId(Score score) {
        return score == null || score.getStudent() == null ? null : score.getStudent().getPersonId();
    }

    private Set<Integer> getTeacherStudentIds() {
        if (!teacherDataScopeService.isCurrentRoleTeacher()) {
            return Collections.emptySet();
        }
        return teacherDataScopeService.getCurrentTeacherStudentIds();
    }

    private List<Score> filterScoresForTeacher(List<Score> scoreList) {
        if (!teacherDataScopeService.isCurrentRoleTeacher()) {
            return scoreList;
        }
        Set<Integer> studentIds = getTeacherStudentIds();
        return scoreList.stream()
                .filter(score -> studentIds.contains(getStudentId(score)))
                .toList();
    }

    private Score requireScore(Integer scoreId) {
        return scoreRepository.findById(scoreId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.SCORE_NOT_FOUND, "成绩记录不存在"));
    }

    private Student requireStudent(Integer studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.SCORE_STUDENT_NOT_FOUND, "添加失败，学生不存在"));
    }

    private Course requireCourse(Integer courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.SCORE_COURSE_NOT_FOUND, "添加失败，课程不存在"));
    }
}
