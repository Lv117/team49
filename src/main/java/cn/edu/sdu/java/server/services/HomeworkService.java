package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.Homework;
import cn.edu.sdu.java.server.models.HomeworkSubmission;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.HomeworkRepository;
import cn.edu.sdu.java.server.repositorys.HomeworkSubmissionRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

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

    public HomeworkService(HomeworkRepository homeworkRepository, 
                          HomeworkSubmissionRepository homeworkSubmissionRepository, 
                          StudentRepository studentRepository,
                          CourseRepository courseRepository) {
        this.homeworkRepository = homeworkRepository;
        this.homeworkSubmissionRepository = homeworkSubmissionRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * 获取作业列表
     */
    public DataResponse getHomeworkList(DataRequest dataRequest) {
        try {
            Integer courseId = dataRequest.getInteger("courseId");
            List<Homework> homeworkList;

            if (courseId != null) {
                homeworkList = homeworkRepository.findByCourseCourseIdOrderByCreateTimeDesc(courseId);
            } else {
                homeworkList = homeworkRepository.findAll();
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

            Homework homework;
            if (homeworkId != null) {
                homework = homeworkRepository.findById(homeworkId).orElse(null);
                if (homework == null) {
                    return CommonMethod.getReturnMessageError("作业不存在");
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
    public DataResponse homeworkDelete(DataRequest dataRequest) {
        try {
            Integer homeworkId = dataRequest.getInteger("homeworkId");
            if (homeworkId == null) {
                return CommonMethod.getReturnMessageError("作业 ID 不能为空");
            }

            if (!homeworkRepository.existsById(homeworkId)) {
                return CommonMethod.getReturnMessageError("作业不存在");
            }

            homeworkRepository.deleteById(homeworkId);

            return CommonMethod.getReturnMessageOK("删除成功");
        } catch (Exception e) {
            log.error("删除作业失败", e);
            return CommonMethod.getReturnMessageError("删除作业失败：" + e.getMessage());
        }
    }

    /**
     * 获取作业提交列表
     */
    public DataResponse getSubmissionList(DataRequest dataRequest) {
        try {
            Integer homeworkId = dataRequest.getInteger("homeworkId");
            Integer studentId = dataRequest.getInteger("studentId");
            List<HomeworkSubmission> submissionList;

            if (homeworkId != null && studentId != null) {
                submissionList = homeworkSubmissionRepository.findByHomeworkHomeworkIdAndStudentPersonId(homeworkId, studentId);
            } else if (homeworkId != null) {
                submissionList = homeworkSubmissionRepository.findByHomeworkHomeworkId(homeworkId);
            } else if (studentId != null) {
                submissionList = homeworkSubmissionRepository.findByStudentPersonIdOrderBySubmitTimeDesc(studentId);
            } else {
                submissionList = homeworkSubmissionRepository.findAll();
            }

            List<Map<String, Object>> list = new ArrayList<>();
            for (HomeworkSubmission s : submissionList) {
                Map<String, Object> m = new HashMap<>();
                m.put("submissionId", s.getSubmissionId());
                m.put("homeworkId", s.getHomework().getHomeworkId());
                m.put("homeworkTitle", s.getHomework().getTitle());
                m.put("studentId", s.getStudent().getPersonId());
                m.put("studentName", s.getStudent().getPerson().getName());
                m.put("content", s.getContent());
                m.put("attachmentUrl", s.getAttachmentUrl());
                m.put("submitTime", s.getSubmitTime());
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
