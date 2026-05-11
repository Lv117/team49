package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.ConsumptionService;
import cn.edu.sdu.java.server.services.CourseMaterialService;
import cn.edu.sdu.java.server.services.ResumeService;
import cn.edu.sdu.java.server.services.ScoreCalculationService;
import cn.edu.sdu.java.server.services.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.*;

/**
 * StudentController 主要是为学生管理数据管理提供的Web请求服务
 */

// origins： 允许可访问的域列表
// maxAge:准备响应前的缓存持续的最大时间（以秒为单位）。
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/student")

public class StudentController {
    private final StudentService studentService;
    private final CourseMaterialService courseMaterialService;
    private final ConsumptionService consumptionService;
    private final ScoreCalculationService scoreCalculationService;
    private final ResumeService resumeService;
    
    public StudentController(StudentService studentService, 
                            CourseMaterialService courseMaterialService,
                            ConsumptionService consumptionService,
                            ScoreCalculationService scoreCalculationService,
                            ResumeService resumeService) {
        this.studentService = studentService;
        this.courseMaterialService = courseMaterialService;
        this.consumptionService = consumptionService;
        this.scoreCalculationService = scoreCalculationService;
        this.resumeService = resumeService;
    }

    /**
     * getStudentList 学生管理 点击查询按钮请求
     * 前台请求参数 numName 学号或名称的 查询串
     * 返回前端 存储学生信息的 MapList 框架会自动将Map转换程用于前后台传输数据的Json对象，Map的嵌套结构和Json的嵌套结构类似
     *
     */


    @PostMapping("/getStudentList")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse getStudentList(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.getStudentList(dataRequest);
    }


    /**
     * studentDelete 删除学生信息Web服务 Student页面的列表里点击删除按钮则可以删除已经存在的学生信息， 前端会将该记录的id 回传到后端，方法从参数获取id，查出相关记录，调用delete方法删除
     * 这里注意删除顺序，应为user关联person,Student关联Person 所以要先删除Student,User，再删除Person
     *
     * @param dataRequest 前端personId 要删除的学生的主键 person_id
     * @return 正常操作
     */

    @PostMapping("/studentDelete")
    public DataResponse studentDelete(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.studentDelete(dataRequest);
    }

    /**
     * getStudentInfo 前端点击学生列表时前端获取学生详细信息请求服务
     *
     * @param dataRequest 从前端获取 personId 查询学生信息的主键 person_id
     * @return 根据personId从数据库中查出数据，存在Map对象里，并返回前端
     */

    @PostMapping("/getStudentInfo")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse getStudentInfo(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.getStudentInfo(dataRequest);
    }

    @PostMapping("/getStudentByIds")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getStudentByIds(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.getStudentByIds(dataRequest);
    }

    /**
     * studentEditSave 前端学生信息提交服务
     * 前端把所有数据打包成一个Json对象作为参数传回后端，后端直接可以获得对应的Map对象form, 再从form里取出所有属性，复制到
     * 实体对象里，保存到数据库里即可，如果是添加一条记录， id 为空，这是先 new Person, User,Student 计算新的id， 复制相关属性，保存，如果是编辑原来的信息，
     * personId不为空。则查询出实体对象，复制相关属性，保存后修改数据库信息，永久修改
     *
     * @return 新建修改学生的主键 student_id 返回前端
     */
    @PostMapping("/studentEditSave")
    @PreAuthorize(" hasRole('ADMIN')")
    public DataResponse studentEditSave(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.studentEditSave(dataRequest);
    }



    /**
     * importFeeData 前端上传消费流水Excl表数据服务
     *
     * @param barr         文件二进制数据
     * @param uploader     上传者
     * @param personIdStr student 主键
     * @param fileName     前端上传的文件名
     */
    @PostMapping(path = "/importFeeData")
    public DataResponse importFeeData(@RequestBody byte[] barr,
                                      @RequestParam(name = "uploader") String uploader,
                                      @RequestParam(name = "personId") String personIdStr,
                                      @RequestParam(name = "fileName") String fileName) {
        return studentService.importFeeData(barr, personIdStr);
    }

