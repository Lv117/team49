package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.*;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.*;
import cn.edu.sdu.java.server.util.ComDataUtil;
import cn.edu.sdu.java.server.util.CommonMethod;
import cn.edu.sdu.java.server.util.DateTimeTool;
import jakarta.validation.ConstraintViolationException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

@Service
public class StudentService {
    private static final Logger log = LoggerFactory.getLogger(StudentService.class);
    private final PersonRepository personRepository;  //人员数据操作自动注入
    private final StudentRepository studentRepository;  //学生数据操作自动注入
    private final UserRepository userRepository;  //学生数据操作自动注入
    private final UserTypeRepository userTypeRepository; //用户类型数据操作自动注入
    private final PasswordEncoder encoder;  //密码服务自动注入
    private final FeeRepository feeRepository;  //消费数据操作自动注入
    private final FamilyMemberRepository familyMemberRepository;
    private final SocialRelationRepository socialRelationRepository;
    private final SystemService systemService;
    private final ScoreRepository scoreRepository;
    private final DevelopmentRepository developmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final TeacherDataScopeService teacherDataScopeService;
    private final CourseSelectionRepository courseSelectionRepository;
    private final HomeworkSubmissionRepository homeworkSubmissionRepository;
    private final PunishmentRepository punishmentRepository;
    private final DailyActivityRepository dailyActivityRepository;
    private final InnovationProjectRepository innovationProjectRepository;
    private final HonorRepository honorRepository;
    private final StudentExamScheduleRepository studentExamScheduleRepository;
    private final StudentLeaveRepository studentLeaveRepository;

    public StudentService(PersonRepository personRepository, StudentRepository studentRepository, UserRepository userRepository, UserTypeRepository userTypeRepository, PasswordEncoder encoder, FeeRepository feeRepository, FamilyMemberRepository familyMemberRepository, SocialRelationRepository socialRelationRepository, SystemService systemService, ScoreRepository scoreRepository, DevelopmentRepository developmentRepository, AttendanceRepository attendanceRepository, TeacherDataScopeService teacherDataScopeService, CourseSelectionRepository courseSelectionRepository, HomeworkSubmissionRepository homeworkSubmissionRepository, PunishmentRepository punishmentRepository, DailyActivityRepository dailyActivityRepository, InnovationProjectRepository innovationProjectRepository, HonorRepository honorRepository, StudentExamScheduleRepository studentExamScheduleRepository, StudentLeaveRepository studentLeaveRepository) {
        this.personRepository = personRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.userTypeRepository = userTypeRepository;
        this.encoder = encoder;
        this.feeRepository = feeRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.socialRelationRepository = socialRelationRepository;
        this.systemService = systemService;
        this.scoreRepository = scoreRepository;
        this.developmentRepository = developmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.teacherDataScopeService = teacherDataScopeService;
        this.courseSelectionRepository = courseSelectionRepository;
        this.homeworkSubmissionRepository = homeworkSubmissionRepository;
        this.punishmentRepository = punishmentRepository;
        this.dailyActivityRepository = dailyActivityRepository;
        this.innovationProjectRepository = innovationProjectRepository;
        this.honorRepository = honorRepository;
        this.studentExamScheduleRepository = studentExamScheduleRepository;
        this.studentLeaveRepository = studentLeaveRepository;
    }

    public Map<String,Object> getMapFromStudent(Student s) {
        Map<String,Object> m = new HashMap<>();
        Person p;
        if(s == null)
            return m;
        m.put("major",s.getMajor());
        m.put("className",s.getClassName());
        p = s.getPerson();
        if(p == null)
            return m;
        m.put("personId", s.getPersonId());
        m.put("num",p.getNum());
        m.put("name",p.getName());
        m.put("dept",p.getDept());
        m.put("card",p.getCard());
        String gender = p.getGender();
        m.put("gender",gender);
        m.put("genderName", ComDataUtil.getInstance().getDictionaryLabelByValue("XBM", gender)); //性别类型的值转换成数据类型名
        m.put("birthday", p.getBirthday());  //时间格式转换字符串
        m.put("email",p.getEmail());
        m.put("phone",p.getPhone());
        m.put("address",p.getAddress());
        m.put("introduce",p.getIntroduce());
        return m;
    }

    //Java 对象的注入 我们定义的这下Java的操作对象都不能自己管理是由有Spring框架来管理的， StudentController 中要使用StudentRepository接口的实现类对象，
    // 需要下列方式注入，否则无法使用， studentRepository 相当于StudentRepository接口实现对象的一个引用，由框架完成对这个引用的赋值，
    // StudentController中的方法可以直接使用

    public List<Map<String,Object>> getStudentMapList(String numName) {
        List<Map<String,Object>> dataList = new ArrayList<>();
        List<Student> sList = studentRepository.findStudentListByNumName(numName);  //数据库查询操作
        if (sList == null || sList.isEmpty())
            return dataList;
        for (Student student : sList) {
            dataList.add(getMapFromStudent(student));
        }
        return dataList;
    }

    public DataResponse getStudentList(DataRequest dataRequest) {
        String numName = dataRequest.getString("numName");
        List<Map<String,Object>> dataList = getStudentMapList(numName);
        // 教师角色只返回授课范围内的学生
        if (teacherDataScopeService.isCurrentRoleTeacher()) {
            Set<Integer> allowedIds = teacherDataScopeService.getCurrentTeacherStudentIds();
            dataList = dataList.stream()
                    .filter(m -> allowedIds.contains(CommonMethod.getInteger(m, "personId")))
                    .toList();
        }
        return CommonMethod.getReturnData(dataList);  //按照测试框架规范会送Map的list
    }



