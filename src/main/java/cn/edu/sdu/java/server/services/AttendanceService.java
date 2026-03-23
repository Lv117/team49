package cn.edu.sdu.java.server.services;

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

    public AttendanceService(AttendanceRepository attendanceRepository, 
                            StudentRepository studentRepository, 
                            CourseRepository courseRepository) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * 获取考勤列表
     */
    public DataResponse getAttendanceList(DataRequest dataRequest) {
        try {
            Integer studentId = dataRequest.getInteger("studentId");
            Integer courseId = dataRequest.getInteger("courseId");
            String startDate = dataRequest.getString("startDate");
            String endDate = dataRequest.getString("endDate");

            List<Attendance> attendanceList;

            if (studentId != null && courseId != null) {
                attendanceList = attendanceRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId);
            } else if (studentId != null) {
                if (startDate != null && endDate != null) {
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    attendanceList = attendanceRepository.findByStudentPersonIdAndAttendanceDateBetween(studentId, start, end);
                } else {
                    attendanceList = attendanceRepository.findByStudentPersonId(studentId);
                }
            } else if (courseId != null) {
                attendanceList = attendanceRepository.findByCourseCourseId(courseId);
            } else {
                attendanceList = attendanceRepository.findAll();
            }

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
        } catch (Exception e) {
            log.error("获取考勤列表失败", e);
            return CommonMethod.getReturnMessageError("获取考勤列表失败：" + e.getMessage());
        }
    }

    /**
     * 保存考勤记录
     */
    public DataResponse attendanceSave(DataRequest dataRequest) {
        try {
            Integer attendanceId = dataRequest.getInteger("attendanceId");
            Integer studentId = dataRequest.getInteger("studentId");
            Integer courseId = dataRequest.getInteger("courseId");
            String attendanceDate = dataRequest.getString("attendanceDate");
            String status = dataRequest.getString("status");
            String type = dataRequest.getString("type");
            String remark = dataRequest.getString("remark");

            Attendance attendance;
            if (attendanceId != null) {
                attendance = attendanceRepository.findById(attendanceId).orElse(null);
                if (attendance == null) {
                    return CommonMethod.getReturnMessageError("考勤记录不存在");
                }
            } else {
                attendance = new Attendance();
            }

            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                return CommonMethod.getReturnMessageError("学生不存在");
            }

            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                return CommonMethod.getReturnMessageError("课程不存在");
            }

            attendance.setStudent(student);
            attendance.setCourse(course);
            attendance.setAttendanceDate(LocalDate.parse(attendanceDate));
            attendance.setStatus(status);
            attendance.setType(type);
            attendance.setRemark(remark);

            attendanceRepository.save(attendance);

            return CommonMethod.getReturnMessageOK("保存成功");
        } catch (Exception e) {
            log.error("保存考勤记录失败", e);
            return CommonMethod.getReturnMessageError("保存考勤记录失败：" + e.getMessage());
        }
    }

    /**
     * 批量保存考勤记录
     */
    public DataResponse attendanceBatchSave(DataRequest dataRequest) {
        try {
            List<?> dataList = dataRequest.getList("data");
            if (dataList == null || dataList.isEmpty()) {
                return CommonMethod.getReturnMessageError("数据为空");
            }

            int successCount = 0;
            for (Object obj : dataList) {
                try {
                    Map<String, Object> data = (Map<String, Object>) obj;
                    Integer studentId = (Integer) data.get("studentId");
                    Integer courseId = (Integer) data.get("courseId");
                    String attendanceDate = (String) data.get("attendanceDate");
                    String status = (String) data.get("status");
                    String type = (String) data.get("type");
                    String remark = (String) data.get("remark");

                    Attendance attendance = new Attendance();
                    Student student = studentRepository.findById(studentId).orElse(null);
                    if (student == null) continue;

                    Course course = courseRepository.findById(courseId).orElse(null);
                    if (course == null) continue;

                    attendance.setStudent(student);
                    attendance.setCourse(course);
                    attendance.setAttendanceDate(LocalDate.parse(attendanceDate));
                    attendance.setStatus(status);
                    attendance.setType(type);
                    attendance.setRemark(remark);

                    attendanceRepository.save(attendance);
                    successCount++;
                } catch (Exception e) {
                    log.warn("保存单条考勤记录失败：" + e.getMessage());
                }
            }

            return CommonMethod.getReturnMessageOK("批量保存成功，成功 " + successCount + " 条");
        } catch (Exception e) {
            log.error("批量保存考勤记录失败", e);
            return CommonMethod.getReturnMessageError("批量保存失败：" + e.getMessage());
        }
    }

    /**
     * 删除考勤记录
     */
    public DataResponse attendanceDelete(DataRequest dataRequest) {
        try {
            Integer attendanceId = dataRequest.getInteger("attendanceId");
            if (attendanceId == null) {
                return CommonMethod.getReturnMessageError("考勤记录 ID 不能为空");
            }

            if (!attendanceRepository.existsById(attendanceId)) {
                return CommonMethod.getReturnMessageError("考勤记录不存在");
            }

            attendanceRepository.deleteById(attendanceId);

            return CommonMethod.getReturnMessageOK("删除成功");
        } catch (Exception e) {
            log.error("删除考勤记录失败", e);
            return CommonMethod.getReturnMessageError("删除考勤记录失败：" + e.getMessage());
        }
    }

    /**
     * 获取考勤统计
     */
    public DataResponse getAttendanceStatistics(DataRequest dataRequest) {
        try {
            Integer studentId = dataRequest.getInteger("studentId");
            Integer courseId = dataRequest.getInteger("courseId");

            Map<String, Object> result = new HashMap<>();
            
            if (studentId != null) {
                List<Object[]> stats = attendanceRepository.countByStatus(studentId);
                Map<String, Long> statMap = new HashMap<>();
                for (Object[] stat : stats) {
                    statMap.put((String) stat[0], (Long) stat[1]);
                }
                result.put("byStudent", statMap);
            }

            if (courseId != null) {
                List<Object[]> stats = attendanceRepository.countByStatusByCourse(courseId);
                Map<String, Long> statMap = new HashMap<>();
                for (Object[] stat : stats) {
                    statMap.put((String) stat[0], (Long) stat[1]);
                }
                result.put("byCourse", statMap);
            }

            return CommonMethod.getReturnData(result);
        } catch (Exception e) {
            log.error("获取考勤统计失败", e);
            return CommonMethod.getReturnMessageError("获取考勤统计失败：" + e.getMessage());
        }
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
}
