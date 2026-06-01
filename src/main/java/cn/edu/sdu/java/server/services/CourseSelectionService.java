package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.CourseSelection;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseSelectionRepository;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * CourseSelection 选课服务类
 */
@Service
public class CourseSelectionService {
    private static final Logger log = LoggerFactory.getLogger(CourseSelectionService.class);

    private final CourseSelectionRepository courseSelectionRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final TeacherDataScopeService teacherDataScopeService;

    public CourseSelectionService(CourseSelectionRepository courseSelectionRepository, 
                                  CourseRepository courseRepository, 
                                  StudentRepository studentRepository,
                                  TeacherDataScopeService teacherDataScopeService) {
        this.courseSelectionRepository = courseSelectionRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.teacherDataScopeService = teacherDataScopeService;
    }

    /**
     * 获取选课列表
     */
    public DataResponse getCourseSelectionList(DataRequest dataRequest) {
        Integer studentId = dataRequest.getInteger("studentId");
        studentId = resolveStudentIdForQuery(studentId, "教师仅可查看本人授课学生的选课记录");
        Integer courseId = dataRequest.getInteger("courseId");
        String status = dataRequest.getString("status");
        List<CourseSelection> selectionList;

        if (studentId != null && courseId != null) {
            selectionList = courseSelectionRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId);
        } else if (studentId != null) {
            if (status != null) {
                selectionList = courseSelectionRepository.findByStudentPersonIdAndStatus(studentId, status);
            } else {
                selectionList = courseSelectionRepository.findByStudentPersonId(studentId);
            }
        } else if (courseId != null) {
            if (status != null) {
                selectionList = courseSelectionRepository.findByCourseCourseIdAndStatus(courseId, status);
            } else {
                selectionList = courseSelectionRepository.findByCourseCourseId(courseId);
            }
        } else {
            selectionList = courseSelectionRepository.findAll();
        }
        selectionList = filterSelectionsForTeacher(selectionList);

        List<Map<String, Object>> list = new ArrayList<>();
        for (CourseSelection cs : selectionList) {
            Map<String, Object> m = new HashMap<>();
            m.put("selectionId", cs.getSelectionId());
            m.put("studentId", cs.getStudent().getPersonId());
            m.put("studentName", cs.getStudent().getPerson().getName());
            m.put("courseId", cs.getCourse().getCourseId());
            m.put("courseName", cs.getCourse().getName());
            m.put("status", cs.getStatus());
            m.put("statusName", getStatusName(cs.getStatus()));
            m.put("score", cs.getScore());
            m.put("ranking", cs.getRanking());
            m.put("selectionTime", cs.getSelectionTime());
            m.put("remark", cs.getRemark());
            list.add(m);
        }

