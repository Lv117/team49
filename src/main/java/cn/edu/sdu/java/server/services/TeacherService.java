package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.*;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.PersonRepository;
import cn.edu.sdu.java.server.repositorys.TeacherRepository;
import cn.edu.sdu.java.server.repositorys.UserRepository;
import cn.edu.sdu.java.server.repositorys.UserTypeRepository;
import cn.edu.sdu.java.server.util.ComDataUtil;
import cn.edu.sdu.java.server.util.CommonMethod;
import cn.edu.sdu.java.server.util.DateTimeTool;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TeacherService {
    private final PersonRepository personRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final PasswordEncoder encoder;

    public TeacherService(PersonRepository personRepository, TeacherRepository teacherRepository, UserRepository userRepository, UserTypeRepository userTypeRepository, PasswordEncoder encoder) {
        this.personRepository = personRepository;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.userTypeRepository = userTypeRepository;
        this.encoder = encoder;
    }

    public Map<String, Object> getMapFromTeacher(Teacher t) {
        Map<String, Object> m = new HashMap<>();
        if (t == null) {
            return m;
        }
        m.put("title", t.getTitle());
        m.put("degree", t.getDegree());
        Person p = t.getPerson();
        if (p == null) {
            return m;
        }
        m.put("personId", t.getPersonId());
        m.put("num", p.getNum());
        m.put("name", p.getName());
        m.put("dept", p.getDept());
        m.put("card", p.getCard());
        String gender = p.getGender();
        m.put("gender", gender);
        m.put("genderName", ComDataUtil.getInstance().getDictionaryLabelByValue("XBM", gender));
        m.put("birthday", p.getBirthday());
        m.put("email", p.getEmail());
        m.put("phone", p.getPhone());
        m.put("address", p.getAddress());
        return m;
    }

    public List<Map<String, Object>> getTeacherMapList(String numName) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        List<Teacher> tList = teacherRepository.findTeacherListByNumName(numName);
        if (tList == null || tList.isEmpty()) {
            return dataList;
        }
        for (Teacher teacher : tList) {
            dataList.add(getMapFromTeacher(teacher));
        }
        return dataList;
    }

    public DataResponse getTeacherList(DataRequest dataRequest) {
        String numName = dataRequest.getString("numName");
        return CommonMethod.getReturnData(getTeacherMapList(numName));
    }

    public DataResponse teacherDelete(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        if (personId == null || personId <= 0) {
            throw new BusinessException(ErrorCodes.TEACHER_NOT_FOUND, "教师ID为空，不能删除");
        }
        Optional<Teacher> op = teacherRepository.findById(personId);
        if (op.isPresent()) {
            Teacher t = op.get();
            Optional<User> uOp = userRepository.findById(personId);
            uOp.ifPresent(userRepository::delete);
            Person p = t.getPerson();
            teacherRepository.delete(t);
            personRepository.delete(p);
        }
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse getTeacherInfo(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        Teacher t = null;
        if (personId != null) {
            Optional<Teacher> op = teacherRepository.findById(personId);
            if (op.isPresent()) {
                t = op.get();
            }
        }
        return CommonMethod.getReturnData(getMapFromTeacher(t));
    }

    @Transactional(rollbackFor = Exception.class)
    public DataResponse teacherEditSave(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        Map<String, Object> form = dataRequest.getMap("form");
        if (form == null) {
            throw new BusinessException(ErrorCodes.TEACHER_FORM_INVALID, "教师信息为空，不能保存");
        }
        String num = CommonMethod.getString(form, "num");
        if (num == null || num.trim().isEmpty()) {
            throw new BusinessException(ErrorCodes.TEACHER_NUM_REQUIRED, "工号为空，不能保存");
        }
        num = num.trim();

        Teacher t = null;
        Person p = null;
        User u;
        if (personId != null) {
            Optional<Teacher> op = teacherRepository.findById(personId);
            if (op.isPresent()) {
                t = op.get();
            }
        }

        Optional<Person> nOp = personRepository.findByNum(num);
        if (nOp.isPresent()) {
            Person existedPerson = nOp.get();
            if (t == null) {
                Optional<Teacher> existedTeacher = teacherRepository.findById(existedPerson.getPersonId());
                if (existedTeacher.isPresent()) {
                    throw new BusinessException(ErrorCodes.TEACHER_NUM_CONFLICT, "新工号已经存在，不能添加或修改");
                }
                if ("2".equals(existedPerson.getType())) {
                    // 历史半成功数据：person(type=2) 存在但 teacher/user 可能缺失，自动补齐
                    p = existedPerson;
                    personId = p.getPersonId();
                    Optional<User> existedUser = userRepository.findByPersonPersonId(personId);
                    UserType teacherType = userTypeRepository.findByName(EUserType.ROLE_TEACHER.name());
                    if (existedUser.isPresent()) {
                        u = existedUser.get();
                        u.setUserName(num);
                        u.setUserType(teacherType);
                        userRepository.save(u);
                    } else {
                        u = new User();
                        u.setPersonId(personId);
                        u.setUserName(num);
                        u.setPassword(encoder.encode("123456"));
                        u.setUserType(teacherType);
                        u.setCreateTime(DateTimeTool.parseDateTime(new Date()));
                        u.setCreatorId(CommonMethod.getPersonId());
                        u.setLoginCount(0);
                        userRepository.save(u);
                    }
                    t = new Teacher();
                    t.setPersonId(personId);
                    teacherRepository.save(t);
                } else {
                    throw new BusinessException(ErrorCodes.TEACHER_NUM_CONFLICT, "新工号已经存在，不能添加或修改");
                }
            } else {
                String oldNum = t.getPerson() == null ? null : t.getPerson().getNum();
                if (!num.equals(oldNum)) {
                    throw new BusinessException(ErrorCodes.TEACHER_NUM_CONFLICT, "新工号已经存在，不能添加或修改");
                }
            }
        }

        if (t == null) {
            p = new Person();
            p.setNum(num);
            p.setType("2");
            personRepository.save(p);

            personId = p.getPersonId();
            u = new User();
            u.setPersonId(personId);
            u.setUserName(num);
            u.setPassword(encoder.encode("123456"));
            u.setUserType(userTypeRepository.findByName(EUserType.ROLE_TEACHER.name()));
            u.setCreateTime(DateTimeTool.parseDateTime(new Date()));
            u.setCreatorId(CommonMethod.getPersonId());
            u.setLoginCount(0);
            userRepository.save(u);

            t = new Teacher();
            t.setPersonId(personId);
            teacherRepository.save(t);
        } else {
            p = t.getPerson();
            if (p == null && personId != null) {
                Optional<Person> pOp = personRepository.findById(personId);
                if (pOp.isPresent()) {
                    p = pOp.get();
                    t.setPerson(p);
                } else {
                    throw new BusinessException(ErrorCodes.TEACHER_NOT_FOUND, "教师对应人员信息不存在");
                }
            }
        }

        personId = p.getPersonId();
        if (!num.equals(p.getNum())) {
            Optional<User> uOp = userRepository.findByPersonPersonId(personId);
            if (uOp.isPresent()) {
                u = uOp.get();
                u.setUserName(num);
                userRepository.save(u);
            }
            p.setNum(num);
        }

        p.setName(CommonMethod.getString(form, "name"));
        p.setDept(CommonMethod.getString(form, "dept"));
        p.setCard(CommonMethod.getString(form, "card"));
        p.setGender(CommonMethod.getString(form, "gender"));
        p.setBirthday(CommonMethod.getString(form, "birthday"));
        p.setEmail(CommonMethod.getString(form, "email"));
        p.setPhone(CommonMethod.getString(form, "phone"));
        p.setAddress(CommonMethod.getString(form, "address"));
        personRepository.save(p);

        t.setTitle(CommonMethod.getString(form, "title"));
        t.setDegree(CommonMethod.getString(form, "degree"));
        t.setPersonId(personId);
        teacherRepository.save(t);

        return CommonMethod.getReturnData(t.getPersonId());
    }
}
