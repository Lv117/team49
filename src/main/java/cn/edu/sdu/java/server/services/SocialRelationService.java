package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.SocialRelation;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.SocialRelationRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * SocialRelationService 社会关系管理服务类
 */
@Service
public class SocialRelationService {
    private final SocialRelationRepository socialRelationRepository;
    private final StudentRepository studentRepository;
    private final TeacherDataScopeService teacherDataScopeService;

    public SocialRelationService(SocialRelationRepository socialRelationRepository,
                                 StudentRepository studentRepository,
                                 TeacherDataScopeService teacherDataScopeService) {
        this.socialRelationRepository = socialRelationRepository;
        this.studentRepository = studentRepository;
        this.teacherDataScopeService = teacherDataScopeService;
    }

    /**
     * 获取社会关系列表
     */
    public DataResponse getSocialRelationList(DataRequest dataRequest) {
        Integer studentId = dataRequest.getInteger("studentId");
        
        // 学生只能查看自己的社会关系
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            Integer currentPersonId = CommonMethod.getPersonId();
            if (currentPersonId == null || currentPersonId <= 0) {
                throw new BusinessException(ErrorCodes.AUTH_FAILED, "当前登录状态无效，请重新登录");
            }
            studentId = currentPersonId;
        }
        
        // 教师只能查看自己授课学生的社会关系
        if (studentId != null && studentId > 0) {
            teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, "教师仅可查看本人授课学生的社会关系");
        }
        
        List<SocialRelation> relationList;
        if (studentId != null && studentId > 0) {
            relationList = socialRelationRepository.findByStudentPersonId(studentId);
        } else {
            relationList = socialRelationRepository.findAll();
        }
        
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (SocialRelation relation : relationList) {
            dataList.add(getMapFromSocialRelation(relation));
        }
        
        return CommonMethod.getReturnData(dataList);
    }

    /**
     * 保存社会关系
     */
    public DataResponse socialRelationSave(DataRequest dataRequest) {
        Map<String, Object> form = dataRequest.getMap("form");
        if (form == null || form.isEmpty()) {
            form = dataRequest.getMap("data");
        }
        if (form == null || form.isEmpty()) {
            form = dataRequest.getData();
        }
        
        Integer relationId = CommonMethod.getInteger(form, "relationId");
        Integer studentId = CommonMethod.getInteger(form, "studentId");
        
        // 学生只能添加自己的社会关系
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
        
        teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, "教师仅可管理本人授课学生的社会关系");
        
        Optional<Student> studentOp = studentRepository.findById(studentId);
        if (studentOp.isEmpty()) {
            throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "学生不存在");
        }
        
        SocialRelation relation;
        if (relationId != null && relationId > 0) {
            Optional<SocialRelation> op = socialRelationRepository.findById(relationId);
            if (op.isPresent()) {
                relation = op.get();
            } else {
                throw new BusinessException(ErrorCodes.SOCIAL_RELATION_NOT_FOUND, "社会关系不存在");
            }
        } else {
            relation = new SocialRelation();
            relation.setStudent(studentOp.get());
        }
        
        relation.setRelationType(CommonMethod.getString(form, "relationType"));
        relation.setName(CommonMethod.getString(form, "name"));
        relation.setGender(CommonMethod.getString(form, "gender"));
        relation.setPhone(CommonMethod.getString(form, "phone"));
        relation.setAge(CommonMethod.getInteger(form, "age"));
        relation.setRemark(CommonMethod.getString(form, "remark"));
        
        socialRelationRepository.save(relation);
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 删除社会关系
     */
    public DataResponse socialRelationDelete(DataRequest dataRequest) {
        Integer relationId = dataRequest.getInteger("relationId");
        if (relationId == null || relationId <= 0) {
            throw new BusinessException(ErrorCodes.SOCIAL_RELATION_NOT_FOUND, "社会关系ID不能为空");
        }
        
        SocialRelation relation = socialRelationRepository.findById(relationId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.SOCIAL_RELATION_NOT_FOUND, "社会关系不存在"));
        
        Integer studentId = relation.getStudent() != null ? relation.getStudent().getPersonId() : null;
        teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, "教师仅可删除本人授课学生的社会关系");
        
        socialRelationRepository.delete(relation);
        return CommonMethod.getReturnMessageOK();
    }

    /**
     * 将 SocialRelation 转换为 Map
     */
    private Map<String, Object> getMapFromSocialRelation(SocialRelation relation) {
        Map<String, Object> map = new HashMap<>();
        map.put("relationId", relation.getRelationId());
        map.put("studentId", relation.getStudent() != null ? relation.getStudent().getPersonId() : null);
        map.put("studentName", relation.getStudent() != null && relation.getStudent().getPerson() != null ? 
                relation.getStudent().getPerson().getName() : "");
        map.put("relationType", relation.getRelationType());
        map.put("name", relation.getName());
        map.put("gender", relation.getGender());
        map.put("phone", relation.getPhone());
        map.put("age", relation.getAge());
        map.put("remark", relation.getRemark());
        return map;
    }
}