    @Transactional(rollbackFor = Exception.class)
    public DataResponse studentDelete(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        if (personId == null || personId <= 0) {
            throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "personId不能为空");
        }
        Student s = studentRepository.findById(personId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "学生不存在"));
        Person p = s.getPerson();
        if (p == null) {
            throw new BusinessException(ErrorCodes.STUDENT_DATA_INCOMPLETE, "人员信息不存在");
        }
        
        // 先删除所有关联表数据（按外键依赖顺序）
        // 1. 删除消费记录
        List<Fee> feeList = feeRepository.findListByStudent(personId);
        if (feeList != null && !feeList.isEmpty()) {
            feeRepository.deleteAll(feeList);
        }
        
        // 2. 删除成绩记录
        List<Score> scoreList = scoreRepository.findByStudentPersonId(personId);
        if (scoreList != null && !scoreList.isEmpty()) {
            scoreRepository.deleteAll(scoreList);
        }
        
        // 3. 删除家庭成员
        List<FamilyMember> familyList = familyMemberRepository.findByStudentPersonId(personId);
        if (familyList != null && !familyList.isEmpty()) {
            familyMemberRepository.deleteAll(familyList);
        }
        
        // 4. 删除社会关系
        List<SocialRelation> socialList = socialRelationRepository.findByStudentPersonId(personId);
        if (socialList != null && !socialList.isEmpty()) {
            socialRelationRepository.deleteAll(socialList);
        }
        
        // 5. 删除考勤记录
        List<Attendance> attendanceList = attendanceRepository.findByStudentPersonId(personId);
        if (attendanceList != null && !attendanceList.isEmpty()) {
            attendanceRepository.deleteAll(attendanceList);
        }
        
        // 6. 删除发展记录
        List<StudentDevelopment> developmentList = developmentRepository.findByStudentId(personId);
        if (developmentList != null && !developmentList.isEmpty()) {
            developmentRepository.deleteAll(developmentList);
        }
        
        // 7. 删除选课记录
        List<CourseSelection> courseSelectionList = courseSelectionRepository.findByStudentPersonId(personId);
        if (courseSelectionList != null && !courseSelectionList.isEmpty()) {
            courseSelectionRepository.deleteAll(courseSelectionList);
        }
        
        // 8. 删除作业提交记录
        List<HomeworkSubmission> homeworkSubmissionList = homeworkSubmissionRepository.findByStudentPersonId(personId);
        if (homeworkSubmissionList != null && !homeworkSubmissionList.isEmpty()) {
            homeworkSubmissionRepository.deleteAll(homeworkSubmissionList);
        }
        
        // 9. 删除处分记录
        List<Punishment> punishmentList = punishmentRepository.findByStudentId(personId);
        if (punishmentList != null && !punishmentList.isEmpty()) {
            punishmentRepository.deleteAll(punishmentList);
        }
        
        // 10. 删除日常活动记录
        List<DailyActivity> dailyActivityList = dailyActivityRepository.findByStudentId(personId);
        if (dailyActivityList != null && !dailyActivityList.isEmpty()) {
            dailyActivityRepository.deleteAll(dailyActivityList);
        }
        
        // 11. 删除创新项目记录
        List<InnovationProject> innovationProjectList = innovationProjectRepository.findByStudentId(personId);
        if (innovationProjectList != null && !innovationProjectList.isEmpty()) {
            innovationProjectRepository.deleteAll(innovationProjectList);
        }
        
        // 12. 删除荣誉记录
        List<Honor> honorList = honorRepository.findByStudentId(personId);
        if (honorList != null && !honorList.isEmpty()) {
            honorRepository.deleteAll(honorList);
        }
        
        // 13. 删除考试安排记录
        List<StudentExamSchedule> examScheduleList = studentExamScheduleRepository.findByStudentId(String.valueOf(personId));
        if (examScheduleList != null && !examScheduleList.isEmpty()) {
            studentExamScheduleRepository.deleteAll(examScheduleList);
        }
        
        // 14. 删除请假记录（使用getStudentLeaveList方法，传入null表示不限制状态）
        List<StudentLeave> studentLeaveList = studentLeaveRepository.getStudentLeaveList(null, "", "", "", personId);
        if (studentLeaveList != null && !studentLeaveList.isEmpty()) {
            studentLeaveRepository.deleteAll(studentLeaveList);
        }
        
        // 删除顺序：先删 student → 再删 user → 最后删 person
        studentRepository.delete(s);
        userRepository.findById(personId).ifPresent(userRepository::delete);
        personRepository.delete(p);
        return CommonMethod.getReturnMessageOK();
    }


    public DataResponse getStudentInfo(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        // 教师只能查询本人授课学生的信息
        if (personId != null && teacherDataScopeService.isCurrentRoleTeacher()) {
            teacherDataScopeService.assertCurrentTeacherAccessStudent(personId, "您仅可查看本人授课学生的信息");
        }
        Student s = null;
        Optional<Student> op;
        if (personId != null) {
            op = studentRepository.findById(personId); //根据学生主键从数据库查询学生的信息
            if (op.isPresent()) {
                s = op.get();
            }
        }
        return CommonMethod.getReturnData(getMapFromStudent(s)); //这里回传包含学生信息的Map对象
    }

    public DataResponse getStudentByIds(DataRequest dataRequest) {
        List<?> ids = dataRequest.getList("ids");
        List<Map<String, Object>> dataList = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            return CommonMethod.getReturnData(dataList);
        }
        Set<Integer> personIdSet = new LinkedHashSet<>();
        for (Object idObj : ids) {
            if (idObj == null) {
                continue;
            }
            Integer personId;
            if (idObj instanceof Number) {
                personId = ((Number) idObj).intValue();
            } else {
                try {
                    personId = (int) Double.parseDouble(idObj.toString());
                } catch (Exception e) {
                    personId = null;
                }
            }
            if (personId != null && personId > 0) {
                personIdSet.add(personId);
            }
        }
        if (personIdSet.isEmpty()) {
            return CommonMethod.getReturnData(dataList);
        }
        for (Integer personId : personIdSet) {
            Optional<Student> op = studentRepository.findById(personId);
            if (op.isEmpty()) {
                continue;
            }
            Student s = op.get();
            Person p = s.getPerson();
            if (p == null) {
                continue;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("personId", s.getPersonId());
            m.put("num", p.getNum());
            m.put("name", p.getName());
            dataList.add(m);
        }
        return CommonMethod.getReturnData(dataList);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 验证邮箱格式是否合法
     * @param email 邮箱地址
     * @return 如果邮箱为空或格式正确返回 null，否则返回错误信息
     */
    private String validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null; // 邮箱为空不验证
        }
        // 邮箱格式正则：本地部分@域名部分
        // 本地部分：允许字母、数字、下划线、点、横线
        // 域名部分：允许字母、数字、横线，必须包含点，顶级域名至少2个字符
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        if (!email.matches(emailRegex)) {
            return "邮箱格式不正确，请输入有效的邮箱地址（如：example@domain.com）";
        }
        return null;
    }

    private void populatePerson(Person p, Map<String, Object> form, String num) {
        p.setNum(num);
        p.setName(normalizeText(CommonMethod.getString(form, "name")));
        p.setDept(normalizeText(CommonMethod.getString(form, "dept")));
        p.setCard(normalizeText(CommonMethod.getString(form, "card")));
        p.setGender(normalizeText(CommonMethod.getString(form, "gender")));
        p.setBirthday(normalizeText(CommonMethod.getString(form, "birthday")));
        p.setEmail(normalizeText(CommonMethod.getString(form, "email")));
        p.setPhone(normalizeText(CommonMethod.getString(form, "phone")));
        p.setAddress(normalizeText(CommonMethod.getString(form, "address")));
    }

    private void populateStudent(Student s, Map<String, Object> form) {
        s.setMajor(normalizeText(CommonMethod.getString(form, "major")));
        s.setClassName(normalizeText(CommonMethod.getString(form, "className")));
    }

    private boolean isStudentNumConflict(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase(Locale.ROOT);
                if (lowerMessage.contains("duplicate entry")
                        || lowerMessage.contains("unique")
                        || lowerMessage.contains("constraint")) {
                    if (lowerMessage.contains("num")
                            || lowerMessage.contains("username")
                            || lowerMessage.contains("user_name")) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String extractValidationMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                if (message.contains("Validation failed for classes")) {
                    return "学生信息校验未通过，请检查邮箱、学号等字段格式";
                }
                if (message.contains("ConstraintViolationImpl{interpolatedMessage='")) {
                    int start = message.indexOf("ConstraintViolationImpl{interpolatedMessage='");
                    if (start >= 0) {
                        start += "ConstraintViolationImpl{interpolatedMessage='".length();
                        int end = message.indexOf("'", start);
                        if (end > start) {
                            return "学生信息校验未通过：" + message.substring(start, end);
                        }
                    }
                }
            }
            current = current.getCause();
        }
        return "学生信息校验未通过，请检查输入内容";
    }

    private void rethrowStudentSaveException(Exception e) {
        if (e instanceof DataIntegrityViolationException && isStudentNumConflict(e)) {
            throw new BusinessException(ErrorCodes.STUDENT_NUM_CONFLICT, "学号已被占用，请使用其他学号", e);
        }
        if (e instanceof TransactionSystemException || e instanceof ConstraintViolationException) {
            throw new BusinessException(ErrorCodes.STUDENT_VALIDATION_ERROR, extractValidationMessage(e), e);
        }
        if (e instanceof CannotAcquireLockException || e instanceof PessimisticLockingFailureException) {
            throw new BusinessException(ErrorCodes.STUDENT_SAVE_TIMEOUT, "数据库写入超时，请稍后重试", e);
        }
        if (e instanceof DataAccessException) {
            throw new BusinessException(ErrorCodes.STUDENT_SAVE_DB_ERROR, "学生信息写入失败，请稍后重试", e);
        }
        throw new BusinessException(ErrorCodes.STUDENT_SAVE_DB_ERROR, "学生信息写入失败，请稍后重试", e);
    }

    @Transactional(rollbackFor = Exception.class)
    public DataResponse studentEditSave(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        Map<String,Object> form = dataRequest.getMap("form"); //参数获取Map对象
        String num = normalizeText(CommonMethod.getString(form, "num"));  //Map 获取属性的值
        Student s = null;
        Person p;
        User u;
        Optional<Student> op;
        boolean isNew = false;
        if (personId != null) {
            op = studentRepository.findById(personId);  //查询对应数据库中主键为id的值的实体对象
            if (op.isPresent()) {
                s = op.get();
            }
        }
        if (num == null) {
            return CommonMethod.getReturnMessageError("学号不能为空", ErrorCodes.STUDENT_NUM_REQUIRED);
        }
        // 验证邮箱格式
        String email = normalizeText(CommonMethod.getString(form, "email"));
        String emailError = validateEmail(email);
        if (emailError != null) {
            return CommonMethod.getReturnMessageError(emailError, ErrorCodes.STUDENT_EMAIL_INVALID);
        }
        Optional<Person> nOp = personRepository.findByNum(num); //查询是否存在num的人员
        Person existingPersonByNum = nOp.orElse(null);
        if (existingPersonByNum != null) {
            boolean sameStudent = s != null && Objects.equals(existingPersonByNum.getPersonId(), s.getPersonId());
            boolean existingStudentRecord = studentRepository.findById(existingPersonByNum.getPersonId()).isPresent();
            if (!sameStudent && existingStudentRecord) {
                return CommonMethod.getReturnMessageError("学号已被占用，请使用其他学号", ErrorCodes.STUDENT_NUM_CONFLICT);
            }
        }

        try {
            if (s == null) {
                if (existingPersonByNum != null) {
                    p = existingPersonByNum;
                    log.warn("检测到学号 {} 对应的残留 Person 记录，自动补全缺失的学生档案，personId={}", num, p.getPersonId());
                } else {
                    p = new Person();
                    p.setType("1");
                }
                populatePerson(p, form, num);
                if (p.getType() == null) {
                    p.setType("1");
                }
                personRepository.saveAndFlush(p);
                personId = p.getPersonId();

                UserType studentType = userTypeRepository.findByName(EUserType.ROLE_STUDENT.name());
                if (studentType == null) {
                    throw new BusinessException(ErrorCodes.STUDENT_USER_TYPE_MISSING, "学生角色配置缺失，请联系管理员");
                }

                Optional<User> uOp = userRepository.findByPersonPersonId(personId);
                if (uOp.isPresent()) {
                    u = uOp.get();
                } else {
                    u = new User();
                    u.setPersonId(personId);
                    u.setPassword(encoder.encode("123456"));
                    u.setCreateTime(DateTimeTool.parseDateTime(new Date()));
                    u.setCreatorId(CommonMethod.getPersonId());
                }
                u.setUserName(num);
                u.setUserType(studentType);
                userRepository.saveAndFlush(u);

                Optional<Student> existingStudentOp = studentRepository.findById(personId);
                if (existingStudentOp.isPresent()) {
                    s = existingStudentOp.get();
                } else {
                    s = new Student();
                    s.setPersonId(personId);
                }
                populateStudent(s, form);
                studentRepository.saveAndFlush(s);
                isNew = true;
            } else {
                p = s.getPerson();
                personId = p.getPersonId();
                populatePerson(p, form, num);
                personRepository.saveAndFlush(p);  // 修改保存人员信息

                Optional<User> uOp = userRepository.findByPersonPersonId(personId);
                if (uOp.isPresent()) {
                    u = uOp.get();
                    u.setUserName(num);
                    userRepository.saveAndFlush(u);
                }

                populateStudent(s, form);
                studentRepository.saveAndFlush(s);  //修改保存学生信息
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            rethrowStudentSaveException(e);
        }
        systemService.modifyLog(s,isNew);
        return CommonMethod.getReturnData(s.getPersonId());  // 将personId返回前端
    }

    public List<Map<String,Object>> getStudentScoreList(List<Score> sList) {
        List<Map<String,Object>> list = new ArrayList<>();
        if (sList == null || sList.isEmpty())
            return list;
        Map<String,Object> m;
        Course c;
        for (Score s : sList) {
            if (s == null || s.getStudent() == null || s.getStudent().getPerson() == null || s.getCourse() == null) {
                continue;
            }
            m = new HashMap<>();
            c = s.getCourse();
            m.put("studentNum", s.getStudent().getPerson().getNum());
            m.put("scoreId", s.getScoreId());
            m.put("courseNum", c.getNum());
            m.put("courseName", c.getName());
            m.put("credit", c.getCredit());
            m.put("mark", s.getMark());
            m.put("ranking", s.getRanking());
            list.add(m);
        }
        return list;
    }


    public List<Map<String,Object>> getStudentMarkList(List<Score> sList) {
        String[] title = {"优", "良", "中", "及格", "不及格"};
        int[] count = new int[5];
        List<Map<String,Object>> list = new ArrayList<>();
        if (sList == null || sList.isEmpty())
            return list;
        Map<String,Object> m;
        Course c;
        for (Score s : sList) {
            c = s.getCourse();
            if (s.getMark() != null && s.getMark().compareTo(new java.math.BigDecimal("90")) >= 0)
                count[0]++;
            else if (s.getMark() != null && s.getMark().compareTo(new java.math.BigDecimal("80")) >= 0)
                count[1]++;
            else if (s.getMark() != null && s.getMark().compareTo(new java.math.BigDecimal("70")) >= 0)
                count[2]++;
            else if (s.getMark() != null && s.getMark().compareTo(new java.math.BigDecimal("60")) >= 0)
                count[3]++;
            else
                count[4]++;
        }
        for (int i = 0; i < 5; i++) {
            m = new HashMap<>();
            m.put("name", title[i]);
            m.put("title", title[i]);
            m.put("value", count[i]);
            list.add(m);
        }
        return list;
    }


    public List<Map<String,Object>> getStudentFeeList(Integer personId) {
        List<Fee> sList = feeRepository.findListByStudent(personId);  // 查询某个学生消费记录集合
        List<Map<String,Object>> list = new ArrayList<>();
        if (sList == null || sList.isEmpty())
            return list;
        Map<String,Object> m;
        for (Fee s : sList) {
            m = new HashMap<>();
            m.put("title", s.getDay());
            m.put("value", s.getMoney());
            list.add(m);
        }
        return list;
    }




    public Map<String, Object> importFeeData(Integer personId, InputStream in){
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        List<String> failReasons = new ArrayList<>();
        
        try {
            Student student = studentRepository.findById(personId).get();
            XSSFWorkbook workbook = new XSSFWorkbook(in);  //打开Excl数据流
            XSSFSheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            Row row;
            Cell cell;
            int rowNum = 1;
            String day, consumptionType, moneyStr, description;
            Optional<Fee> fOp;
            double dMoney;
            Fee f;
            rowIterator.next();  // 跳过标题行
            while (rowIterator.hasNext()) {
                row = rowIterator.next();
                cell = row.getCell(0);
                if (cell == null)
                    break;
                // 读取导出文件的格式：日期(0)、类型(1)、金额(2)、备注(3)、创建时间(4)
                day = getCellStringValue(cell);  // 第0列：消费日期
                cell = row.getCell(1);
                consumptionType = cell != null ? getCellStringValue(cell) : "other";  // 第1列：消费类型
                cell = row.getCell(2);
                moneyStr = cell != null ? getCellStringValue(cell) : "0";  // 第2列：消费金额
                cell = row.getCell(3);
                description = cell != null ? getCellStringValue(cell) : "";  // 第3列：备注
                
                try {
                    // 转换消费类型
                    String typeCode = parseConsumptionType(consumptionType);
                    
                    fOp = feeRepository.findByStudentPersonIdAndDay(personId, day);  //查询是否存在记录
                    if (fOp.isEmpty()) {
                        f = new Fee();
                        f.setDay(day);
                        f.setStudent(student);  //不存在 添加
                    } else {
                        f = fOp.get();  //存在 更新
                    }
                    dMoney = Double.parseDouble(moneyStr);
                    f.setMoney(dMoney);
                    f.setConsumptionType(typeCode);
                    f.setDescription(description);
                    feeRepository.save(f);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    failReasons.add("第" + (rowNum + 1) + "行导入失败: " + e.getMessage());
                    log.error("导入第{}行数据失败: {}", rowNum + 1, e.getMessage());
                }
                rowNum++;
            }
            workbook.close();  //关闭Excl输入流
        } catch (Exception e) {
            log.error(e.getMessage());
            failReasons.add("上传错误: " + e.getMessage());
        }
        
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failReasons", failReasons);
        return result;
    }
    
    /**
     * 获取单元格值为字符串
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return new java.text.SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    return String.valueOf((long) val);
                }
                return String.valueOf(val);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return cell.getCellFormula();
                }
            default:
                return "";
        }
    }
    
    /**
     * 解析消费类型名称为类型代码
     */
    private String parseConsumptionType(String typeName) {
        if (typeName == null || typeName.trim().isEmpty()) {
            return "other";
        }
        String name = typeName.trim();
        return switch (name) {
            case "餐饮", "餐饮消费", "dining" -> "dining";
            case "学习用品", "study" -> "study";
            case "交通", "交通费", "transport" -> "transport";
            case "生活用品", "life" -> "life";
            case "娱乐", "娱乐消费", "entertainment" -> "entertainment";
            default -> "other";
        };
    }

    public DataResponse importFeeData(@RequestBody byte[] barr,
                                      String personIdStr
                                      ) {
        Integer personId =  Integer.parseInt(personIdStr);
        Map<String, Object> result = importFeeData(personId, new ByteArrayInputStream(barr));
        return CommonMethod.getReturnData(result);
    }

    public ResponseEntity<StreamingResponseBody> getStudentListExcl( DataRequest dataRequest) {
        String numName = dataRequest.getString("numName");
        List<Map<String,Object>> list = getStudentMapList(numName);
        Integer[] widths = {8, 20, 10, 15, 15, 15, 25, 10, 15, 30, 20, 30};
        int i, j;
        String[] titles = {"序号", "学号", "姓名", "学院", "专业", "班级", "证件号码", "性别", "出生日期", "邮箱", "电话", "地址"};
        String outPutSheetName = "student.xlsx";
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet(outPutSheetName);
        for (j = 0; j < widths.length; j++) {
            sheet.setColumnWidth(j, widths[j] * 256);
        }
        //合并第一行
        XSSFCellStyle style = CommonMethod.createCellStyle(wb, 11);
        XSSFRow row = null;
        XSSFCell[] cell = new XSSFCell[widths.length];
        row = sheet.createRow((int) 0);
        for (j = 0; j < widths.length; j++) {
            cell[j] = row.createCell(j);
            cell[j].setCellStyle(style);
            cell[j].setCellValue(titles[j]);
            cell[j].getCellStyle();
        }
        Map<String,Object> m;
        if (list != null && !list.isEmpty()) {
            for (i = 0; i < list.size(); i++) {
                row = sheet.createRow(i + 1);
                for (j = 0; j < widths.length; j++) {
                    cell[j] = row.createCell(j);
                    cell[j].setCellStyle(style);
                }
                m = list.get(i);
                cell[0].setCellValue((i + 1) + "");
                cell[1].setCellValue(CommonMethod.getString(m, "num"));
                cell[2].setCellValue(CommonMethod.getString(m, "name"));
                cell[3].setCellValue(CommonMethod.getString(m, "dept"));
                cell[4].setCellValue(CommonMethod.getString(m, "major"));
                cell[5].setCellValue(CommonMethod.getString(m, "className"));
                cell[6].setCellValue(CommonMethod.getString(m, "card"));
                cell[7].setCellValue(CommonMethod.getString(m, "genderName"));
                cell[8].setCellValue(CommonMethod.getString(m, "birthday"));
                cell[9].setCellValue(CommonMethod.getString(m, "email"));
                cell[10].setCellValue(CommonMethod.getString(m, "phone"));
                cell[11].setCellValue(CommonMethod.getString(m, "address"));
            }
        }
        try {
            StreamingResponseBody stream = wb::write;
            return ResponseEntity.ok()
                    .contentType(CommonMethod.exelType)
                    .body(stream);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }

    }

    public DataResponse getStudentPageData(DataRequest dataRequest) {
        String numName = dataRequest.getString("numName");
        Integer cPage = dataRequest.getCurrentPage();
        int dataTotal = 0;
        int size = 40;
        List<Map<String,Object>> dataList = new ArrayList<>();
        Page<Student> page = null;
        Pageable pageable = PageRequest.of(cPage, size);
        page = studentRepository.findStudentPageByNumName(numName, pageable);
        Map<String,Object> m;
        if (page != null) {
            dataTotal = (int) page.getTotalElements();
            List<Student> list = page.getContent();
            if (!list.isEmpty()) {
                for (Student student : list) {
                    m = getMapFromStudent(student);
                    dataList.add(m);
                }
            }
        }
        Map<String,Object> data = new HashMap<>();
        data.put("dataTotal", dataTotal);
        data.put("pageSize", size);
        data.put("dataList", dataList);
        return CommonMethod.getReturnData(data);
    }



    /*
        FamilyMember
     */
    public DataResponse getFamilyMemberList(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        personId = resolveOwnedStudentPersonId(personId);
        teacherDataScopeService.restrictStudentIdForTeacher(personId);
        List<FamilyMember> fList = familyMemberRepository.findByStudentPersonId(personId);
        List<Map<String,Object>> dataList = new ArrayList<>();
        Map<String,Object> m;
        if (fList != null) {
            for (FamilyMember f : fList) {
                m = new HashMap<>();
                m.put("memberId", f.getMemberId());
                m.put("personId", f.getStudent().getPersonId());
                m.put("relation", f.getRelation());
                m.put("name", f.getName());
                m.put("gender", f.getGender());
                m.put("age", f.getAge()+"");
                m.put("unit", f.getUnit());
                m.put("phone", f.getPhone());
                dataList.add(m);
            }
        }
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse familyMemberSave(DataRequest dataRequest) {
        Map<String,Object> form = dataRequest.getMap("form");
        Integer personId = CommonMethod.getInteger(form,"personId");
        Integer memberId = CommonMethod.getInteger(form,"memberId");
        personId = resolveOwnedStudentPersonId(personId);
        teacherDataScopeService.restrictStudentIdForTeacher(personId);
        Optional<FamilyMember> op;
        FamilyMember f = null;
        if(memberId != null) {
            op = familyMemberRepository.findById(memberId);
            if(op.isPresent()) {
                f = op.get();
                assertStudentOwnsRecord(f.getStudent() == null ? null : f.getStudent().getPersonId());
                teacherDataScopeService.assertCurrentTeacherAccessStudent(
                        f.getStudent() == null ? null : f.getStudent().getPersonId(),
                        "教师仅可维护本人授课学生的家庭成员信息");
                if (personId == null && f.getStudent() != null) {
                    personId = f.getStudent().getPersonId();
                }
            }
        }
        if(f== null) {
            f = new FamilyMember();
            if (personId == null || personId <= 0) {
                throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "学生ID不能为空");
            }
            Student student = studentRepository.findById(personId)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "学生不存在"));
            f.setStudent(student);
        }
        f.setRelation(CommonMethod.getString(form,"relation"));
        f.setName(CommonMethod.getString(form,"name"));
        f.setGender(CommonMethod.getString(form,"gender"));
        f.setAge(CommonMethod.getInteger(form,"age"));
        f.setUnit(CommonMethod.getString(form,"unit"));
        f.setPhone(CommonMethod.getString(form,"phone"));
        familyMemberRepository.save(f);
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse familyMemberDelete(DataRequest dataRequest) {
        Integer memberId = dataRequest.getInteger("memberId");
        Optional<FamilyMember> op;
        op = familyMemberRepository.findById(memberId);
        if (op.isPresent()) {
            FamilyMember familyMember = op.get();
            assertStudentOwnsRecord(familyMember.getStudent() == null ? null : familyMember.getStudent().getPersonId());
            teacherDataScopeService.assertCurrentTeacherAccessStudent(
                    familyMember.getStudent() == null ? null : familyMember.getStudent().getPersonId(),
                    "教师仅可删除本人授课学生的家庭成员信息");
            familyMemberRepository.delete(familyMember);
        }
        return CommonMethod.getReturnMessageOK();
    }

    /*
        SocialRelation
     */
    public DataResponse getSocialRelationList(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        personId = resolveOwnedStudentPersonId(personId);
        teacherDataScopeService.restrictStudentIdForTeacher(personId);
        List<SocialRelation> sList = socialRelationRepository.findByStudentPersonId(personId);
        List<Map<String, Object>> dataList = new ArrayList<>();
        if (sList != null) {
            for (SocialRelation s : sList) {
                Map<String, Object> m = new HashMap<>();
                m.put("relationId", s.getRelationId());
                m.put("personId", s.getStudent().getPersonId());
                m.put("relationType", s.getRelationType());
                m.put("name", s.getName());
                m.put("gender", s.getGender());
                m.put("phone", s.getPhone());
                m.put("age", s.getAge() == null ? "" : s.getAge().toString());
                m.put("remark", s.getRemark());
                dataList.add(m);
            }
        }
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse socialRelationSave(DataRequest dataRequest) {
        Map<String,Object> form = dataRequest.getMap("form");
        Integer personId = CommonMethod.getInteger(form,"personId");
        Integer relationId = CommonMethod.getInteger(form,"relationId");
        personId = resolveOwnedStudentPersonId(personId);
        teacherDataScopeService.restrictStudentIdForTeacher(personId);
        Optional<SocialRelation> op;
        SocialRelation s = null;
        if(relationId != null) {
            op = socialRelationRepository.findById(relationId);
            if(op.isPresent()) {
                s = op.get();
                assertStudentOwnsRecord(s.getStudent() == null ? null : s.getStudent().getPersonId());
                teacherDataScopeService.assertCurrentTeacherAccessStudent(
                        s.getStudent() == null ? null : s.getStudent().getPersonId(),
                        "教师仅可维护本人授课学生的社会关系信息");
                if (personId == null && s.getStudent() != null) {
                    personId = s.getStudent().getPersonId();
                }
            }
        }
        if(s== null) {
            s = new SocialRelation();
            if (personId == null || personId <= 0) {
                throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "学生ID不能为空");
            }
            Student student = studentRepository.findById(personId)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "学生不存在"));
            s.setStudent(student);
        }
        s.setRelationType(CommonMethod.getString(form,"relationType"));
        s.setName(CommonMethod.getString(form,"name"));
        s.setGender(CommonMethod.getString(form,"gender"));
        s.setPhone(CommonMethod.getString(form,"phone"));
        s.setAge(CommonMethod.getInteger(form,"age"));
        s.setRemark(CommonMethod.getString(form,"remark"));
        socialRelationRepository.save(s);
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse socialRelationDelete(DataRequest dataRequest) {
        Integer relationId = dataRequest.getInteger("relationId");
        Optional<SocialRelation> op;
        op = socialRelationRepository.findById(relationId);
        if (op.isPresent()) {
            SocialRelation socialRelation = op.get();
            assertStudentOwnsRecord(socialRelation.getStudent() == null ? null : socialRelation.getStudent().getPersonId());
            teacherDataScopeService.assertCurrentTeacherAccessStudent(
                    socialRelation.getStudent() == null ? null : socialRelation.getStudent().getPersonId(),
                    "教师仅可删除本人授课学生的社会关系信息");
            socialRelationRepository.delete(socialRelation);
        }
        return CommonMethod.getReturnMessageOK();
    }

    private Integer resolveOwnedStudentPersonId(Integer personId) {
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            Integer currentPersonId = CommonMethod.getPersonId();
            if (currentPersonId == null || currentPersonId <= 0) {
                throw new BusinessException(ErrorCodes.AUTH_FAILED, "当前登录状态无效，请重新登录");
            }
            return currentPersonId;
        }
        return personId;
    }

    private void assertStudentOwnsRecord(Integer ownerPersonId) {
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            Integer currentPersonId = CommonMethod.getPersonId();
            if (currentPersonId == null || !currentPersonId.equals(ownerPersonId)) {
                throw new BusinessException(ErrorCodes.ACCESS_DENIED, "学生只能操作自己的数据");
            }
        }
    }


    public DataResponse importFeeDataWeb(Map<String,Object> request,MultipartFile file) {
        Integer personId = CommonMethod.getInteger(request, "personId");
        try {
            Map<String, Object> result = importFeeData(personId, file.getInputStream());
            return CommonMethod.getReturnData(result);
        } catch (Exception e) {
            log.error(e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("successCount", 0);
            errorResult.put("failCount", 0);
            List<String> failReasons = new ArrayList<>();
            failReasons.add("上传错误: " + e.getMessage());
            errorResult.put("failReasons", failReasons);
            return CommonMethod.getReturnData(errorResult);
        }
    }

    public DataResponse getStudentIntroduceData(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        Optional<Student> sOp;
        if(personId == null || personId <= 0) {
            String username = CommonMethod.getUsername();
            sOp = studentRepository.findByPersonNum(username);  // 查询获得 Student对象
        }else {
            sOp = studentRepository.findById(personId);  // 根据personId查询获得 Student对象
        }
        if (sOp.isEmpty())
            throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "学生不存在");
        Student s = sOp.get();
        Map<String,Object> info = getMapFromStudent(s);  // 查询学生信息Map对象
        List<Score> sList = scoreRepository.findByStudentPersonId(s.getPersonId()); //获得学生成绩对象集合
        Map<String,Object> data = new HashMap<>();
        data.put("info", info);
        data.put("scoreList", getStudentScoreList(sList));
        data.put("markList", getStudentMarkList(sList));
        data.put("feeList", getStudentFeeList(s.getPersonId()));
        return CommonMethod.getReturnData(data);//将前端所需数据保留Map对象里，返还前端
    }

    /**
     * 获取学生个人画像数据(聚合基本信息、成绩、考勤、实践荣誉等)
     */
    public DataResponse getStudentPortrait(DataRequest dataRequest) {
        Integer studentId = dataRequest.getInteger("studentId");
        
        // 如果未传studentId,使用当前登录用户
        if (studentId == null || studentId <= 0) {
            String username = CommonMethod.getUsername();
            Optional<Student> sOp = studentRepository.findByPersonNum(username);
            if (sOp.isPresent()) {
                studentId = sOp.get().getPersonId();
            }
        }
        
        if (studentId == null || studentId <= 0) {
            throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "学生ID不能为空");
        }
        teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, "教师仅可查看本人授课学生的画像数据");
        
        Map<String, Object> portrait = new HashMap<>();
        
        // 1. 基本信息
        Optional<Student> sOp = studentRepository.findById(studentId);
        if (sOp.isPresent()) {
            Student student = sOp.get();
            portrait.put("basicInfo", getMapFromStudent(student));
        } else {
            throw new BusinessException(ErrorCodes.STUDENT_NOT_FOUND, "学生不存在");
        }
        
        // 2. 成绩雷达图数据(按课程统计平均分)
        List<Score> scoreList = scoreRepository.findByStudentPersonId(studentId);
        List<Map<String, Object>> scoreRadar = new ArrayList<>();
        if (scoreList != null && !scoreList.isEmpty()) {
            for (Score score : scoreList) {
                Map<String, Object> scoreItem = new HashMap<>();
                scoreItem.put("courseName", score.getCourse() != null ? score.getCourse().getName() : "未知课程");
                scoreItem.put("score", score.getMark());
                scoreRadar.add(scoreItem);
            }
        }
        portrait.put("scoreRadar", scoreRadar);
        
        // 3. 考勤统计（已废弃，考勤功能未实现，返回空数据）
        portrait.put("attendanceStats", new HashMap<>());
        
        // 4. 实践荣誉统计(按类型统计)
        List<StudentDevelopment> developmentList = developmentRepository.findByStudentId(studentId);
        Map<String, Object> developmentStats = new HashMap<>();
        int honorCount = 0, innovationCount = 0, competitionCount = 0, achievementCount = 0;
        int approvedCount = 0;
        
        if (developmentList != null) {
            for (StudentDevelopment dev : developmentList) {
                String type = dev.getDevelopmentType();
                if ("honor".equals(type)) {
                    honorCount++;
                } else if ("innovation".equals(type)) {
                    innovationCount++;
                } else if ("competition".equals(type)) {
                    competitionCount++;
                } else if ("achievement".equals(type)) {
                    achievementCount++;
                }
                
                if ("approved".equals(dev.getStatus())) {
                    approvedCount++;
                }
            }
        }
        
        developmentStats.put("total", developmentList != null ? developmentList.size() : 0);
        developmentStats.put("honorCount", honorCount);
        developmentStats.put("innovationCount", innovationCount);
        developmentStats.put("competitionCount", competitionCount);
        developmentStats.put("achievementCount", achievementCount);
        developmentStats.put("approvedCount", approvedCount);
        
        // 计算荣誉得分（按级别加分，上限100）- 从honor表查询，与绩分计算面板一致
        double honorScore = 0.0;
        List<Honor> honorList = honorRepository.findByStudentId(studentId);
        if (honorList != null) {
            for (Honor honor : honorList) {
                if ("approved".equals(honor.getStatus())) {
                    String level = honor.getHonorLevel();
                    if (level != null) {
                        switch (level) {
                            case "国家级": honorScore += 50; break;
                            case "省级": honorScore += 30; break;
                            case "校级": honorScore += 20; break;
                            case "院级": honorScore += 10; break;
                        }
                    }
                }
            }
        }
        honorScore = Math.min(honorScore, 100.0);
        developmentStats.put("honorScore", honorScore);
        
        portrait.put("developmentStats", developmentStats);
        
        // 5. 添加计算好的四维分数和权重（与绩分计算面板一致）
        // 成绩分数（按学分加权平均）
        double courseScore = 0.0;
        if (scoreList != null && !scoreList.isEmpty()) {
            double totalScore = 0.0;
            double totalCredit = 0.0;
            for (Score score : scoreList) {
                if (score.getMark() != null && score.getCourse() != null && score.getCourse().getCredit() != null) {
                    totalScore += score.getMark().doubleValue() * score.getCourse().getCredit();
                    totalCredit += score.getCourse().getCredit();
                }
            }
            if (totalCredit > 0) {
                courseScore = totalScore / totalCredit;
            }
        }
        
        // 实践分数（从社会实践板块计算：日常活动/培训讲座/校外实习/志愿服务）
        List<DailyActivity> activityList = dailyActivityRepository.findByStudentId(studentId);
        long practiceApprovedCount = 0;
        if (activityList != null) {
            practiceApprovedCount = activityList.stream()
                    .filter(a -> "approved".equals(a.getStatus()))
                    .filter(a -> {
                        String type = a.getActivityType();
                        return "daily_activity".equals(type)
                                || "training".equals(type)
                                || "internship".equals(type)
                                || "volunteer".equals(type);
                    })
                    .count();
        }
        double practiceScore = Math.min(practiceApprovedCount * 20.0, 100.0);
        
        // 创新分数（从创新创业板块计算：创业实践/学科竞赛/科研成果）
        double innovationScore = 0.0;
        if (developmentList != null) {
            long innovationApprovedCount = developmentList.stream()
                    .filter(d -> "approved".equals(d.getStatus()))
                    .filter(d -> {
                        String type = d.getDevelopmentType();
                        return "innovation".equals(type)
                                || "competition".equals(type)
                                || "achievement".equals(type);
                    })
                    .count();
            innovationScore = Math.min(innovationApprovedCount * 20.0, 100.0);
        }
        
        // 默认权重配置（成绩80%、实践5%、创新5%、荣誉10%）
        Map<String, Object> weights = new HashMap<>();
        weights.put("scoreWeight", 0.8);
        weights.put("attendanceWeight", 0.05);  // attendanceWeight现在存储实践权重
        weights.put("practiceWeight", 0.05);    // practiceWeight现在存储创新权重
        weights.put("honorWeight", 0.1);
        
        Map<String, Object> calculatedScores = new HashMap<>();
        calculatedScores.put("scoreValue", Math.round(courseScore * 100.0) / 100.0);
        calculatedScores.put("attendanceValue", Math.round(practiceScore * 100.0) / 100.0);  // attendanceValue存储实践分数
        calculatedScores.put("practiceValue", Math.round(innovationScore * 100.0) / 100.0);  // practiceValue存储创新分数
        calculatedScores.put("honorValue", Math.round(honorScore * 100.0) / 100.0);
        calculatedScores.put("weights", weights);
        
        portrait.put("calculatedScores", calculatedScores);
        
        // 5. 消费趋势(最近6个月)
        List<Fee> feeList = feeRepository.findListByStudent(studentId);
        List<Map<String, Object>> consumptionTrend = new ArrayList<>();
        if (feeList != null && !feeList.isEmpty()) {
            // 简单按月统计(实际应该用SQL GROUP BY)
            Map<String, Double> monthMap = new LinkedHashMap<>();
            for (Fee fee : feeList) {
                if (fee.getDay() != null && !fee.getDay().isEmpty()) {
                    String month = fee.getDay().substring(0, 7); // yyyy-MM
                    monthMap.merge(month, fee.getMoney(), Double::sum);
                }
            }
            for (Map.Entry<String, Double> entry : monthMap.entrySet()) {
                Map<String, Object> trendItem = new HashMap<>();
                trendItem.put("month", entry.getKey());
                trendItem.put("amount", entry.getValue());
                consumptionTrend.add(trendItem);
            }
        }
        portrait.put("consumptionTrend", consumptionTrend);
        
        return CommonMethod.getReturnData(portrait);
    }
}
