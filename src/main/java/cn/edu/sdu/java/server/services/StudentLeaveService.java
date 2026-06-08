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
        String studentNum = dataRequest.getString("studentNum");
        if(studentNum == null)
            studentNum = "";
        Integer teacherId = dataRequest.getInteger("teacherId");
        
        // 调试日志
        System.out.println("[DEBUG] ========== 请假查询开始 ==========");
        System.out.println("[DEBUG] 请假查询参数 - 角色: " + roleName + ", 用户名: " + userName 
            + ", state: " + state + ", search: " + search 
            + ", studentNum: " + studentNum + ", teacherId: " + teacherId);
        
        // 测试：先查全部记录
        List<StudentLeave> allLeaves = studentLeaveRepository.findAll();
        System.out.println("[DEBUG] 数据库中总共有 " + allLeaves.size() + " 条请假记录");
        if (!allLeaves.isEmpty()) {
            for (int i = 0; i < Math.min(3, allLeaves.size()); i++) {
                StudentLeave sl = allLeaves.get(i);
                System.out.println("[DEBUG] 记录" + (i+1) + " ID=" + sl.getStudentLeaveId() 
                    + ", studentId=" + (sl.getStudent() != null ? sl.getStudent().getPersonId() : "null")
                    + ", student.person.num=" + (sl.getStudent() != null && sl.getStudent().getPerson() != null ? sl.getStudent().getPerson().getNum() : "null")
                    + ", state=" + sl.getState());
            }
        }

        // 处理特殊请假进度：4=未返校(state=1且已逾期), 5=待返校(state=1且未逾期)
        Integer queryState = state;
        boolean filterOverdue = (state != null && state == 4);  // true=筛选已逾期, false=筛选未逾期
        boolean applyDateFilter = (state != null && (state == 4 || state == 5));
        if (state != null && (state == 4 || state == 5)) {
            queryState = 1;
        }

        List<StudentLeave> slList = switch (roleName) {
            // 学生角色：查询自己的记录，search参数用于前端搜索（但学生只看自己的，所以search传空）
            case "ROLE_STUDENT" -> studentLeaveRepository.getStudentLeaveList(-1, "", userName, "", null);
            // 教师角色：查询关联自己的记录，search参数用于匹配学生姓名或学号
            case "ROLE_TEACHER" -> studentLeaveRepository.getStudentLeaveList(-1, search, "", userName, null);
            // 管理员角色：search参数用于匹配学生姓名或学号，studentNum传空（不使用精确匹配）
            case "ROLE_ADMIN" -> {
                System.out.println("[DEBUG] 执行管理员查询 - queryState: " + queryState + ", search: " + search + ", teacherId: " + teacherId);
                // studentNum 传空字符串，让查询只使用 search 进行模糊匹配
                yield studentLeaveRepository.getStudentLeaveList(queryState, search, "", "", teacherId);
            }
            case null, default -> throw new BusinessException(ErrorCodes.LEAVE_ROLE_INVALID, "当前角色无法查询请假记录");
        };
        
        System.out.println("[DEBUG] 查询结果数量: " + (slList != null ? slList.size() : 0));
        if (slList != null && !slList.isEmpty()) {
            for (int i = 0; i < Math.min(3, slList.size()); i++) {
                StudentLeave sl = slList.get(i);
                System.out.println("[DEBUG] 记录" + (i+1) + " - 学生ID: " + (sl.getStudent() != null ? sl.getStudent().getPersonId() : "null") + ", 学号: " + (sl.getStudent() != null && sl.getStudent().getPerson() != null ? sl.getStudent().getPerson().getNum() : "null") + ", 姓名: " + (sl.getStudent() != null && sl.getStudent().getPerson() != null ? sl.getStudent().getPerson().getName() : "null"));
            }
        }

        // 对"未返校"和"待返校"进行日期过滤
        if (applyDateFilter && slList != null) {
            LocalDate today = LocalDate.now();
            slList = slList.stream().filter(sl -> {
                String leaveDate = sl.getLeaveDate();
                if (leaveDate == null || !leaveDate.contains("至")) return false;
                try {
                    String endDateStr = leaveDate.split("至")[1].trim();
                    LocalDate endDate = LocalDate.parse(endDateStr);
                    if (filterOverdue) {
                        // 未返校：结束日期已过
                        return today.isAfter(endDate);
                    } else {
                        // 待返校：结束日期未过（含当天）
                        return !today.isAfter(endDate);
                    }
                } catch (Exception e) {
                    return false;
                }
            }).toList();
        }

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
                map.put("adminComment", sl.getAdminComment());
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
    // 教师/管理员审批：通过→state=1，驳回→state=2。
    // 安全约定：仅接收 id + state + comment 三个字段，其余字段一概忽略。
    // 从数据库查出原记录后，只更新审批相关的字段，绝不触碰学生提交的原始数据。
    // 教师审批使用 teacherComment 和 teacherTime，管理员审批使用 adminComment 和 adminTime
    public DataResponse studentLeaveCheck(DataRequest dataRequest) {
        String roleName = CommonMethod.getRoleName();
        Integer state = dataRequest.getInteger("state");
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        String comment = dataRequest.getString("teacherComment");

        if(!"ROLE_TEACHER".equals(roleName) && !"ROLE_ADMIN".equals(roleName)) {
            throw new BusinessException(ErrorCodes.LEAVE_ROLE_INVALID, "仅教师和管理员可审批请假申请");
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
        
        // 根据角色分别记录审批信息
        if("ROLE_ADMIN".equals(roleName)) {
            // 管理员审批
            sl.setAdminComment(comment);
            sl.setAdminTime(new Date());
        } else {
            // 教师审批
            sl.setTeacherComment(comment);
            sl.setTeacherTime(new Date());
        }
        
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

        // Step 2: 审批阶段 — 优先显示管理员审批，如果没有则显示教师审批
        Map<String, Object> step2 = new HashMap<>();
        step2.put("step", 2);
        
        // 判断是管理员审批还是教师审批
        boolean hasAdminApproval = sl.getAdminTime() != null;
        boolean hasTeacherApproval = sl.getTeacherTime() != null;
        
        if (hasAdminApproval) {
            // 管理员审批
            step2.put("stage", "管理员审批");
            step2.put("operator", "管理员");
            if(sl.getState() != null && sl.getState() >= 1) {
                step2.put("timestamp", sdf.format(sl.getAdminTime()));
                String ac = sl.getAdminComment() != null && !sl.getAdminComment().isEmpty()
                        ? sl.getAdminComment() : "暂无意见";
                step2.put("comment", ac);
                step2.put("status", sl.getState() == 2 ? "rejected" : "completed");
            } else {
                step2.put("timestamp", "");
                step2.put("status", "pending");
                step2.put("comment", "待审批");
            }
        } else {
            // 教师审批
            step2.put("stage", "教师审批");
            step2.put("operator", getTeacherName(sl.getTeacher()));
            if(sl.getState() != null && sl.getState() >= 1 && hasTeacherApproval) {
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
     * 删除请假记录（管理员、教师可操作，学生只能删除自己的待审批记录）
     */
    public DataResponse deleteStudentLeave(DataRequest dataRequest) {
        String roleName = CommonMethod.getRoleName();
        String username = CommonMethod.getUsername();
        
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        if(studentLeaveId == null || studentLeaveId <= 0) {
            throw new BusinessException(ErrorCodes.LEAVE_RECORD_NOT_FOUND, "请假ID不能为空");
        }

        StudentLeave sl = studentLeaveRepository.findById(studentLeaveId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.LEAVE_RECORD_NOT_FOUND, "请假记录不存在"));

        // 学生角色：只能删除自己的待审批(state=0)记录
        if("ROLE_STUDENT".equals(roleName)) {
            // 验证是否是学生自己的记录
            if(sl.getStudent() == null || !username.equals(sl.getStudent().getPerson().getNum())) {
                throw new BusinessException(ErrorCodes.LEAVE_ROLE_INVALID, "只能删除自己的请假记录");
            }
            // 验证是否处于待审批状态
            if(sl.getState() == null || sl.getState() != 0) {
                throw new BusinessException(ErrorCodes.LEAVE_STATUS_INVALID, "只能删除待审批状态的请假记录");
            }
        } 
        // 教师和管理员可以删除任何记录
        else if(!"ROLE_ADMIN".equals(roleName) && !"ROLE_TEACHER".equals(roleName)) {
            throw new BusinessException(ErrorCodes.LEAVE_ROLE_INVALID, "无权删除请假记录");
        }

        studentLeaveRepository.delete(sl);
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 学生申请销假（替代原来的直接销假）
     * 流程：学生申请销假 → state变为4(待销假审批) → 管理员/教师审批 → 通过则state=3(已返校)
     */
    public DataResponse applyStudentReturn(DataRequest dataRequest) {
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        if(studentLeaveId == null) {
            throw new BusinessException(ErrorCodes.LEAVE_DATA_INCOMPLETE, "请假记录ID不能为空");
        }
        
        StudentLeave sl = studentLeaveRepository.findById(studentLeaveId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.LEAVE_RECORD_NOT_FOUND, "请假记录不存在"));
        
        // 验证状态：只有请假中(state=1)才能申请销假
        if(sl.getState() == null || sl.getState() != 1) {
            throw new BusinessException(ErrorCodes.LEAVE_STATUS_INVALID, "只有请假中的记录才能申请销假");
        }
        
        // 更新状态为待销假审批
        // 注：不再拦截逾期申请，允许逾期后申请销假，最终显示为"已归"或"晚归"
        sl.setState(4);
        sl.setReturnTime(new Date()); // 记录申请时间
        studentLeaveRepository.save(sl);
        
        return CommonMethod.getReturnMessageOK("销假申请已提交，等待审批");
    }
    
    /**
     * 管理员/教师审批销假申请
     */
    public DataResponse studentReturnCheck(DataRequest dataRequest) {
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        Integer state = dataRequest.getInteger("state"); // 1=同意，2=驳回
        String comment = dataRequest.getString("comment");
        
        if(studentLeaveId == null) {
            throw new BusinessException(ErrorCodes.LEAVE_DATA_INCOMPLETE, "请假记录ID不能为空");
        }
        if(state == null || (state != 1 && state != 2)) {
            throw new BusinessException(ErrorCodes.LEAVE_DATA_INCOMPLETE, "审批状态必须为1(同意)或2(驳回)");
        }
        
        StudentLeave sl = studentLeaveRepository.findById(studentLeaveId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.LEAVE_RECORD_NOT_FOUND, "请假记录不存在"));
        
        // 验证状态：只有待销假审批(state=4)才能审批
        if(sl.getState() == null || sl.getState() != 4) {
            throw new BusinessException(ErrorCodes.LEAVE_STATUS_INVALID, "该记录不在待销假审批状态");
        }
        
        String username = CommonMethod.getUsername();
        String role = CommonMethod.getRoleName();
        
        if (state == 1) {
            // 同意销假
            sl.setState(3); // 已返校
            sl.setReturnTime(new Date()); // 更新为实际返校时间
            if ("ROLE_ADMIN".equals(role)) {
                sl.setAdminComment(comment);
            } else if ("ROLE_TEACHER".equals(role)) {
                sl.setTeacherComment(comment);
            }
        } else {
            // 驳回销假申请，退回请假中状态
            sl.setState(1); // 退回请假中
            sl.setReturnTime(null); // 清空申请时间
            if ("ROLE_ADMIN".equals(role)) {
                sl.setAdminComment(comment);
            } else if ("ROLE_TEACHER".equals(role)) {
                sl.setTeacherComment(comment);
            }
        }
        
        studentLeaveRepository.save(sl);
        
        String msg = (state == 1) ? "销假审批通过" : "销假申请已驳回";
        return CommonMethod.getReturnMessageOK(msg);
    }
    
    /**
     * 同步请假记录到考勤系统
     */
    public DataResponse syncLeaveWithAttendance(DataRequest dataRequest) {
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        if(studentLeaveId == null || studentLeaveId <= 0) {
            throw new BusinessException(ErrorCodes.LEAVE_DATA_INCOMPLETE, "请假ID不能为空");
        }
        
        StudentLeave sl = studentLeaveRepository.findById(studentLeaveId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.LEAVE_RECORD_NOT_FOUND, "请假记录不存在"));
        
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
