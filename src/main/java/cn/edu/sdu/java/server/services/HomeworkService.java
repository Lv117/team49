package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.CourseSelection;
import cn.edu.sdu.java.server.models.Homework;
import cn.edu.sdu.java.server.models.HomeworkSubmission;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.CourseSelectionRepository;
import cn.edu.sdu.java.server.repositorys.HomeworkRepository;
import cn.edu.sdu.java.server.repositorys.HomeworkSubmissionRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Arrays;

/**
 * Homework 作业服务类
 */
@Service
public class HomeworkService {
    private static final Logger log = LoggerFactory.getLogger(HomeworkService.class);

    private final HomeworkRepository homeworkRepository;
    private final HomeworkSubmissionRepository homeworkSubmissionRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final CourseSelectionRepository courseSelectionRepository;
    private final TeacherDataScopeService teacherDataScopeService;

    public HomeworkService(HomeworkRepository homeworkRepository, 
                          HomeworkSubmissionRepository homeworkSubmissionRepository, 
                          StudentRepository studentRepository,
                          CourseRepository courseRepository,
                          CourseSelectionRepository courseSelectionRepository,
                          TeacherDataScopeService teacherDataScopeService) {
        this.homeworkRepository = homeworkRepository;
        this.homeworkSubmissionRepository = homeworkSubmissionRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.courseSelectionRepository = courseSelectionRepository;
        this.teacherDataScopeService = teacherDataScopeService;
    }

    /**
     * 获取作业列表
     */
    public DataResponse getHomeworkList(DataRequest dataRequest) {
        try {
            Integer courseId = dataRequest.getInteger("courseId");
            String role = CommonMethod.getRoleName();
            Integer currentPersonId = CommonMethod.getPersonId();
            String username = CommonMethod.getUsername();
            
            System.out.println("[HOMEWORK DEBUG] ====================");
            System.out.println("[HOMEWORK DEBUG] 查询作业列表");
            System.out.println("[HOMEWORK DEBUG] 用户名: " + username);
            System.out.println("[HOMEWORK DEBUG] 角色: " + role);
            System.out.println("[HOMEWORK DEBUG] personId: " + currentPersonId);
            System.out.println("[HOMEWORK DEBUG] courseId参数: " + courseId);
            System.out.println("[HOMEWORK DEBUG] ====================");
            
            List<Homework> homeworkList;

            // 根据角色过滤作业
            if ("ROLE_STUDENT".equals(role)) {
                // 学生只能查看自己已选课程的作业
                if (currentPersonId == null) {
                    log.warn("学生权限验证失败：currentPersonId为null");
                    return CommonMethod.getReturnMessageError("登录状态无效，请重新登录");
                }
                            
                log.info("学生[{}]查询作业列表，currentPersonId={}", role, currentPersonId);
                            
                // 获取学生已选的课程ID列表，只包括状态为"已选"或"已完成"的课程
                List<CourseSelection> selections = courseSelectionRepository.findByStudentPersonIdAndStatusIn(currentPersonId, Arrays.asList("已选", "已完成"));
                log.info("学生[{}]已选课程数量：{}", currentPersonId, selections.size());
                            
                Set<Integer> selectedCourseIds = new HashSet<>();
                for (CourseSelection cs : selections) {
                    selectedCourseIds.add(cs.getCourse().getCourseId());
                    log.info("学生[{}]已选课程：courseId={}, courseName={}, status={}", 
                        currentPersonId, cs.getCourse().getCourseId(), cs.getCourse().getName(), cs.getStatus());
                }
                
                if (selectedCourseIds.isEmpty()) {
                    // 学生没有选任何课程，返回空列表
                    return CommonMethod.getReturnData(new ArrayList<>());
                }
                
                // 如果指定了courseId，验证是否在该学生已选课程中
                if (courseId != null) {
                    if (!selectedCourseIds.contains(courseId)) {
                        return CommonMethod.getReturnMessageError("您未选择该课程，无法查看作业");
                    }
                    homeworkList = homeworkRepository.findByCourseCourseIdOrderByCreateTimeDesc(courseId);
                } else {
                    // 获取所有已选课程的作业
                    homeworkList = new ArrayList<>();
                    for (Integer cid : selectedCourseIds) {
                        homeworkList.addAll(homeworkRepository.findByCourseCourseIdOrderByCreateTimeDesc(cid));
                    }
                }
            } else if ("ROLE_TEACHER".equals(role)) {
                // 教师只能查看自己授课课程的作业
                Set<Integer> teacherCourseIds = teacherDataScopeService.getCurrentTeacherCourseIds();
                
                if (teacherCourseIds.isEmpty()) {
                    // 教师没有授课课程，返回空列表
                    return CommonMethod.getReturnData(new ArrayList<>());
                }
                
                // 如果指定了courseId，验证是否在该教师授课课程中
                if (courseId != null) {
                    if (!teacherCourseIds.contains(courseId)) {
                        return CommonMethod.getReturnMessageError("您未教授该课程，无法查看作业");
                    }
                    homeworkList = homeworkRepository.findByCourseCourseIdOrderByCreateTimeDesc(courseId);
                } else {
                    // 获取所有授课课程的作业
                    homeworkList = new ArrayList<>();
                    for (Integer cid : teacherCourseIds) {
                        homeworkList.addAll(homeworkRepository.findByCourseCourseIdOrderByCreateTimeDesc(cid));
                    }
                }
            } else {
                // 管理员可以查看所有作业
                if (courseId != null) {
                    homeworkList = homeworkRepository.findByCourseCourseIdOrderByCreateTimeDesc(courseId);
                } else {
                    homeworkList = homeworkRepository.findAll();
                }
            }

            List<Map<String, Object>> list = new ArrayList<>();
            for (Homework h : homeworkList) {
                Map<String, Object> m = new HashMap<>();
                m.put("homeworkId", h.getHomeworkId());
                m.put("courseId", h.getCourse().getCourseId());
                m.put("courseName", h.getCourse().getName());
                m.put("title", h.getTitle());
                m.put("content", h.getContent());
                m.put("deadline", h.getDeadline());
                m.put("maxScore", h.getMaxScore());
                m.put("attachmentUrl", h.getAttachmentUrl());
                m.put("createTime", h.getCreateTime());
                
                // 统计提交情况
                long submitCount = homeworkSubmissionRepository.countByHomeworkHomeworkId(h.getHomeworkId());
                long gradedCount = homeworkSubmissionRepository.countByHomeworkHomeworkIdAndStatus(h.getHomeworkId(), "已批改");
                m.put("submitCount", submitCount);
                m.put("gradedCount", gradedCount);
                
                list.add(m);
            }

            return CommonMethod.getReturnData(list);
        } catch (Exception e) {
            log.error("获取作业列表失败", e);
            return CommonMethod.getReturnMessageError("获取作业列表失败：" + e.getMessage());
        }
    }

