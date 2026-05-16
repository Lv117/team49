package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.EUserType;
import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.models.User;
import cn.edu.sdu.java.server.models.UserType;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.PersonRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.repositorys.UserRepository;
import cn.edu.sdu.java.server.repositorys.UserTypeRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import cn.edu.sdu.java.server.util.DateTimeTool;
import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

/**
 * 用户注册服务实现类
 * 实现用户名重名校验和用户注册功能
 */
@Service
public class RegisterServiceImpl implements RegisterService {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final UserTypeRepository userTypeRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public RegisterServiceImpl(UserRepository userRepository,
                               PersonRepository personRepository,
                               UserTypeRepository userTypeRepository,
                               StudentRepository studentRepository,
                               PasswordEncoder passwordEncoder,
                               EntityManager entityManager) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.userTypeRepository = userTypeRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    /**
     * 校验用户名是否已存在
     * 仅校验 user 表的 user_name 字段
     */
    @Override
    public DataResponse checkUsername(DataRequest dataRequest) {
        String username = dataRequest.getString("username");

        // 参数校验
        if (username == null || username.trim().isEmpty()) {
            return DataResponse.success(false);
        }

        // 查询 user 表中 user_name 是否已存在
        boolean exists = userRepository.existsByUserName(username.trim());
        // data=true 可用（不存在），data=false 已存在
        return DataResponse.success(!exists);
    }

    /**
     * 用户注册
     * 包含完整的事务管理
     */
    @Override
    @Transactional
    public DataResponse register(DataRequest dataRequest) {
        // 1. 获取请求参数
        String username = dataRequest.getString("username");
        String password = dataRequest.getString("password");
        String name = dataRequest.getString("name");
        String role = dataRequest.getString("role");
        String userId = dataRequest.getString("userId");
        String email = dataRequest.getString("email");
        String phone = dataRequest.getString("phone");
        String major = dataRequest.getString("major");
        String className = dataRequest.getString("className");

        // 2. 参数校验
        if (username == null || username.trim().isEmpty()) {
            return CommonMethod.getReturnMessageError("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return CommonMethod.getReturnMessageError("密码不能为空");
        }
        if (name == null || name.trim().isEmpty()) {
            return CommonMethod.getReturnMessageError("姓名不能为空");
        }
        if (role == null || role.trim().isEmpty()) {
            return CommonMethod.getReturnMessageError("角色不能为空");
        }
        if (userId == null || userId.trim().isEmpty()) {
            return CommonMethod.getReturnMessageError("学号/工号不能为空");
        }

        // 用户名长度校验
        if (username.length() > 20) {
            return CommonMethod.getReturnMessageError("用户名长度不能超过20位");
        }

        // 密码长度校验
        if (password.length() < 6) {
            return CommonMethod.getReturnMessageError("密码长度不能少于6位");
        }

        // 3. 校验用户名是否重复
        if (userRepository.existsByUserName(username.trim())) {
            return CommonMethod.getReturnMessageError("用户名已存在");
        }

        // 4. 角色映射
        // 管理员→user_type_id=1，学生→2，教师→3
        UserType userType;
        String personType;
        switch (role.trim()) {
            case "管理员":
                userType = userTypeRepository.findByName(EUserType.ROLE_ADMIN.name());
                personType = "0";
                break;
            case "学生":
                userType = userTypeRepository.findByName(EUserType.ROLE_STUDENT.name());
                personType = "1";
                break;
            case "教师":
                userType = userTypeRepository.findByName(EUserType.ROLE_TEACHER.name());
                personType = "2";
                break;
            default:
                return CommonMethod.getReturnMessageError("无效的角色，仅支持：管理员、学生、教师");
        }

        if (userType == null) {
            return CommonMethod.getReturnMessageError("角色类型不存在，请联系管理员");
        }

        // 5. 创建 Person 记录
        Person person = new Person();
        person.setNum(userId.trim());       // 学号/工号
        person.setName(name.trim());        // 姓名
        person.setType(personType);         // 人员类型
        if (email != null && !email.trim().isEmpty()) {
            person.setEmail(email.trim());  // 邮箱（选填）
        }
        if (phone != null && !phone.trim().isEmpty()) {
            person.setPhone(phone.trim());  // 手机号（选填）
        }
        // 保存 Person 获取自动生成的 personId
        person = personRepository.save(person);

        // 6. 创建 User 记录
        User user = new User();
        // 设置 personId（@Id 主键），与 @OneToOne 关联的 person 保持一致
        user.setPersonId(person.getPersonId());
        user.setPerson(person);                  // 关联 Person 对象
        user.setUserType(userType);              // 用户类型
        user.setUserName(username.trim());       // 登录账号
        // 密码 BCrypt 加密
        user.setPassword(passwordEncoder.encode(password.trim()));
        // 当前时间
        String now = DateTimeTool.parseDateTime(new Date());
        user.setCreateTime(now);                 // 创建时间
        user.setCreatorId(0);                    // 注册用户 creator_id=0
        user.setLoginCount(0);                   // 登录次数初始为0
        // lastLoginTime 为空（默认）

        // 使用 persist 而非 save，避免因 personId 已设置导致 Hibernate 误判为 detached 而调用 merge
        entityManager.persist(user);
        entityManager.flush();

        // 7. 如果角色为【学生】，同步创建 Student 记录
        if ("学生".equals(role.trim())) {
            Student student = new Student();
            student.setPersonId(person.getPersonId());
            student.setPerson(person);
            // 专业和班级：前端传入则使用，否则使用默认值
            student.setMajor(major != null && !major.trim().isEmpty() ? major.trim() : "未填写");
            student.setClassName(className != null && !className.trim().isEmpty() ? className.trim() : "未填写");
            // 使用 persist 而非 save，避免因 personId 已设置导致 Hibernate 误判为 detached 而调用 merge
            entityManager.persist(student);
        }

        // 8. 返回成功
        return DataResponse.success();
    }
}