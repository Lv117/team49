package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.Punishment;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.PunishmentRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PunishmentService {
    private final PunishmentRepository punishmentRepository;
    private final StudentRepository studentRepository;
    private final TeacherDataScopeService teacherDataScopeService;

    public PunishmentService(PunishmentRepository punishmentRepository,
                             StudentRepository studentRepository,
                             TeacherDataScopeService teacherDataScopeService) {
        this.punishmentRepository = punishmentRepository;
        this.studentRepository = studentRepository;
        this.teacherDataScopeService = teacherDataScopeService;
    }

    public DataResponse getPunishmentList(DataRequest dataRequest) {
        String numName = dataRequest.getString("numName");
        Integer studentId = dataRequest.getInteger("studentId");

        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            studentId = CommonMethod.getPersonId();
        }

        List<Punishment> list;

        if (numName != null && !numName.isEmpty()) {
            list = punishmentRepository.findAll().stream()
                    .filter(p -> (p.getStudentNum() != null && p.getStudentNum().contains(numName))
                            || (p.getStudentName() != null && p.getStudentName().contains(numName)))
                    .toList();
        } else if (studentId != null) {
            list = punishmentRepository.findByStudentId(studentId);
        } else {
            list = punishmentRepository.findAll();
        }

        if (teacherDataScopeService.isCurrentRoleTeacher()) {
            Set<Integer> allowedIds = teacherDataScopeService.getCurrentTeacherStudentIds();
            if (allowedIds.isEmpty()) {
                list = new ArrayList<>();
            } else {
                final List<Integer> allowed = new ArrayList<>(allowedIds);
                list = list.stream()
                        .filter(p -> p.getStudentId() != null && allowed.contains(p.getStudentId()))
                        .toList();
            }
        }

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Punishment p : list) {
            dataList.add(getMapFromPunishment(p));
        }
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse punishmentSave(DataRequest dataRequest) {
        Map<String, Object> form = dataRequest.getMap("form");
        if (form == null || form.isEmpty()) {
            throw new BusinessException(ErrorCodes.PUNISHMENT_FORM_INVALID, "表单数据不能为空");
        }

        Integer punishmentId = CommonMethod.getInteger(form, "punishmentId");
        Integer studentId = CommonMethod.getInteger(form, "studentId");
        String studentName = CommonMethod.getString(form, "studentName");
        String studentNum = CommonMethod.getString(form, "studentNum");

        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            Integer currentPersonId = CommonMethod.getPersonId();
            if (currentPersonId == null) {
                throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "未识别到当前学生身份");
            }
            Student student = studentRepository.findByPersonPersonId(currentPersonId)
                    .filter(s -> s.getPerson() != null)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.STUDENT_DATA_INCOMPLETE, "当前学生信息不存在"));
            studentId = currentPersonId;
            studentName = student.getPerson().getName();
            studentNum = student.getPerson().getNum();
        }

        if (studentId == null || studentId <= 0) {
            throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "请先通过学号查找学生");
        }
        // 只对教师角色进行数据范围限制，管理员可以管理任意学生的处分
        if (teacherDataScopeService.isCurrentRoleTeacher()) {
            teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, "教师仅可管理本人授课学生的处分记录");
        }

        Punishment p = null;
        if (punishmentId != null && punishmentId > 0) {
            Optional<Punishment> op = punishmentRepository.findById(punishmentId);
            if (op.isPresent()) {
                p = op.get();
                p.setUpdateTime(LocalDateTime.now());
            }
        }

        if (p == null) {
            p = new Punishment();
            p.setCreateTime(LocalDateTime.now());
        }

        p.setStudentId(studentId);
        p.setStudentName(studentName);
        p.setStudentNum(studentNum);
        p.setPunishmentType(CommonMethod.getString(form, "punishmentType"));

        String dateStr = CommonMethod.getString(form, "punishmentDate");
        if (dateStr != null && !dateStr.isEmpty()) {
            try { p.setPunishmentDate(LocalDate.parse(dateStr)); } catch (Exception ignored) {}
        }

        String status = CommonMethod.getString(form, "status");
        p.setStatus(status != null && !status.isEmpty() ? status : "pending");

        p.setReason(CommonMethod.getString(form, "reason"));
        p.setRemark(CommonMethod.getString(form, "remark"));

        punishmentRepository.save(p);
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse punishmentDelete(DataRequest dataRequest) {
        Integer punishmentId = dataRequest.getInteger("punishmentId");
        if (punishmentId != null && punishmentId > 0) {
            Optional<Punishment> op = punishmentRepository.findById(punishmentId);
            if (op.isPresent()) {
                Punishment punishment = op.get();
                // 只对教师角色进行数据范围限制，管理员可以删除任意学生的处分
                if (teacherDataScopeService.isCurrentRoleTeacher()) {
                    teacherDataScopeService.assertCurrentTeacherAccessStudent(
                            punishment.getStudentId(),
                            "教师仅可删除本人授课学生的处分记录");
                }
                punishmentRepository.delete(punishment);
            }
        }
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse revokePunishment(DataRequest dataRequest) {
        Integer punishmentId = dataRequest.getInteger("punishmentId");
        if (punishmentId == null || punishmentId <= 0) {
            throw new BusinessException(ErrorCodes.PUNISHMENT_NOT_FOUND, "处分ID不能为空");
        }
        Punishment p = punishmentRepository.findById(punishmentId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.PUNISHMENT_NOT_FOUND, "处分记录不存在"));
        // 只对教师角色进行数据范围限制，管理员可以撤销任意学生的处分
        if (teacherDataScopeService.isCurrentRoleTeacher()) {
            teacherDataScopeService.assertCurrentTeacherAccessStudent(
                    p.getStudentId(),
                    "教师仅可撤销本人授课学生的处分记录");
        }
        p.setStatus("revoked");
        p.setUpdateTime(LocalDateTime.now());
        punishmentRepository.save(p);
        return CommonMethod.getReturnMessageOK();
    }

    private Map<String, Object> getMapFromPunishment(Punishment p) {
        Map<String, Object> map = new HashMap<>();
        map.put("punishmentId", p.getPunishmentId());
        map.put("studentId", p.getStudentId());
        map.put("studentName", p.getStudentName());
        map.put("studentNum", p.getStudentNum());
        map.put("punishmentType", p.getPunishmentType());
        map.put("punishmentDate", p.getPunishmentDate());
        map.put("status", p.getStatus());
        map.put("reason", p.getReason());
        map.put("remark", p.getRemark());
        map.put("createTime", p.getCreateTime());
        map.put("updateTime", p.getUpdateTime());
        return map;
    }
}
