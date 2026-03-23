package cn.edu.sdu.java.server.services;

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

            Homework homework;
            if (homeworkId != null) {
                homework = homeworkRepository.findById(homeworkId).orElse(null);
                if (homework == null) {
                    return CommonMethod.getReturnMessageError("作业不存在");
                }
            } else {
                homework = new Homework();
            }

            homework.setCourse(courseRepository.findById(courseId).orElse(null));
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

            Homework homework = homeworkRepository.findById(homeworkId).orElse(null);
            if (homework == null) {
                return CommonMethod.getReturnMessageError("作业不存在");
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
            BigDecimal score = scoreStr != null ? new BigDecimal(scoreStr) : null;
            String comment = dataRequest.getString("comment");

            HomeworkSubmission submission = homeworkSubmissionRepository.findById(submissionId).orElse(null);
            if (submission == null) {
                return CommonMethod.getReturnMessageError("提交记录不存在");
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
     */
    public DataResponse getHomeworkStatistics(DataRequest dataRequest) {
        try {
            Integer courseId = dataRequest.getInteger("courseId");
            List<Homework> homeworkList = homeworkRepository.findByCourseCourseId(courseId);

            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> homeworkStats = new ArrayList<>();

            for (Homework h : homeworkList) {
                Map<String, Object> m = new HashMap<>();
                m.put("homeworkId", h.getHomeworkId());
                m.put("title", h.getTitle());
                long submitCount = homeworkSubmissionRepository.countByHomeworkHomeworkId(h.getHomeworkId());
                long gradedCount = homeworkSubmissionRepository.countByHomeworkHomeworkIdAndStatus(h.getHomeworkId(), "已批改");
                m.put("submitCount", submitCount);
                m.put("gradedCount", gradedCount);
                m.put("maxScore", h.getMaxScore());
                homeworkStats.add(m);
            }

            result.put("homeworkList", homeworkStats);
            return CommonMethod.getReturnData(result);
        } catch (Exception e) {
            log.error("获取作业统计失败", e);
            return CommonMethod.getReturnMessageError("获取作业统计失败：" + e.getMessage());
        }
    }
}