    /**
     * getStudentListExcl 前端下载导出学生基本信息Excl表数据
     *
     */
    @PostMapping("/getStudentListExcl")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<StreamingResponseBody> getStudentListExcl(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.getStudentListExcl(dataRequest);
    }


    @PostMapping("/getStudentPageData")
    @PreAuthorize(" hasRole('ADMIN')")
    public DataResponse getStudentPageData(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.getStudentPageData(dataRequest);
    }

    /*
        FamilyMember
     */
    @PostMapping("/getFamilyMemberList")
    @PreAuthorize(" hasRole('ADMIN') or  hasRole('STUDENT')")
    public DataResponse getFamilyMemberList(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.getFamilyMemberList(dataRequest);
    }

    @PostMapping("/familyMemberSave")
    @PreAuthorize(" hasRole('ADMIN') or  hasRole('STUDENT')")
    public DataResponse familyMemberSave(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.familyMemberSave(dataRequest);
    }

    @PostMapping("/familyMemberDelete")
    @PreAuthorize(" hasRole('ADMIN') or  hasRole('STUDENT')")
    public DataResponse familyMemberDelete(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.familyMemberDelete(dataRequest);
    }


    @PostMapping("/importFeeDataWeb")
    @PreAuthorize("hasRole('STUDENT')")
    public DataResponse importFeeDataWeb(@RequestParam Map<String,Object> request, @RequestParam("file") MultipartFile file) {
        return studentService.importFeeDataWeb(request, file);
    }

    @PostMapping("/getStudentIntroduceData")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    public DataResponse getStudentIntroduceData(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.getStudentIntroduceData(dataRequest);
    }

    /**
     * 获取学生个人画像数据(聚合基本信息、成绩、考勤、实践荣誉等)
     */
    @PostMapping("/getStudentPortrait")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse getStudentPortrait(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.getStudentPortrait(dataRequest);
    }

    // ==================== 课程中心接口 ====================

    /**
     * 获取课程资料列表
     */
    @PostMapping("/getCourseMaterialList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getCourseMaterialList(@Valid @RequestBody DataRequest dataRequest) {
        return courseMaterialService.getCourseMaterialList(dataRequest);
    }

    /**
     * 保存课程资料(包含文件上传) - multipart/form-data 方式
     * 权限控制：仅教师/管理员可上传
     */
    @PostMapping(value = "/courseMaterialSave", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse courseMaterialSaveMultipart(@RequestParam Map<String, Object> request,
                                                     @RequestParam("file") MultipartFile file) {
        DataRequest dataRequest = new DataRequest();
        dataRequest.setData(request);
        return courseMaterialService.courseMaterialSave(dataRequest, file);
    }

    /**
     * 保存课程资料(文件上传) - application/octet-stream 方式
     * 前端通过二进制流上传文件，参数通过URL query string传递
     * 
     * @param fileData      文件二进制数据（请求体）
     * @param courseType    资料类型：textbook/courseware/reference（必传）
     * @param courseId      课程ID（必传）
     * @param fileName      文件名（必传，如：xxx.pdf）
     * @param courseName    课程名称（可选）
     * @param materialName  资料名称（可选，不传则使用fileName）
     * @param description   描述（可选）
     * @param uploader      上传者（可选，从token获取）
     * @return DataResponse 包含code和msg
     */
    @PostMapping(value = "/courseMaterialSave", consumes = "application/octet-stream")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse courseMaterialSaveBinary(
            @RequestBody byte[] fileData,
            @RequestParam(value = "courseType", required = false) String courseType,
            @RequestParam(value = "courseId", required = false) Integer courseId,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam(value = "courseName", required = false) String courseName,
            @RequestParam(value = "materialName", required = false) String materialName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "uploader", required = false) String uploader) {
        return courseMaterialService.courseMaterialSaveBinary(
                fileData, courseType, courseId, fileName, courseName, materialName, description, uploader);
    }

    /**
     * 删除课程资料
     */
    @PostMapping("/courseMaterialDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse courseMaterialDelete(@Valid @RequestBody DataRequest dataRequest) {
        return courseMaterialService.courseMaterialDelete(dataRequest);
    }