    /**
     * 保存作业
     */
    public DataResponse homeworkSave(DataRequest dataRequest) {
        try {
            Integer homeworkId = dataRequest.getInteger("homeworkId");
            Integer courseId = dataRequest.getInteger("courseId");
            String title = dataRequest.getString("title");
            String content = dataRequest.getString("content");
            String deadline = dataRequest.getString("deadline");
            Integer maxScore = dataRequest.getInteger("maxScore");
            String attachmentUrl = dataRequest.getString("attachmentUrl");

            // 验证必填参数
            if (courseId == null) {
                return CommonMethod.getReturnMessageError("课程ID不能为空");
            }
            if (title == null || title.trim().isEmpty()) {
                return CommonMethod.getReturnMessageError("作业标题不能为空");
            }
            if (title.length() > 100) {
                return CommonMethod.getReturnMessageError("作业标题不能超过100个字符");
            }
            if (deadline == null || deadline.isEmpty()) {
                return CommonMethod.getReturnMessageError("截止时间不能为空");
            }
            if (maxScore == null || maxScore <= 0) {
                return CommonMethod.getReturnMessageError("满分分数必须大于0");
            }
            if (maxScore > 1000) {
                return CommonMethod.getReturnMessageError("满分分数不能超过1000");
            }

            // 验证日期格式
            try {
                LocalDateTime.parse(deadline);
            } catch (Exception e) {
                return CommonMethod.getReturnMessageError("日期格式错误，请使用 yyyy-MM-ddTHH:mm:ss 格式");
            }

            // 验证教师权限：教师只能布置自己授课课程的作业
            String role = CommonMethod.getRoleName();
            if ("ROLE_TEACHER".equals(role)) {
                teacherDataScopeService.assertCurrentTeacherAccessCourse(courseId, "您未教授该课程，无法布置作业");
            }

            Homework homework;
            if (homeworkId != null) {
                homework = homeworkRepository.findById(homeworkId).orElse(null);
                if (homework == null) {
                    return CommonMethod.getReturnMessageError("作业不存在");
                }
                // 编辑时也要验证权限
                if ("ROLE_TEACHER".equals(role)) {
                    teacherDataScopeService.assertCurrentTeacherAccessCourse(homework.getCourse().getCourseId(), "您无权编辑该作业");
                }
            } else {
                homework = new Homework();
            }

            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                return CommonMethod.getReturnMessageError("课程不存在");
            }

            homework.setCourse(course);
            homework.setTitle(title);
            homework.setContent(content);
            homework.setDeadline(LocalDateTime.parse(deadline));
            homework.setMaxScore(maxScore);
            homework.setAttachmentUrl(attachmentUrl);

            homeworkRepository.save(homework);

            return CommonMethod.getReturnMessageOK("保存成功");
        } catch (Exception e) {
            log.error("保存作业失败", e);
            return CommonMethod.getReturnMessageError("保存作业失败：" + e.getMessage());
        }
    }

    /**
     * 删除作业
     */
    @Transactional
    public DataResponse homeworkDelete(DataRequest dataRequest) {
        try {
            Integer homeworkId = dataRequest.getInteger("homeworkId");
            if (homeworkId == null) {
                return CommonMethod.getReturnMessageError("作业 ID 不能为空");
            }

            Homework homework = homeworkRepository.findById(homeworkId).orElse(null);
            if (homework == null) {
                return CommonMethod.getReturnMessageError("作业不存在");
            }

            // 验证教师权限：教师只能删除自己授课课程的作业
            String role = CommonMethod.getRoleName();
            if ("ROLE_TEACHER".equals(role)) {
                teacherDataScopeService.assertCurrentTeacherAccessCourse(homework.getCourse().getCourseId(), "您无权删除该作业");
            }

            // 先删除该作业的所有提交记录，再删除作业本身
            homeworkSubmissionRepository.deleteByHomeworkHomeworkId(homeworkId);
            homeworkRepository.deleteById(homeworkId);

            return CommonMethod.getReturnMessageOK("删除成功");
        } catch (Exception e) {
            log.error("删除作业失败", e);
            return CommonMethod.getReturnMessageError("删除作业失败：" + e.getMessage());
        }
    }

    /**
     * 删除学生作业提交记录
     */
    @Transactional
    public DataResponse submissionDelete(DataRequest dataRequest) {
        try {
            Integer submissionId = dataRequest.getInteger("submissionId");
            if (submissionId == null) {
                return CommonMethod.getReturnMessageError("提交记录 ID 不能为空");
            }

            HomeworkSubmission submission = homeworkSubmissionRepository.findById(submissionId).orElse(null);
            if (submission == null) {
                return CommonMethod.getReturnMessageError("提交记录不存在");
            }

            // 验证权限
            String role = CommonMethod.getRoleName();
            Integer currentPersonId = CommonMethod.getPersonId();
            
            if ("ROLE_STUDENT".equals(role)) {
                // 学生只能删除自己的提交记录
                if (currentPersonId == null || !currentPersonId.equals(submission.getStudent().getPersonId())) {
                    return CommonMethod.getReturnMessageError("您只能删除自己的提交记录");
                }
                
                // 验证学生是否已选该课程
                List<CourseSelection> selections = courseSelectionRepository.findByStudentPersonIdAndStatusIn(currentPersonId, Arrays.asList("已选", "已完成"));
                boolean hasSelected = selections.stream()
                    .anyMatch(cs -> cs.getCourse().getCourseId().equals(submission.getHomework().getCourse().getCourseId()));
                
                if (!hasSelected) {
                    return CommonMethod.getReturnMessageError("您未选择该课程，无法删除提交记录");
                }
            } else if ("ROLE_TEACHER".equals(role)) {
                // 教师只能删除自己授课课程的提交记录
                teacherDataScopeService.assertCurrentTeacherAccessCourse(
                    submission.getHomework().getCourse().getCourseId(), 
                    "您无权删除该课程的提交记录"
                );
            }
            // 管理员可以删除任何提交记录

            homeworkSubmissionRepository.deleteById(submissionId);

            return CommonMethod.getReturnMessageOK("删除成功");
        } catch (Exception e) {
            log.error("删除提交记录失败", e);
            return CommonMethod.getReturnMessageError("删除提交记录失败：" + e.getMessage());
        }
    }

    /**
     * 获取作业提交列表
     */
    public DataResponse getSubmissionList(DataRequest dataRequest) {
        try {
            Integer homeworkId = dataRequest.getInteger("homeworkId");
            Integer studentId = dataRequest.getInteger("studentId");
            String role = CommonMethod.getRoleName();
            Integer currentPersonId = CommonMethod.getPersonId();
            List<HomeworkSubmission> submissionList;

            // 根据角色进行权限控制
            if ("ROLE_STUDENT".equals(role)) {
                // 学生只能查看自己的提交记录
                if (currentPersonId == null) {
                    return CommonMethod.getReturnMessageError("登录状态无效，请重新登录");
                }
                
                if (studentId != null && !studentId.equals(currentPersonId)) {
                    return CommonMethod.getReturnMessageError("您只能查看自己的提交记录");
                }
                
                // 如果指定了homeworkId，验证学生是否已选该课程
                if (homeworkId != null) {
                    Homework homework = homeworkRepository.findById(homeworkId).orElse(null);
                    if (homework != null) {
                        List<CourseSelection> selections = courseSelectionRepository.findByStudentPersonIdAndStatusIn(currentPersonId, Arrays.asList("已选", "已完成"));
                        boolean hasSelected = selections.stream()
                            .anyMatch(cs -> cs.getCourse().getCourseId().equals(homework.getCourse().getCourseId()));
                        
                        if (!hasSelected) {
                            return CommonMethod.getReturnMessageError("您未选择该课程，无法查看提交记录");
                        }
                    }
                }
                
                // 学生只能查看自己的提交记录
                if (homeworkId != null) {
                    submissionList = homeworkSubmissionRepository.findByHomeworkHomeworkIdAndStudentPersonId(homeworkId, currentPersonId);
                } else {
                    submissionList = homeworkSubmissionRepository.findByStudentPersonIdOrderBySubmitTimeDesc(currentPersonId);
                }
            } else if ("ROLE_TEACHER".equals(role)) {
                // 教师只能查看自己授课课程的提交记录
                if (homeworkId != null) {
                    Homework homework = homeworkRepository.findById(homeworkId).orElse(null);
                    if (homework != null) {
                        teacherDataScopeService.assertCurrentTeacherAccessCourse(homework.getCourse().getCourseId(), "您未教授该课程，无法查看提交记录");
                    }
                    submissionList = homeworkSubmissionRepository.findByHomeworkHomeworkId(homeworkId);
                } else if (studentId != null) {
                    teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, "您无权查看该学生的提交记录");
                    submissionList = homeworkSubmissionRepository.findByStudentPersonIdOrderBySubmitTimeDesc(studentId);
                } else {
                    // 教师查看所有自己授课课程的提交记录
                    Set<Integer> teacherCourseIds = teacherDataScopeService.getCurrentTeacherCourseIds();
                    submissionList = new ArrayList<>();
                    for (Integer courseId : teacherCourseIds) {
                        List<Homework> homeworks = homeworkRepository.findByCourseCourseIdOrderByCreateTimeDesc(courseId);
                        for (Homework hw : homeworks) {
                            submissionList.addAll(homeworkSubmissionRepository.findByHomeworkHomeworkId(hw.getHomeworkId()));
                        }
                    }
                }
            } else {
                // 管理员可以查看所有提交记录
                if (homeworkId != null && studentId != null) {
                    submissionList = homeworkSubmissionRepository.findByHomeworkHomeworkIdAndStudentPersonId(homeworkId, studentId);
                } else if (homeworkId != null) {
                    submissionList = homeworkSubmissionRepository.findByHomeworkHomeworkId(homeworkId);
                } else if (studentId != null) {
                    submissionList = homeworkSubmissionRepository.findByStudentPersonIdOrderBySubmitTimeDesc(studentId);
                } else {
                    submissionList = homeworkSubmissionRepository.findAll();
                }
            }

            List<Map<String, Object>> list = new ArrayList<>();
            for (HomeworkSubmission s : submissionList) {
                Map<String, Object> m = new HashMap<>();
                m.put("submissionId", s.getSubmissionId());
                m.put("homeworkId", s.getHomework().getHomeworkId());
                m.put("homeworkTitle", s.getHomework().getTitle());
                m.put("studentId", s.getStudent().getPersonId());
                m.put("studentNum", s.getStudent().getPerson().getNum());
                m.put("studentName", s.getStudent().getPerson().getName());
                m.put("content", s.getContent());
                m.put("attachmentUrl", s.getAttachmentUrl());
                // 格式化提交时间，去除 T 和时间秒数
                m.put("submitTime", s.getSubmitTime() != null ? s.getSubmitTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                m.put("score", s.getScore());
                m.put("comment", s.getComment());
                m.put("status", s.getStatus());
                list.add(m);
            }

            return CommonMethod.getReturnData(list);
        } catch (Exception e) {
            log.error("获取提交列表失败", e);
            return CommonMethod.getReturnMessageError("获取提交列表失败：" + e.getMessage());
        }
    }

    /**
     * 提交作业
     */
    public DataResponse submitHomework(DataRequest dataRequest) {
        try {
            Integer homeworkId = dataRequest.getInteger("homeworkId");
            Integer studentId = dataRequest.getInteger("studentId");
            String content = dataRequest.getString("content");
            String attachmentUrl = dataRequest.getString("attachmentUrl");

            // 验证必填参数
            if (homeworkId == null) {
                return CommonMethod.getReturnMessageError("作业ID不能为空");
            }
            if (studentId == null) {
                return CommonMethod.getReturnMessageError("学生ID不能为空");
            }
            if (content == null || content.trim().isEmpty()) {
                return CommonMethod.getReturnMessageError("作业内容不能为空");
            }

            Homework homework = homeworkRepository.findById(homeworkId).orElse(null);
            if (homework == null) {
                return CommonMethod.getReturnMessageError("作业不存在");
            }

            // 验证学生权限：学生只能提交已选课程的作业
            String role = CommonMethod.getRoleName();
            if ("ROLE_STUDENT".equals(role)) {
                Integer currentPersonId = CommonMethod.getPersonId();
                if (currentPersonId == null || !currentPersonId.equals(studentId)) {
                    return CommonMethod.getReturnMessageError("您只能提交自己的作业");
                }
                
                // 验证学生是否已选该课程，只包括状态为"已选"或"已完成"的课程
                List<CourseSelection> selections = courseSelectionRepository.findByStudentPersonIdAndCourseCourseId(studentId, homework.getCourse().getCourseId());
                boolean hasSelected = selections.stream()
                    .anyMatch(cs -> "已选".equals(cs.getStatus()) || "已完成".equals(cs.getStatus()));
                
                if (!hasSelected) {
                    return CommonMethod.getReturnMessageError("您未选择该课程，无法提交作业");
                }
            }

            // 检查是否已过截止时间
            if (homework.getDeadline() != null && homework.getDeadline().isBefore(LocalDateTime.now())) {
                return CommonMethod.getReturnMessageError("该作业已超过截止时间，无法提交");
            }

            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                return CommonMethod.getReturnMessageError("学生不存在");
            }

            // 检查是否已提交
            List<HomeworkSubmission> existSubmissions = homeworkSubmissionRepository.findByHomeworkHomeworkIdAndStudentPersonId(homeworkId, studentId);
            if (!existSubmissions.isEmpty()) {
                return CommonMethod.getReturnMessageError("该作业已提交，不能重复提交");
            }

            HomeworkSubmission submission = new HomeworkSubmission();
            submission.setHomework(homework);
            submission.setStudent(student);
            submission.setContent(content);
            submission.setAttachmentUrl(attachmentUrl);
            submission.setStatus("已提交");

            homeworkSubmissionRepository.save(submission);

            return CommonMethod.getReturnMessageOK("提交成功");
        } catch (Exception e) {
            log.error("提交作业失败", e);
            return CommonMethod.getReturnMessageError("提交作业失败：" + e.getMessage());
        }
    }

    /**
     * 批改作业
     */
    public DataResponse gradeHomework(DataRequest dataRequest) {
        try {
            Integer submissionId = dataRequest.getInteger("submissionId");
            String scoreStr = dataRequest.getString("score");
            String comment = dataRequest.getString("comment");

            // 验证必填参数
            if (submissionId == null) {
                return CommonMethod.getReturnMessageError("提交记录ID不能为空");
            }
            if (scoreStr == null || scoreStr.trim().isEmpty()) {
                return CommonMethod.getReturnMessageError("分数不能为空");
            }

            // 验证分数格式
            BigDecimal score;
            try {
                score = new BigDecimal(scoreStr);
            } catch (Exception e) {
                return CommonMethod.getReturnMessageError("分数格式错误");
            }

            if (score.compareTo(BigDecimal.ZERO) < 0) {
                return CommonMethod.getReturnMessageError("分数不能为负数");
            }

            HomeworkSubmission submission = homeworkSubmissionRepository.findById(submissionId).orElse(null);
            if (submission == null) {
                return CommonMethod.getReturnMessageError("提交记录不存在");
            }

            // 验证教师权限：只有教师和管理员可以批改作业
            String role = CommonMethod.getRoleName();
            if ("ROLE_TEACHER".equals(role)) {
                teacherDataScopeService.assertCurrentTeacherAccessCourse(
                    submission.getHomework().getCourse().getCourseId(), 
                    "您无权批改该课程的作业"
                );
            } else if ("ROLE_STUDENT".equals(role)) {
                return CommonMethod.getReturnMessageError("学生无权批改作业");
            }

            // 检查分数是否超过满分
            if (submission.getHomework().getMaxScore() != null && score.compareTo(new BigDecimal(submission.getHomework().getMaxScore())) > 0) {
                return CommonMethod.getReturnMessageError("分数不能超过作业满分（" + submission.getHomework().getMaxScore() + "分）");
            }

            submission.setScore(score);
            submission.setComment(comment);
            submission.setStatus("已批改");

            homeworkSubmissionRepository.save(submission);

            return CommonMethod.getReturnMessageOK("批改成功");
        } catch (Exception e) {
            log.error("批改作业失败", e);
            return CommonMethod.getReturnMessageError("批改作业失败：" + e.getMessage());
        }
    }

    /**
     * 获取作业统计
     * 按照对接规范返回：{"total": 30, "已提交": 25, "已批改": 20, "已逾期": 5, "averageScore": 87.5}
     */
    public DataResponse getHomeworkStatistics(DataRequest dataRequest) {
        try {
            Integer homeworkId = dataRequest.getInteger("homeworkId");

            // 验证参数
            if (homeworkId == null) {
                return CommonMethod.getReturnMessageError("请提供homeworkId参数");
            }

            // 检查作业是否存在
            Homework homework = homeworkRepository.findById(homeworkId).orElse(null);
            if (homework == null) {
                return CommonMethod.getReturnMessageError("作业不存在");
            }

            // 获取该作业的所有提交记录
            List<HomeworkSubmission> submissions = homeworkSubmissionRepository.findByHomeworkHomeworkId(homeworkId);
            
            // 统计各状态数量
            long totalCount = homeworkSubmissionRepository.countByHomeworkHomeworkId(homeworkId);
            long 已提交 = homeworkSubmissionRepository.countByHomeworkHomeworkIdAndStatus(homeworkId, "已提交");
            long 已批改 = homeworkSubmissionRepository.countByHomeworkHomeworkIdAndStatus(homeworkId, "已批改");
            
            // 计算已逾期数量（已提交但未批改，且超过截止时间）
            LocalDateTime deadline = homework.getDeadline();
            long 已逾期 = 0;
            if (deadline != null) {
                已逾期 = submissions.stream()
                    .filter(s -> "已提交".equals(s.getStatus()) || "未提交".equals(s.getStatus()))
                    .filter(s -> deadline.isBefore(LocalDateTime.now()))
                    .count();
            }

            // 计算平均分
            double averageScore = submissions.stream()
                .filter(s -> s.getScore() != null)
                .mapToDouble(s -> s.getScore().doubleValue())
                .average()
                .orElse(0.0);

            Map<String, Object> result = new HashMap<>();
            result.put("total", totalCount);
            result.put("已提交", 已提交);
            result.put("已批改", 已批改);
            result.put("已逾期", 已逾期);
            result.put("averageScore", Math.round(averageScore * 10.0) / 10.0); // 保留一位小数

            return CommonMethod.getReturnData(result);
        } catch (Exception e) {
            log.error("获取作业统计失败", e);
            return CommonMethod.getReturnMessageError("获取作业统计失败：" + e.getMessage());
        }
    }
}
