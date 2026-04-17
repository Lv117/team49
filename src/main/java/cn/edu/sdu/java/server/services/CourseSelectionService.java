package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.CourseSelection;
import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseSelectionRepository;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * CourseSelection 选课服务类
 */
@Service
public class CourseSelectionService {
    private static final Logger log = LoggerFactory.getLogger(CourseSelectionService.class);

    private final CourseSelectionRepository courseSelectionRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    public CourseSelectionService(CourseSelectionRepository courseSelectionRepository, 
                                  CourseRepository courseRepository, 
                                  StudentRepository studentRepository) {
        this.courseSelectionRepository = courseSelectionRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * 获取选课列表
     */
    public DataResponse getCourseSelectionList(DataRequest dataRequest) {
        try {
            Integer studentId = dataRequest.getInteger("studentId");
            Integer courseId = dataRequest.getInteger("courseId");
            String status = dataRequest.getString("status");
            List<CourseSelection> selectionList;

            if (studentId != null && courseId != null) {
                if (status != null) {
                    selectionList = courseSelectionRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId);
                } else {
                    selectionList = courseSelectionRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId);
                }
            } else if (studentId != null) {
                if (status != null) {
                    selectionList = courseSelectionRepository.findByStudentPersonIdAndStatus(studentId, status);
                } else {
                    selectionList = courseSelectionRepository.findByStudentPersonId(studentId);
                }
            } else if (courseId != null) {
                if (status != null) {
                    selectionList = courseSelectionRepository.findByCourseCourseIdAndStatus(courseId, status);
                } else {
                    selectionList = courseSelectionRepository.findByCourseCourseId(courseId);
                }
            } else {
                selectionList = courseSelectionRepository.findAll();
            }

            List<Map<String, Object>> list = new ArrayList<>();
            for (CourseSelection cs : selectionList) {
                Map<String, Object> m = new HashMap<>();
                m.put("selectionId", cs.getSelectionId());
                m.put("studentId", cs.getStudent().getPersonId());
                m.put("studentName", cs.getStudent().getPerson().getName());
                m.put("courseId", cs.getCourse().getCourseId());
                m.put("courseName", cs.getCourse().getName());
                m.put("status", cs.getStatus());
                m.put("statusName", getStatusName(cs.getStatus()));
                m.put("score", cs.getScore());
                m.put("ranking", cs.getRanking());
                m.put("selectionTime", cs.getSelectionTime());
                m.put("remark", cs.getRemark());
                list.add(m);
            }

            return CommonMethod.getReturnData(list);
        } catch (Exception e) {
            log.error("获取选课列表失败", e);
            return CommonMethod.getReturnMessageError("获取选课列表失败：" + e.getMessage());
        }
    }

    /**
     * 学生选课
     */
    public DataResponse selectCourse(DataRequest dataRequest) {
        try {
            Integer studentId = dataRequest.getInteger("studentId");
            Integer courseId = dataRequest.getInteger("courseId");

            // 验证必填参数
            if (studentId == null) {
                return CommonMethod.getReturnMessageError("学生ID不能为空");
            }
            if (courseId == null) {
                return CommonMethod.getReturnMessageError("课程ID不能为空");
            }

            // 检查学生是否存在
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                return CommonMethod.getReturnMessageError("学生不存在");
            }

            // 检查课程是否存在
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                return CommonMethod.getReturnMessageError("课程不存在");
            }

            // 检查是否已选
            List<CourseSelection> existSelections = courseSelectionRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId);
            if (!existSelections.isEmpty()) {
                // 如果是已退课状态，允许重新选
                CourseSelection existSelection = existSelections.get(0);
                if ("已退课".equals(existSelection.getStatus())) {
                    existSelection.setStatus("已选");
                    courseSelectionRepository.save(existSelection);
                    return CommonMethod.getReturnMessageOK("重新选课成功");
                }
                return CommonMethod.getReturnMessageError("该课程已选，不能重复选课");
            }

            CourseSelection selection = new CourseSelection();
            selection.setStudent(student);
            selection.setCourse(course);
            selection.setStatus("已选");

            courseSelectionRepository.save(selection);

            return CommonMethod.getReturnMessageOK("选课成功");
        } catch (Exception e) {
            log.error("学生选课失败", e);
            return CommonMethod.getReturnMessageError("选课失败：" + e.getMessage());
        }
    }

    /**
     * 退课
     */
    public DataResponse dropCourse(DataRequest dataRequest) {
        try {
            Integer studentId = dataRequest.getInteger("studentId");
            Integer courseId = dataRequest.getInteger("courseId");

            List<CourseSelection> selectionList = courseSelectionRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId);
            if (selectionList.isEmpty()) {
                return CommonMethod.getReturnMessageError("未找到选课记录");
            }

            CourseSelection selection = selectionList.get(0);
            if ("已完成".equals(selection.getStatus())) {
                return CommonMethod.getReturnMessageError("课程已完成，不能退课");
            }

            selection.setStatus("已退课");
            selection.setRemark("退课时间：" + LocalDateTime.now());

            courseSelectionRepository.save(selection);

            return CommonMethod.getReturnMessageOK("退课成功");
        } catch (Exception e) {
            log.error("退课失败", e);
            return CommonMethod.getReturnMessageError("退课失败：" + e.getMessage());
        }
    }

    /**
     * 批量导入选课
     */
    public DataResponse batchSelectCourse(DataRequest dataRequest) {
        try {
            List<?> dataList = dataRequest.getList("data");
            if (dataList == null || dataList.isEmpty()) {
                return CommonMethod.getReturnMessageError("数据为空");
            }

            int successCount = 0;
            for (Object obj : dataList) {
                try {
                    Map<String, Object> data = (Map<String, Object>) obj;
                    Integer studentId = (Integer) data.get("studentId");
                    Integer courseId = (Integer) data.get("courseId");

                    // 检查是否已选
                    List<CourseSelection> existSelections = courseSelectionRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId);
                    if (!existSelections.isEmpty()) {
                        continue;
                    }

                    Student student = studentRepository.findById(studentId).orElse(null);
                    if (student == null) continue;

                    Course course = courseRepository.findById(courseId).orElse(null);
                    if (course == null) continue;

                    CourseSelection selection = new CourseSelection();
                    selection.setStudent(student);
                    selection.setCourse(course);
                    selection.setStatus("已选");

                    courseSelectionRepository.save(selection);
                    successCount++;
                } catch (Exception e) {
                    log.warn("导入单条选课记录失败：" + e.getMessage());
                }
            }

            return CommonMethod.getReturnMessageOK("批量导入成功，成功 " + successCount + " 条");
        } catch (Exception e) {
            log.error("批量导入选课失败", e);
            return CommonMethod.getReturnMessageError("批量导入失败：" + e.getMessage());
        }
    }

    /**
     * 删除选课记录
     */
    public DataResponse courseSelectionDelete(DataRequest dataRequest) {
        try {
            Integer selectionId = dataRequest.getInteger("selectionId");
            if (selectionId == null) {
                return CommonMethod.getReturnMessageError("选课记录 ID 不能为空");
            }

            if (!courseSelectionRepository.existsById(selectionId)) {
                return CommonMethod.getReturnMessageError("选课记录不存在");
            }

            courseSelectionRepository.deleteById(selectionId);

            return CommonMethod.getReturnMessageOK("删除成功");
        } catch (Exception e) {
            log.error("删除选课记录失败", e);
            return CommonMethod.getReturnMessageError("删除选课记录失败：" + e.getMessage());
        }
    }

    /**
     * 获取选课统计
     * 按照对接规范返回：{"total": 50, "已选": 45, "已退课": 3, "已完成": 2}
     */
    public DataResponse getCourseSelectionStatistics(DataRequest dataRequest) {
        try {
            Integer courseId = dataRequest.getInteger("courseId");
            
            // 验证参数
            if (courseId == null) {
                return CommonMethod.getReturnMessageError("请提供courseId参数");
            }

            // 检查课程是否存在
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                return CommonMethod.getReturnMessageError("课程不存在");
            }

            // 统计各状态数量
            long totalCount = courseSelectionRepository.countByCourseCourseId(courseId);
            List<CourseSelection> selectedList = courseSelectionRepository.findByCourseCourseIdAndStatus(courseId, "已选");
            List<CourseSelection> completedList = courseSelectionRepository.findByCourseCourseIdAndStatus(courseId, "已完成");
            List<CourseSelection> droppedList = courseSelectionRepository.findByCourseCourseIdAndStatus(courseId, "已退课");

            // 按照对接规范格式返回（使用中文字段名）
            Map<String, Object> result = new HashMap<>();
            result.put("total", totalCount);
            result.put("已选", selectedList.size());
            result.put("已退课", droppedList.size());
            result.put("已完成", completedList.size());

            return CommonMethod.getReturnData(result);
        } catch (Exception e) {
            log.error("获取选课统计失败", e);
            return CommonMethod.getReturnMessageError("获取选课统计失败：" + e.getMessage());
        }
    }

    /**
     * 状态值转换
     */
    private String getStatusName(String status) {
        if (status == null) return "";
        return switch (status) {
            case "已选" -> "已选";
            case "已退课" -> "已退课";
            case "已完成" -> "已完成";
            default -> status;
        };
    }
}
