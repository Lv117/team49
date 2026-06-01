package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.*;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.OptionItem;
import cn.edu.sdu.java.server.payload.response.OptionItemList;
import cn.edu.sdu.java.server.repositorys.*;
import cn.edu.sdu.java.server.util.ComDataUtil;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class StudentLeaveService {
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentLeaveRepository studentLeaveRepository;
    private final AttendanceRepository attendanceRepository;
    private final CourseRepository courseRepository;

    public StudentLeaveService(StudentRepository studentRepository, TeacherRepository teacherRepository, 
                             StudentLeaveRepository studentLeaveRepository, AttendanceRepository attendanceRepository,
                             CourseRepository courseRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.studentLeaveRepository = studentLeaveRepository;
        this.attendanceRepository = attendanceRepository;
        this.courseRepository = courseRepository;
    }

    public OptionItemList getTeacherItemOptionList(DataRequest dataRequest) {
        List<Teacher> sList = teacherRepository.findAll();
        List<OptionItem> itemList = new ArrayList<>();
        for (Teacher t : sList) {
            Person person = t.getPerson();
            if (person == null) {
                continue;
            }
            itemList.add(new OptionItem(t.getPersonId(), t.getPersonId() + "", safePersonDisplay(person)));
        }
        return new OptionItemList(0, itemList);
    }

    public DataResponse getStudentLeaveList(DataRequest dataRequest) {
        String roleName = CommonMethod.getRoleName();
        String userName = CommonMethod.getUsername();
        Integer state = dataRequest.getInteger("state");
        if(state == null)
            state = -1;
        String search = dataRequest.getString("search");
        if(search == null)
            search = "";
        List<StudentLeave> slList = switch (roleName) {
            case "ROLE_STUDENT" -> studentLeaveRepository.getStudentLeaveList(-1, search, userName, "");
            case "ROLE_TEACHER" -> studentLeaveRepository.getStudentLeaveList(-1, search, "", userName);
            case "ROLE_ADMIN" -> studentLeaveRepository.getStudentLeaveList(state, search, "", "");
            case null, default -> throw new BusinessException(ErrorCodes.LEAVE_ROLE_INVALID, "当前角色无法查询请假记录");
        };
        List<Map<String, Object>> dataList = new ArrayList<>();
        Map<String, Object> map;
        Student s;
        Teacher t;
        ComDataUtil di = ComDataUtil.getInstance();
        if (slList != null && !slList.isEmpty()) {
            for (StudentLeave sl : slList) {
                map = new HashMap<>();
                s = sl.getStudent();
                t = sl.getTeacher();
                map.put("studentLeaveId", sl.getStudentLeaveId());
                map.put("studentNum", getStudentNum(s));
                map.put("studentName", getStudentName(s));
                map.put("studentId", s != null ? s.getPersonId() : null);
                map.put("teacherName", getTeacherName(t));
                map.put("state", sl.getState());
                map.put("stateName", di.getDictionaryLabelByValue("SHZTM", sl.getState()+""));
                map.put("reason", sl.getReason());
                map.put("leaveDate", sl.getLeaveDate());
                map.put("teacherId", t != null ? t.getPersonId() : null);
                map.put("teacherComment", sl.getTeacherComment());
                map.put("returnTime", sl.getReturnTime());
                dataList.add(map);
            }
        }
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse studentLeaveSave(DataRequest dataRequest) {
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        Integer teacherId = dataRequest.getInteger("teacherId");
        String leaveDate = dataRequest.getString("leaveDate");
        String reason = dataRequest.getString("reason");

        if(teacherId == null || teacherId <= 0) {
            throw new BusinessException(ErrorCodes.LEAVE_DATA_INCOMPLETE, "请选择指导老师");
        }

        String username = CommonMethod.getUsername();
        if(username == null) {
            throw new BusinessException(ErrorCodes.LEAVE_DATA_INCOMPLETE, "无法获取当前用户信息，请重新登录");
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.LEAVE_DATA_INCOMPLETE, "所选指导老师不存在"));

        StudentLeave sl;

        if(studentLeaveId != null && studentLeaveId > 0) {
            sl = studentLeaveRepository.findById(studentLeaveId)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.LEAVE_RECORD_NOT_FOUND, "请假记录不存在"));

            // 状态拦截：只有待审批(0)和已驳回(2)才允许修改
            if(sl.getState() != null && sl.getState() == 1) {
                throw new BusinessException(ErrorCodes.LEAVE_STATUS_INVALID, "请假已审批通过，无法修改");
            }
            if(sl.getState() != null && sl.getState() == 3) {
                throw new BusinessException(ErrorCodes.LEAVE_STATUS_INVALID, "已返校销假，无法修改");
            }

            // 更新可变字段
            sl.setLeaveDate(leaveDate);
            sl.setReason(reason);

            // 指导老师变更：重置审批记录
            Teacher oldTeacher = sl.getTeacher();
            if(oldTeacher != null && !oldTeacher.getPersonId().equals(teacherId)) {
                sl.setTeacher(teacher);
                sl.setTeacherComment("");
                sl.setTeacherTime(null);
            }

            // 强制重置为待审批状态，清空历史审批
            sl.setState(0);
        } else {
            // 新增记录：忽略前端id，强制创建新记录
            sl = new StudentLeave();
            sl.setState(0);
            sl.setApplyTime(new Date());
            sl.setTeacherComment("");

            Student currentStudent = studentRepository.findByPersonNum(username)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.LEAVE_DATA_INCOMPLETE, "当前学生信息不存在，无法提交请假"));
            sl.setStudent(currentStudent);
            sl.setTeacher(teacher);
            sl.setLeaveDate(leaveDate);
            sl.setReason(reason);
        }

        studentLeaveRepository.save(sl);
        return CommonMethod.getReturnMessageOK();
    }
    // 教师审批：通过→state=1，驳回→state=2。
    // 安全约定：仅接收 id + state + teacherComment 三个字段，其余字段一概忽略。
    // 从数据库查出原记录后，只更新审批相关的三个字段，绝不触碰学生提交的原始数据。
    public DataResponse studentLeaveCheck(DataRequest dataRequest) {
        String roleName = CommonMethod.getRoleName();
        Integer state = dataRequest.getInteger("state");
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        String teacherComment = dataRequest.getString("teacherComment");

        if(!"ROLE_TEACHER".equals(roleName)) {
            throw new BusinessException(ErrorCodes.LEAVE_ROLE_INVALID, "仅教师可审批请假申请");
        }
        if(state == null || (state != 1 && state != 2)) {
            throw new BusinessException(ErrorCodes.LEAVE_STATUS_INVALID, "审批结果无效，仅允许通过(state=1)或驳回(state=2)");
        }
        if(studentLeaveId == null || studentLeaveId <= 0) {
            throw new BusinessException(ErrorCodes.LEAVE_RECORD_NOT_FOUND, "请假ID不能为空");
        }

        StudentLeave sl = studentLeaveRepository.findById(studentLeaveId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.LEAVE_RECORD_NOT_FOUND, "请假记录不存在"));

        if(sl.getState() == null || sl.getState() != 0) {
            throw new BusinessException(ErrorCodes.LEAVE_STATUS_INVALID, "仅待审批(state=0)的请假可进行审批操作");
        }

        // 仅更新审批相关字段，其余字段（学生信息、日期、理由等）从原记录保留
        sl.setState(state);
        sl.setTeacherComment(teacherComment);
        sl.setTeacherTime(new Date());
        studentLeaveRepository.save(sl);
        return CommonMethod.getReturnMessageOK();
    }

    // 学生返校报备：state 必须为 1（请假中），更新为 3（已返校）并记录返校时间
    public DataResponse studentReturn(DataRequest dataRequest) {
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        if(studentLeaveId == null || studentLeaveId <= 0) {
            throw new BusinessException(ErrorCodes.LEAVE_RECORD_NOT_FOUND, "请假ID不能为空");
        }
        StudentLeave sl = studentLeaveRepository.findById(studentLeaveId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.LEAVE_RECORD_NOT_FOUND, "请假记录不存在"));
        if(sl.getState() == null || sl.getState() != 1) {
            throw new BusinessException(ErrorCodes.LEAVE_STATUS_INVALID, "当前状态不允许销假，仅请假中(state=1)可报备返校");
        }
        sl.setState(3);
        sl.setReturnTime(new Date());
        studentLeaveRepository.save(sl);
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 获取请假进度时间线
     * 新流程：学生提交 → 教师审批 → 返校报备
     */
    public DataResponse getLeaveProgress(DataRequest dataRequest) {
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        if(studentLeaveId == null || studentLeaveId <= 0) {
            throw new BusinessException(ErrorCodes.LEAVE_RECORD_NOT_FOUND, "请假ID不能为空");
        }

        Optional<StudentLeave> op = studentLeaveRepository.findById(studentLeaveId);
        if(!op.isPresent()) {
            throw new BusinessException(ErrorCodes.LEAVE_RECORD_NOT_FOUND, "请假记录不存在");
        }

        StudentLeave sl = op.get();
        List<Map<String, Object>> progressList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Step 1: 学生提交申请 — comment = reason（请假理由）
        Map<String, Object> step1 = new HashMap<>();
        step1.put("step", 1);
        step1.put("stage", "学生提交申请");
        step1.put("operator", getStudentName(sl.getStudent()));
        step1.put("timestamp", sl.getApplyTime() != null ? sdf.format(sl.getApplyTime()) : "");
        step1.put("status", "completed");
        step1.put("comment", sl.getReason() != null ? sl.getReason() : "");
        progressList.add(step1);

        // Step 2: 教师审批 — comment 严格使用 teacherComment，绝不取 reason
        Map<String, Object> step2 = new HashMap<>();
        step2.put("step", 2);
        step2.put("stage", "教师审批");
        step2.put("operator", getTeacherName(sl.getTeacher()));
        if(sl.getState() != null && sl.getState() >= 1 && sl.getTeacherTime() != null) {
            step2.put("timestamp", sdf.format(sl.getTeacherTime()));
            String tc = sl.getTeacherComment() != null && !sl.getTeacherComment().isEmpty()
                    ? sl.getTeacherComment() : "暂无意见";
            step2.put("comment", tc);
            step2.put("status", sl.getState() == 2 ? "rejected" : "completed");
        } else {
            step2.put("timestamp", "");
            step2.put("status", "pending");
            step2.put("comment", "待审批");
        }
        progressList.add(step2);

        // Step 3: 返校报备
        Map<String, Object> step3 = new HashMap<>();
        step3.put("step", 3);
        step3.put("stage", "返校报备");
        step3.put("operator", getStudentName(sl.getStudent()));
        if(sl.getState() != null && sl.getState() == 3 && sl.getReturnTime() != null) {
            step3.put("timestamp", sdf.format(sl.getReturnTime()));
            step3.put("status", "completed");
            step3.put("comment", "学生已返校销假");
        } else if(sl.getState() != null && sl.getState() == 1) {
            step3.put("timestamp", "");
            step3.put("status", "pending");
            step3.put("comment", "待返校报备");
        } else {
            step3.put("timestamp", "");
            step3.put("status", "pending");
            step3.put("comment", "");
        }
        progressList.add(step3);

        String currentStatus;
        if(sl.getState() == null) {
            currentStatus = "未知";
        } else {
            currentStatus = switch (sl.getState()) {
                case 0 -> "待老师审批";
                case 1 -> "老师已批，请假中";
                case 2 -> "已驳回";
                case 3 -> "已返校";
                default -> "状态异常";
            };
        }

        Map<String, Object> result = new HashMap<>();
        result.put("studentLeaveId", studentLeaveId);
        result.put("currentStatus", currentStatus);
        result.put("progressTimeline", progressList);

        return CommonMethod.getReturnData(result);
    }

    /**
     * 请假审批通过后同步到考勤系统
     * 根据请假日期范围批量创建Attendance记录
     */
    public DataResponse syncLeaveWithAttendance(DataRequest dataRequest) {
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        if(studentLeaveId == null || studentLeaveId <= 0) {
            throw new BusinessException(ErrorCodes.LEAVE_RECORD_NOT_FOUND, "请假ID不能为空");
        }
        
        StudentLeave sl = studentLeaveRepository.findById(studentLeaveId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.LEAVE_RECORD_NOT_FOUND, "请假记录不存在"));

        if (sl.getLeaveDate() == null || sl.getLeaveDate().isBlank()) {
            throw new BusinessException(ErrorCodes.LEAVE_DATE_INVALID, "请假日期为空，无法同步到考勤系统");
        }
        if (sl.getStudent() == null) {
            throw new BusinessException(ErrorCodes.LEAVE_DATA_INCOMPLETE, "请假记录缺少学生信息，无法同步到考勤系统");
        }
        
        // 教师审批通过后即可同步到考勤
        if(sl.getState() == null || sl.getState() < 1) {
            throw new BusinessException(ErrorCodes.LEAVE_STATUS_INVALID, "请假未审批通过，无法同步到考勤系统");
        }
        
        try {
            // 解析请假日期范围
            String[] dateRange = sl.getLeaveDate().split("至");
            if(dateRange.length != 2) {
                throw new BusinessException(ErrorCodes.LEAVE_DATE_INVALID, "请假日期格式错误");
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date startDateUtil = sdf.parse(dateRange[0].trim());
            Date endDateUtil = sdf.parse(dateRange[1].trim());
            
            // 转换为LocalDate
            LocalDate startDate = startDateUtil.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate endDate = endDateUtil.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            
            // 获取该学生的所有课程
            List<Course> courseList = courseRepository.findAll();
            Student student = sl.getStudent();
            
            // 为请假期间的每一天创建考勤记录
            LocalDate currentDate = startDate;
            
            int syncCount = 0;
            while(!currentDate.isAfter(endDate)) {
                // 为该学生的每门课程创建"请假"状态的考勤记录
                for(Course course : courseList) {
                    // 检查是否已存在该日期的考勤记录
                    List<Attendance> existingAttendance = attendanceRepository.findAll();
                    boolean exists = false;
                    for(Attendance att : existingAttendance) {
                        if(att.getStudent().getPersonId().equals(student.getPersonId()) &&
                           att.getCourse().getCourseId().equals(course.getCourseId()) &&
                           att.getAttendanceDate().equals(currentDate)) {
                            exists = true;
                            break;
                        }
                    }
                    
                    if(!exists) {
                        Attendance attendance = new Attendance();
                        attendance.setStudent(student);
                        attendance.setCourse(course);
                        attendance.setAttendanceDate(currentDate);
                        attendance.setStatus("请假");
                        attendance.setRemark("根据请假记录自动生成");
                        attendanceRepository.save(attendance);
                        syncCount++;
                    }
                }
                
                currentDate = currentDate.plusDays(1);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("syncCount", syncCount);
            result.put("message", "已同步" + syncCount + "条考勤记录到系统");
            
            return CommonMethod.getReturnData(result);
        } catch(Exception e) {
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCodes.LEAVE_SYNC_FAILED, "同步考勤记录失败，请稍后重试", e);
        }
    }

    private String safePersonDisplay(Person person) {
        String num = person.getNum() != null ? person.getNum() : "";
        String name = person.getName() != null ? person.getName() : "";
        String label = num + "-" + name;
        return label.equals("-") ? "未命名教师" : label;
    }

    private String getStudentNum(Student student) {
        if (student == null || student.getPerson() == null) {
            return "";
        }
        return student.getPerson().getNum();
    }

    private String getStudentName(Student student) {
        if (student == null || student.getPerson() == null || student.getPerson().getName() == null) {
            return "未知学生";
        }
        return student.getPerson().getName();
    }

    private String getTeacherName(Teacher teacher) {
        if (teacher == null || teacher.getPerson() == null) {
            return "待分配教师";
        }
        return safePersonDisplay(teacher.getPerson());
    }
}
