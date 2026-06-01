package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.Attendance;
import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.AttendanceRepository;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Attendance 考勤服务类
 */
@Service
public class AttendanceService {
    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final TeacherDataScopeService teacherDataScopeService;

    public AttendanceService(AttendanceRepository attendanceRepository, 
                            StudentRepository studentRepository, 
                            CourseRepository courseRepository,
                            TeacherDataScopeService teacherDataScopeService) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.teacherDataScopeService = teacherDataScopeService;
    }

    /**
     * 获取考勤列表
     */
    public DataResponse getAttendanceList(DataRequest dataRequest) {
        Integer studentId = dataRequest.getInteger("studentId");
        studentId = resolveStudentIdForQuery(studentId, "教师仅可查看本人授课学生的考勤记录");
        Integer courseId = dataRequest.getInteger("courseId");
        String startDate = dataRequest.getString("startDate");
        String endDate = dataRequest.getString("endDate");

        List<Attendance> attendanceList;

        if (studentId != null && courseId != null) {
            attendanceList = attendanceRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId);
        } else if (studentId != null) {
            if (startDate != null && endDate != null) {
                LocalDate start = parseAttendanceDate(startDate);
                LocalDate end = parseAttendanceDate(endDate);
                attendanceList = attendanceRepository.findByStudentPersonIdAndAttendanceDateBetween(studentId, start, end);
            } else {
                attendanceList = attendanceRepository.findByStudentPersonId(studentId);
            }
        } else if (courseId != null) {
            attendanceList = attendanceRepository.findByCourseCourseId(courseId);
        } else {
            attendanceList = attendanceRepository.findAll();
        }
        attendanceList = filterAttendancesForTeacher(attendanceList);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Attendance a : attendanceList) {
            Map<String, Object> m = new HashMap<>();
            m.put("attendanceId", a.getAttendanceId());
            m.put("studentId", a.getStudent().getPersonId());
            m.put("studentName", a.getStudent().getPerson().getName());
            m.put("courseId", a.getCourse().getCourseId());
            m.put("courseName", a.getCourse().getName());
            m.put("attendanceDate", a.getAttendanceDate());
            m.put("status", a.getStatus());
            m.put("statusName", getStatusName(a.getStatus()));
            m.put("type", a.getType());
            m.put("typeName", getTypeName(a.getType()));
            m.put("remark", a.getRemark());
            list.add(m);
        }

        return CommonMethod.getReturnData(list);
    }

    /**
     * 保存考勤记录
     */
    public DataResponse attendanceSave(DataRequest dataRequest) {
        Integer attendanceId = dataRequest.getInteger("attendanceId");
        Integer studentId = dataRequest.getInteger("studentId");
        studentId = resolveRequiredStudentId(studentId, ErrorCodes.ATTENDANCE_STUDENT_REQUIRED, "学生ID不能为空", "教师仅可维护本人授课学生的考勤记录");
        Integer courseId = dataRequest.getInteger("courseId");
        String attendanceDate = dataRequest.getString("attendanceDate");
        String status = dataRequest.getString("status");
        String type = dataRequest.getString("type");
        String remark = dataRequest.getString("remark");

        if (courseId == null) {
            throw new BusinessException(ErrorCodes.ATTENDANCE_COURSE_REQUIRED, "课程ID不能为空");
        }
        if (attendanceDate == null || attendanceDate.isEmpty()) {
            throw new BusinessException(ErrorCodes.ATTENDANCE_DATE_REQUIRED, "考勤日期不能为空");
        }
        if (status == null || status.isEmpty()) {
            throw new BusinessException(ErrorCodes.ATTENDANCE_STATUS_REQUIRED, "考勤状态不能为空");
        }
        validateAttendanceStatus(status);
        validateAttendanceType(type);

        Attendance attendance;
        if (attendanceId != null) {
            attendance = requireAttendance(attendanceId);
            assertStudentAccessible(getStudentId(attendance), "教师仅可维护本人授课学生的考勤记录");
        } else {
            attendance = new Attendance();
        }

        Student student = requireStudent(studentId);
        Course course = requireCourse(courseId);

        attendance.setStudent(student);
        attendance.setCourse(course);
        attendance.setAttendanceDate(parseAttendanceDate(attendanceDate));
        attendance.setStatus(status);
        attendance.setType(type);
        attendance.setRemark(remark);

        attendanceRepository.save(attendance);

        return CommonMethod.getReturnMessageOK("保存成功");
    }

    /**
     * 批量保存考勤记录
     */
    public DataResponse attendanceBatchSave(DataRequest dataRequest) {
        List<?> dataList = dataRequest.getList("data");
        if (dataList == null || dataList.isEmpty()) {
            throw new BusinessException(ErrorCodes.ATTENDANCE_DATA_EMPTY, "数据为空");
        }

        int successCount = 0;
        for (Object obj : dataList) {
            try {
                Map<String, Object> data = (Map<String, Object>) obj;
                Integer studentId = (Integer) data.get("studentId");
                studentId = resolveRequiredStudentId(studentId, ErrorCodes.ATTENDANCE_STUDENT_REQUIRED, "学生ID不能为空", "教师仅可批量维护本人授课学生的考勤记录");
                Integer courseId = (Integer) data.get("courseId");
                if (courseId == null) {
                    throw new BusinessException(ErrorCodes.ATTENDANCE_COURSE_REQUIRED, "课程ID不能为空");
                }
                String attendanceDate = (String) data.get("attendanceDate");
                if (attendanceDate == null || attendanceDate.isEmpty()) {
                    throw new BusinessException(ErrorCodes.ATTENDANCE_DATE_REQUIRED, "考勤日期不能为空");
                }
                String status = (String) data.get("status");
                if (status == null || status.isEmpty()) {
                    throw new BusinessException(ErrorCodes.ATTENDANCE_STATUS_REQUIRED, "考勤状态不能为空");
                }
                String type = (String) data.get("type");
                String remark = (String) data.get("remark");

                validateAttendanceStatus(status);
                validateAttendanceType(type);

                Attendance attendance = new Attendance();
                Student student = requireStudent(studentId);
                Course course = requireCourse(courseId);

                attendance.setStudent(student);
                attendance.setCourse(course);
                attendance.setAttendanceDate(parseAttendanceDate(attendanceDate));
                attendance.setStatus(status);
                attendance.setType(type);
                attendance.setRemark(remark);

                attendanceRepository.save(attendance);
                successCount++;
            } catch (Exception e) {
                log.warn("保存单条考勤记录失败：{}", e.getMessage());
            }
        }

        return CommonMethod.getReturnMessageOK("批量保存成功，成功 " + successCount + " 条");
    }

    /**
     * 删除考勤记录
     */
    public DataResponse attendanceDelete(DataRequest dataRequest) {
        Integer attendanceId = dataRequest.getInteger("attendanceId");
        if (attendanceId == null || attendanceId <= 0) {
            throw new BusinessException(ErrorCodes.ATTENDANCE_ID_REQUIRED, "考勤记录ID不能为空");
        }
        Attendance attendance = requireAttendance(attendanceId);
        assertStudentAccessible(getStudentId(attendance), "教师仅可删除本人授课学生的考勤记录");
        attendanceRepository.deleteById(attendanceId);
        return CommonMethod.getReturnMessageOK("删除成功");
    }

    /**
     * 获取考勤统计
     * 按照对接规范返回：{"total": 100, "出勤": 85, "缺勤": 10, "迟到": 3, "早退": 2}
     */
    public DataResponse getAttendanceStatistics(DataRequest dataRequest) {
        Integer studentId = dataRequest.getInteger("studentId");
        studentId = resolveStudentIdForQuery(studentId, "教师仅可查看本人授课学生的考勤统计");
        Integer courseId = dataRequest.getInteger("courseId");
        String startDate = dataRequest.getString("startDate");
        String endDate = dataRequest.getString("endDate");

        if (studentId == null && courseId == null) {
            throw new BusinessException(ErrorCodes.ATTENDANCE_QUERY_REQUIRED, "请提供studentId或courseId参数");
        }

        Map<String, Object> result = new HashMap<>();
        List<Attendance> attendanceList;

        if (studentId != null && courseId != null) {
            if (startDate != null && endDate != null) {
                LocalDate start = parseAttendanceDate(startDate);
                LocalDate end = parseAttendanceDate(endDate);
                attendanceList = attendanceRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId)
                    .stream()
                    .filter(a -> !a.getAttendanceDate().isBefore(start) && !a.getAttendanceDate().isAfter(end))
                    .toList();
            } else if (studentId != null) {
                attendanceList = attendanceRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId);
            } else {
                attendanceList = attendanceRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId);
            }
        } else if (studentId != null) {
            if (startDate != null && endDate != null) {
                LocalDate start = parseAttendanceDate(startDate);
                LocalDate end = parseAttendanceDate(endDate);
                attendanceList = attendanceRepository.findByStudentPersonIdAndAttendanceDateBetween(studentId, start, end);
            } else {
                attendanceList = attendanceRepository.findByStudentPersonId(studentId);
            }
        } else {
            if (startDate != null && endDate != null) {
                LocalDate start = parseAttendanceDate(startDate);
                LocalDate end = parseAttendanceDate(endDate);
                attendanceList = attendanceRepository.findByCourseCourseId(courseId)
                    .stream()
                    .filter(a -> !a.getAttendanceDate().isBefore(start) && !a.getAttendanceDate().isAfter(end))
                    .toList();
            } else {
                attendanceList = attendanceRepository.findByCourseCourseId(courseId);
            }
        }
        attendanceList = filterAttendancesForTeacher(attendanceList);

        long totalCount = attendanceList.size();
        long 出勤 = attendanceList.stream().filter(a -> "出席".equals(a.getStatus()) || "出勤".equals(a.getStatus())).count();
        long 缺勤 = attendanceList.stream().filter(a -> "缺勤".equals(a.getStatus())).count();
        long 迟到 = attendanceList.stream().filter(a -> "迟到".equals(a.getStatus())).count();
        long 早退 = attendanceList.stream().filter(a -> "早退".equals(a.getStatus())).count();

        result.put("total", totalCount);
        result.put("出勤", 出勤);
        result.put("缺勤", 缺勤);
        result.put("迟到", 迟到);
        result.put("早退", 早退);

        return CommonMethod.getReturnData(result);
    }

    /**
     * 状态值转换
     */
    private String getStatusName(String status) {
        if (status == null) return "";
        return switch (status) {
            case "出席" -> "出席";
            case "缺勤" -> "缺勤";
            case "迟到" -> "迟到";
            case "早退" -> "早退";
            default -> status;
        };
    }

    /**
     * 类型值转换
     */
    private String getTypeName(String type) {
        if (type == null) return "";
        return switch (type) {
            case "正常" -> "正常";
            case "病假" -> "病假";
            case "事假" -> "事假";
            case "旷课" -> "旷课";
            default -> type;
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
                throw new BusinessException(ErrorCodes.ACCESS_DENIED, "学生只能操作自己的考勤数据");
            }
        }
        teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, teacherMessage);
    }

    private Integer getStudentId(Attendance attendance) {
        return attendance == null || attendance.getStudent() == null ? null : attendance.getStudent().getPersonId();
    }

    private List<Attendance> filterAttendancesForTeacher(List<Attendance> attendanceList) {
        if (!teacherDataScopeService.isCurrentRoleTeacher()) {
            return attendanceList;
        }
        Set<Integer> studentIds = teacherDataScopeService.getCurrentTeacherStudentIds();
        return attendanceList.stream()
                .filter(attendance -> studentIds.contains(getStudentId(attendance)))
                .toList();
    }

    private void validateAttendanceStatus(String status) {
        if (!status.matches("^(出席|缺勤|迟到|早退)$")) {
            throw new BusinessException(ErrorCodes.ATTENDANCE_STATUS_INVALID, "考勤状态必须为：出席、缺勤、迟到、早退之一");
        }
    }

    private void validateAttendanceType(String type) {
        if (type != null && !type.isEmpty() && !type.matches("^(正常|病假|事假|旷课)$")) {
            throw new BusinessException(ErrorCodes.ATTENDANCE_TYPE_INVALID, "考勤类型必须为：正常、病假、事假、旷课之一");
        }
    }

    private LocalDate parseAttendanceDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodes.ATTENDANCE_DATE_INVALID, "日期格式错误，请使用 yyyy-MM-dd 格式");
        }
    }

    private Attendance requireAttendance(Integer attendanceId) {
        return attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ATTENDANCE_NOT_FOUND, "考勤记录不存在"));
    }

    private Student requireStudent(Integer studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "学生不存在"));
    }

    private Course requireCourse(Integer courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.COURSE_SELECTION_COURSE_NOT_FOUND, "课程不存在"));
    }
}