        return CommonMethod.getReturnData(list);
    }

    /**
     * 学生选课
     */
    public DataResponse selectCourse(DataRequest dataRequest) {
        Integer studentId = dataRequest.getInteger("studentId");
        studentId = resolveRequiredStudentId(studentId, ErrorCodes.COURSE_SELECTION_STUDENT_REQUIRED, "学生ID不能为空", "教师仅可维护本人授课学生的选课记录");
        Integer courseId = dataRequest.getInteger("courseId");
        if (courseId == null) {
            throw new BusinessException(ErrorCodes.COURSE_SELECTION_COURSE_REQUIRED, "课程ID不能为空");
        }

        Student student = requireStudent(studentId);
        Course course = requireCourse(courseId);

        List<CourseSelection> existSelections = courseSelectionRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId);
        if (!existSelections.isEmpty()) {
            CourseSelection existSelection = existSelections.get(0);
            if ("已退课".equals(existSelection.getStatus())) {
                existSelection.setStatus("已选");
                courseSelectionRepository.save(existSelection);
                return CommonMethod.getReturnMessageOK("重新选课成功");
            }
            throw new BusinessException(ErrorCodes.COURSE_SELECTION_ALREADY_EXISTS, "该课程已选，不能重复选课");
        }

        CourseSelection selection = new CourseSelection();
        selection.setStudent(student);
        selection.setCourse(course);
        selection.setStatus("已选");

        courseSelectionRepository.save(selection);

        return CommonMethod.getReturnMessageOK("选课成功");
    }

    /**
     * 退课
     */
    public DataResponse dropCourse(DataRequest dataRequest) {
        Integer studentId = dataRequest.getInteger("studentId");
        studentId = resolveRequiredStudentId(studentId, ErrorCodes.COURSE_SELECTION_STUDENT_REQUIRED, "学生ID不能为空", "教师仅可维护本人授课学生的选课记录");
        Integer courseId = dataRequest.getInteger("courseId");
        if (courseId == null) {
            throw new BusinessException(ErrorCodes.COURSE_SELECTION_COURSE_REQUIRED, "课程ID不能为空");
        }

        List<CourseSelection> selectionList = courseSelectionRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId);
        if (selectionList.isEmpty()) {
            throw new BusinessException(ErrorCodes.COURSE_SELECTION_NOT_FOUND, "未找到选课记录");
        }

        CourseSelection selection = selectionList.get(0);
        if ("已完成".equals(selection.getStatus())) {
            throw new BusinessException(ErrorCodes.COURSE_SELECTION_COMPLETED, "课程已完成，不能退课");
        }

        selection.setStatus("已退课");
        selection.setRemark("退课时间：" + LocalDateTime.now());

        courseSelectionRepository.save(selection);

        return CommonMethod.getReturnMessageOK("退课成功");
    }

    /**
     * 批量导入选课
     */
    public DataResponse batchSelectCourse(DataRequest dataRequest) {
        List<?> dataList = dataRequest.getList("data");
        if (dataList == null || dataList.isEmpty()) {
            throw new BusinessException(ErrorCodes.COURSE_SELECTION_DATA_EMPTY, "数据为空");
        }

        int successCount = 0;
        for (Object obj : dataList) {
            try {
                Map<String, Object> data = (Map<String, Object>) obj;
                Integer studentId = (Integer) data.get("studentId");
                studentId = resolveRequiredStudentId(studentId, ErrorCodes.COURSE_SELECTION_STUDENT_REQUIRED, "学生ID不能为空", "教师仅可批量维护本人授课学生的选课记录");
                Integer courseId = (Integer) data.get("courseId");
                if (courseId == null) {
                    throw new BusinessException(ErrorCodes.COURSE_SELECTION_COURSE_REQUIRED, "课程ID不能为空");
                }

                List<CourseSelection> existSelections = courseSelectionRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId);
                if (!existSelections.isEmpty()) {
                    continue;
                }

                Student student = requireStudent(studentId);
                Course course = requireCourse(courseId);

                CourseSelection selection = new CourseSelection();
                selection.setStudent(student);
                selection.setCourse(course);
                selection.setStatus("已选");

                courseSelectionRepository.save(selection);
                successCount++;
            } catch (Exception e) {
                log.warn("导入单条选课记录失败：{}", e.getMessage());
            }
        }

        return CommonMethod.getReturnMessageOK("批量导入成功，成功 " + successCount + " 条");
    }

    /**
     * 删除选课记录
     */
    public DataResponse courseSelectionDelete(DataRequest dataRequest) {
        Integer selectionId = dataRequest.getInteger("selectionId");
        if (selectionId == null || selectionId <= 0) {
            throw new BusinessException(ErrorCodes.COURSE_SELECTION_ID_REQUIRED, "选课记录ID不能为空");
        }
        CourseSelection selection = requireSelection(selectionId);
        assertStudentAccessible(getStudentId(selection), "教师仅可删除本人授课学生的选课记录");
        courseSelectionRepository.deleteById(selectionId);
        return CommonMethod.getReturnMessageOK("删除成功");
    }

    /**
     * 获取选课统计
     * 按照对接规范返回：{"total": 50, "已选": 45, "已退课": 3, "已完成": 2}
     */
    public DataResponse getCourseSelectionStatistics(DataRequest dataRequest) {
        Integer studentId = dataRequest.getInteger("studentId");
        studentId = resolveStudentIdForQuery(studentId, "教师仅可查看本人授课学生的选课统计");
        Integer courseId = dataRequest.getInteger("courseId");
        if (courseId == null) {
            throw new BusinessException(ErrorCodes.COURSE_SELECTION_COURSE_REQUIRED, "请提供courseId参数");
        }

        requireCourse(courseId);

        List<CourseSelection> courseSelections = courseSelectionRepository.findByCourseCourseId(courseId);
        if (studentId != null && studentId > 0) {
            final Integer targetStudentId = studentId;
            courseSelections = courseSelections.stream()
                    .filter(selection -> targetStudentId.equals(getStudentId(selection)))
                    .toList();
        } else {
            courseSelections = filterSelectionsForTeacher(courseSelections);
        }
        List<CourseSelection> selectedList = courseSelections.stream()
                .filter(selection -> "已选".equals(selection.getStatus()))
                .toList();
        List<CourseSelection> completedList = courseSelections.stream()
                .filter(selection -> "已完成".equals(selection.getStatus()))
                .toList();
        List<CourseSelection> droppedList = courseSelections.stream()
                .filter(selection -> "已退课".equals(selection.getStatus()))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("total", courseSelections.size());
        result.put("已选", selectedList.size());
        result.put("已退课", droppedList.size());
        result.put("已完成", completedList.size());

        return CommonMethod.getReturnData(result);
    }

    /**
     * 状态值转换
     */
    private String getStatusName(String status) {
        if (status == null) return "";
        return switch (status) {
            case "已选" -> "已选";
            case "已退课" -> "已退课";
            case "已完成" -> "已完成";
            default -> status;
        };
    }

    private Integer resolveStudentIdForQuery(Integer studentId, String teacherMessage) {
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            Integer currentPersonId = CommonMethod.getPersonId();
            if (currentPersonId == null || currentPersonId <= 0) {
                throw new BusinessException(ErrorCodes.AUTH_FAILED, "当前登录状态无效，请重新登录");
            }
            return currentPersonId;
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
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            Integer currentPersonId = CommonMethod.getPersonId();
            if (currentPersonId == null || currentPersonId <= 0) {
                throw new BusinessException(ErrorCodes.AUTH_FAILED, "当前登录状态无效，请重新登录");
            }
            if (!currentPersonId.equals(studentId)) {
                throw new BusinessException(ErrorCodes.ACCESS_DENIED, "学生只能操作自己的选课数据");
            }
        }
        teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, teacherMessage);
    }

    private Integer getStudentId(CourseSelection selection) {
        return selection == null || selection.getStudent() == null ? null : selection.getStudent().getPersonId();
    }

    private List<CourseSelection> filterSelectionsForTeacher(List<CourseSelection> selectionList) {
        if (!teacherDataScopeService.isCurrentRoleTeacher()) {
            return selectionList;
        }
        Set<Integer> studentIds = teacherDataScopeService.getCurrentTeacherStudentIds();
        return selectionList.stream()
                .filter(selection -> studentIds.contains(getStudentId(selection)))
                .toList();
    }

    private CourseSelection requireSelection(Integer selectionId) {
        return courseSelectionRepository.findById(selectionId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.COURSE_SELECTION_NOT_FOUND, "选课记录不存在"));
    }

    private Student requireStudent(Integer studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.COURSE_SELECTION_STUDENT_NOT_FOUND, "学生不存在"));
    }

    private Course requireCourse(Integer courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.COURSE_SELECTION_COURSE_NOT_FOUND, "课程不存在"));
    }
}
