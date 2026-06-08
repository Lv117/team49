package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.FamilyMember;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.FamilyMemberRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * FamilyMemberService 家庭成员管理服务类
 */
@Service
public class FamilyMemberService {
    private final FamilyMemberRepository familyMemberRepository;
    private final StudentRepository studentRepository;
    private final TeacherDataScopeService teacherDataScopeService;

    public FamilyMemberService(FamilyMemberRepository familyMemberRepository,
                               StudentRepository studentRepository,
                               TeacherDataScopeService teacherDataScopeService) {
        this.familyMemberRepository = familyMemberRepository;
        this.studentRepository = studentRepository;
        this.teacherDataScopeService = teacherDataScopeService;
    }

    /**
     * 获取家庭成员列表
     */
    public DataResponse getFamilyMemberList(DataRequest dataRequest) {
        Integer studentId = dataRequest.getInteger("studentId");
        
        // 学生只能查看自己的家庭成员
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            Integer currentPersonId = CommonMethod.getPersonId();
            if (currentPersonId == null || currentPersonId <= 0) {
                throw new BusinessException(ErrorCodes.AUTH_FAILED, "当前登录状态无效，请重新登录");
            }
            studentId = currentPersonId;
        }
        
        // 教师只能查看自己授课学生的家庭成员
        if (studentId != null && studentId > 0) {
            teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, "教师仅可查看本人授课学生的家庭成员");
        }
        
        List<FamilyMember> memberList;
        if (studentId != null && studentId > 0) {
            memberList = familyMemberRepository.findByStudentPersonId(studentId);
        } else {
            memberList = familyMemberRepository.findAll();
        }
        
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (FamilyMember member : memberList) {
            dataList.add(getMapFromFamilyMember(member));
        }
        
        return CommonMethod.getReturnData(dataList);
    }

    /**
     * 保存家庭成员
     */
    public DataResponse familyMemberSave(DataRequest dataRequest) {
        Map<String, Object> form = dataRequest.getMap("form");
        if (form == null || form.isEmpty()) {
            form = dataRequest.getMap("data");
        }
        if (form == null || form.isEmpty()) {
            form = dataRequest.getData();
        }
        
        Integer memberId = CommonMethod.getInteger(form, "memberId");
        Integer studentId = CommonMethod.getInteger(form, "studentId");
        
        // 学生只能添加自己的家庭成员
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            Integer currentPersonId = CommonMethod.getPersonId();
            if (currentPersonId == null || currentPersonId <= 0) {
                throw new BusinessException(ErrorCodes.AUTH_FAILED, "当前登录状态无效，请重新登录");
            }
            studentId = currentPersonId;
        }
        
        if (studentId == null || studentId <= 0) {
            throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "学生ID不能为空");
        }
        
        teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, "教师仅可管理本人授课学生的家庭成员");
        
        Optional<Student> studentOp = studentRepository.findById(studentId);
        if (studentOp.isEmpty()) {
            throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "学生不存在");
        }
        
        FamilyMember member;
        if (memberId != null && memberId > 0) {
            Optional<FamilyMember> op = familyMemberRepository.findById(memberId);
            if (op.isPresent()) {
                member = op.get();
            } else {
                throw new BusinessException(ErrorCodes.FAMILY_MEMBER_NOT_FOUND, "家庭成员不存在");
            }
        } else {
            member = new FamilyMember();
            member.setStudent(studentOp.get());
        }
        
        member.setRelation(CommonMethod.getString(form, "relation"));
        member.setName(CommonMethod.getString(form, "name"));
        member.setGender(CommonMethod.getString(form, "gender"));
        member.setAge(CommonMethod.getInteger(form, "age"));
        member.setUnit(CommonMethod.getString(form, "unit"));
        member.setPhone(CommonMethod.getString(form, "phone"));

        familyMemberRepository.save(member);
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 删除家庭成员
     */
    public DataResponse familyMemberDelete(DataRequest dataRequest) {
        Integer memberId = dataRequest.getInteger("memberId");
        if (memberId == null || memberId <= 0) {
            throw new BusinessException(ErrorCodes.FAMILY_MEMBER_NOT_FOUND, "家庭成员ID不能为空");
        }
        
        FamilyMember member = familyMemberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.FAMILY_MEMBER_NOT_FOUND, "家庭成员不存在"));
        
        Integer studentId = member.getStudent() != null ? member.getStudent().getPersonId() : null;
        teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, "教师仅可删除本人授课学生的家庭成员");
        
        familyMemberRepository.delete(member);
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 将 FamilyMember 转换为 Map
     */
    private Map<String, Object> getMapFromFamilyMember(FamilyMember member) {
        Map<String, Object> map = new HashMap<>();
        map.put("memberId", member.getMemberId());
        map.put("studentId", member.getStudent() != null ? member.getStudent().getPersonId() : null);
        map.put("studentName", member.getStudent() != null && member.getStudent().getPerson() != null ? 
                member.getStudent().getPerson().getName() : "");
        map.put("relation", member.getRelation());
        map.put("name", member.getName());
        map.put("gender", member.getGender());
        map.put("age", member.getAge());
        map.put("unit", member.getUnit());
        map.put("phone", member.getPhone());
        return map;
    }
}