    /**
     * 下载课程资料
     * 前端发送 POST 请求，请求体: {"materialId": 1}
     * 返回文件二进制数据（byte[]）
     */
    @PostMapping("/courseMaterialDownload")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public ResponseEntity<byte[]> courseMaterialDownload(@Valid @RequestBody DataRequest dataRequest) {
        return courseMaterialService.courseMaterialDownload(dataRequest);
    }

    // ==================== 消费日志接口 ====================

    /**
     * 获取消费记录列表
     */
    @PostMapping("/getConsumptionList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getConsumptionList(@Valid @RequestBody DataRequest dataRequest) {
        return consumptionService.getConsumptionList(dataRequest);
    }

    /**
     * 保存消费记录
     */
    @PostMapping("/consumptionSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse consumptionSave(@Valid @RequestBody DataRequest dataRequest) {
        return consumptionService.consumptionSave(dataRequest);
    }

    /**
     * 删除消费记录
     */
    @PostMapping("/consumptionDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse consumptionDelete(@Valid @RequestBody DataRequest dataRequest) {
        return consumptionService.consumptionDelete(dataRequest);
    }

    /**
     * 获取月度消费统计
     */
    @PostMapping("/getMonthlyConsumptionStats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getMonthlyConsumptionStats(@Valid @RequestBody DataRequest dataRequest) {
        return consumptionService.getMonthlyConsumptionStats(dataRequest);
    }

    /**
     * Excel批量导入消费数据
     */
    @PostMapping("/importConsumptionData")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse importConsumptionData(@RequestParam Map<String, Object> request,
                                              @RequestParam("file") MultipartFile file) {
        DataRequest dataRequest = new DataRequest();
        dataRequest.setData(request);
        return consumptionService.importConsumptionData(dataRequest, file);
    }

    // ==================== 综合绩分计算接口 ====================

    /**
     * 计算学生个人综合绩分
     */
    @PostMapping("/calculateStudentScore")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse calculateStudentScore(@Valid @RequestBody DataRequest dataRequest) {
        Integer studentId = dataRequest.getInteger("studentId");
        if (studentId == null) {
            studentId = dataRequest.getInteger("personId");
        }
        
        if (studentId == null || studentId <= 0) {
            return new DataResponse(1, null, "学生ID不能为空");
        }
        
        Map<String, Double> weights = null;
        Object weightsObj = dataRequest.getData().get("weights");
        if (weightsObj instanceof Map) {
            weights = (Map<String, Double>) weightsObj;
        }
        
        Map<String, Object> result = scoreCalculationService.calculateStudentScore(studentId, weights);
        return new DataResponse(0, result, "success");
    }

    /**
     * 获取所有学生绩分排名
     */
    @PostMapping("/getScoreRanking")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse getScoreRanking(@Valid @RequestBody DataRequest dataRequest) {
        return scoreCalculationService.getScoreRanking();
    }

    /**
     * 获取默认权重配置
     */
    @PostMapping("/getDefaultWeights")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getDefaultWeights(@Valid @RequestBody DataRequest dataRequest) {
        return scoreCalculationService.getDefaultWeights();
    }

    /**
     * 保存自定义权重配置
     */
    @PostMapping("/saveWeightConfig")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse saveWeightConfig(@Valid @RequestBody DataRequest dataRequest) {
        return scoreCalculationService.saveWeightConfig(dataRequest);
    }

    /**
     * 批量计算所有学生绩分
     */
    @PostMapping("/batchCalculateAllScores")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse batchCalculateAllScores(@Valid @RequestBody DataRequest dataRequest) {
        return scoreCalculationService.batchCalculateAllScores();
    }

    // ==================== 个人简历接口 ====================

    /**
     * 生成个人简历（返回JSON数据供前端生成PDF）
     */
    @PostMapping("/generateResumePDF")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public ResponseEntity<org.springframework.core.io.Resource> generateResumePDF(@Valid @RequestBody DataRequest dataRequest) {
        return resumeService.generateResumePDF(dataRequest);
    }

    /**
     * 预览简历数据（JSON格式）
     */
    @PostMapping("/previewResumeData")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse previewResumeData(@Valid @RequestBody DataRequest dataRequest) {
        return resumeService.previewResumeData(dataRequest);
    }
}
