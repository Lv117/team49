package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.CourseMaterial;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseMaterialRepository;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CourseMaterial 课程资料服务类
 */
@Service
public class CourseMaterialService {
    private static final Logger log = LoggerFactory.getLogger(CourseMaterialService.class);
    
    // 允许的文件类型
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".ppt", ".pptx", ".doc", ".docx");
    // 最大文件大小 50MB
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    // 上传目录
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/course-materials/";

    /**
     * 解析文件路径：兼容旧数据的绝对路径和新数据的相对文件名
     */
    private Path resolveFilePath(String storedPath) {
        if (storedPath == null || storedPath.isEmpty()) {
            return null;
        }
        Path path = Paths.get(storedPath);
        if (path.isAbsolute()) {
            return path;
        }
        return Paths.get(UPLOAD_DIR, storedPath);
    }
    
    private final CourseMaterialRepository courseMaterialRepository;
    private final CourseRepository courseRepository;

    public CourseMaterialService(CourseMaterialRepository courseMaterialRepository,
                                 CourseRepository courseRepository) {
        this.courseMaterialRepository = courseMaterialRepository;
        this.courseRepository = courseRepository;
    }
    
    /**
     * 获取课程资料列表
     */
    public DataResponse getCourseMaterialList(DataRequest dataRequest) {
        Integer courseId = dataRequest.getInteger("courseId");
        String courseType = dataRequest.getString("courseType");
        String keyword = dataRequest.getString("keyword");
        
        if (courseId == null) {
            courseId = 0;
        }
        
        List<CourseMaterial> materialList = courseMaterialRepository.findByConditions(courseId, courseType, keyword);
        
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (CourseMaterial material : materialList) {
            dataList.add(getMapFromMaterial(material));
        }
        
        return CommonMethod.getReturnData(dataList);
    }
    
    /**
     * 保存课程资料(包含文件上传)
     */
    public DataResponse courseMaterialSave(DataRequest dataRequest, MultipartFile file) {
        validateMultipartFile(file);
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        
        // 获取表单数据
        Map<String, Object> form = dataRequest.getData();
        if (form == null || form.isEmpty()) {
            form = dataRequest.getMap("form");
        }
        
        Integer materialId = CommonMethod.getInteger(form, "materialId");
        Integer courseId = CommonMethod.getInteger(form, "courseId");
        String courseName = CommonMethod.getString(form, "courseName");
        String materialName = CommonMethod.getString(form, "materialName");
        String courseType = CommonMethod.getString(form, "courseType");
        String description = CommonMethod.getString(form, "description");
        
        if (materialName == null || materialName.isEmpty()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "资料名称不能为空");
        }
        
        CourseMaterial material;
        
        // 更新或新建
        if (materialId != null && materialId > 0) {
            Optional<CourseMaterial> op = courseMaterialRepository.findById(materialId);
            if (op.isPresent()) {
                material = op.get();
                
                // 删除旧文件
                String oldStoredPath = material.getFilePath();
                if (oldStoredPath != null && !oldStoredPath.isEmpty()) {
                    try {
                        Path oldResolved = resolveFilePath(oldStoredPath);
                        if (oldResolved != null) {
                            Files.deleteIfExists(oldResolved);
                        }
                    } catch (IOException e) {
                        log.warn("删除旧文件失败: {}", oldStoredPath);
                    }
                }
            } else {
                throw new BusinessException(ErrorCodes.COURSE_MATERIAL_NOT_FOUND, "资料不存在");
            }
        } else {
            material = new CourseMaterial();
            material.setUploadTime(LocalDateTime.now());
        }
        
        String fileName = saveFile(file);
        if (fileName == null) {
            throw new BusinessException(ErrorCodes.COURSE_MATERIAL_SAVE_FAILED, "文件保存失败");
        }
        
        // 设置资料信息
        material.setCourseId(courseId);
        material.setCourseName(courseName);
        material.setMaterialName(materialName);
        material.setCourseType(courseType);
        material.setFileName(originalFilename);
        material.setFilePath(fileName);
        material.setFileSize(file.getSize());
        material.setDescription(description);
        material.setUploaderId(CommonMethod.getPersonId());
        material.setUploaderName(CommonMethod.getUsername());

        courseMaterialRepository.save(material);

        return CommonMethod.getReturnMessageOK();
    }
    
    /**
     * 删除课程资料
     */
    public DataResponse courseMaterialDelete(DataRequest dataRequest) {
        Integer materialId = dataRequest.getInteger("materialId");
        
        if (materialId != null && materialId > 0) {
            Optional<CourseMaterial> op = courseMaterialRepository.findById(materialId);
            if (op.isPresent()) {
                CourseMaterial material = op.get();
                
                // 删除文件
                String storedPath = material.getFilePath();
                if (storedPath != null && !storedPath.isEmpty()) {
                    try {
                        Path resolved = resolveFilePath(storedPath);
                        if (resolved != null) {
                            Files.deleteIfExists(resolved);
                        }
                    } catch (IOException e) {
                        log.warn("删除文件失败: {}", storedPath);
                    }
                }
                
                courseMaterialRepository.delete(material);
            }
        }
        
        return CommonMethod.getReturnMessageOK();
    }
    
    /**
     * 下载课程资料（返回文件二进制流）
     * 前端发送 POST 请求，请求体: {"materialId": 1}
     * 返回文件二进制数据（byte[]）
     */
    public ResponseEntity<byte[]> courseMaterialDownload(DataRequest dataRequest) {
        Integer materialId = dataRequest.getInteger("materialId");
        
        if (materialId == null || materialId <= 0) {
            log.warn("下载失败：资料ID无效 materialId={}", materialId);
            return ResponseEntity.badRequest().build();
        }
        
        Optional<CourseMaterial> op = courseMaterialRepository.findById(materialId);
        if (op.isEmpty()) {
            log.warn("下载失败：资料不存在 materialId={}", materialId);
            return ResponseEntity.notFound().build();
        }
        
        CourseMaterial material = op.get();
        String storedPath = material.getFilePath();
        String fileName = material.getFileName();

        if (storedPath == null || storedPath.isEmpty()) {
            log.warn("下载失败：文件路径为空 materialId={}", materialId);
            return ResponseEntity.notFound().build();
        }

        try {
            Path path = resolveFilePath(storedPath);
            if (path == null || !Files.exists(path)) {
                log.warn("下载失败：文件不存在 storedPath={}", storedPath);
                return ResponseEntity.notFound().build();
            }
            
            byte[] fileContent = Files.readAllBytes(path);
            
            // 根据文件扩展名设置 Content-Type
            String contentType = getContentType(fileName);
            
            // 对文件名进行 URL 编码，支持中文文件名
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(fileContent.length);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
            
            log.info("下载成功：materialId={}, fileName={}, size={}bytes", materialId, fileName, fileContent.length);
            
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("下载失败：读取文件异常 storedPath={}", storedPath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 根据文件名获取 Content-Type
     */
    private String getContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        return switch (extension) {
            case ".pdf" -> "application/pdf";
            case ".ppt" -> "application/vnd.ms-powerpoint";
            case ".pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }
    
    /**
     * 保存课程资料(二进制流上传)
     * 前端通过 application/octet-stream 方式上传文件
     * 
     * @param fileData     文件二进制数据
     * @param courseType   资料类型：textbook/courseware/reference
     * @param courseId     课程ID
     * @param fileName     文件名
     * @param courseName   课程名称
     * @param materialName 资料名称
     * @param description  描述
     * @param uploader     上传者
     * @return DataResponse 包含code和msg
     */
    public DataResponse courseMaterialSaveBinary(byte[] fileData, String courseType, Integer courseId,
                                                  String fileName, String courseName, String materialName,
                                                  String description, String uploader) {
        validateBinaryUpload(fileData, courseType, courseId, fileName);
        
        String savedFileName = saveFileFromBytes(fileData, fileName);
        if (savedFileName == null) {
            throw new BusinessException(ErrorCodes.COURSE_MATERIAL_SAVE_FAILED, "文件保存失败");
        }
        
        // 创建资料记录
        CourseMaterial material = new CourseMaterial();
        material.setCourseId(courseId);
        material.setCourseName(courseName != null && !courseName.isEmpty() ? courseName : null);
        material.setMaterialName(materialName != null && !materialName.isEmpty() ? materialName : fileName);
        material.setCourseType(courseType);
        material.setFileName(fileName);
        material.setFilePath(savedFileName);
        material.setFileSize((long) fileData.length);
        material.setDescription(description);
        material.setUploadTime(LocalDateTime.now());
        material.setUploaderId(CommonMethod.getPersonId());
        material.setUploaderName(uploader != null ? uploader : CommonMethod.getUsername());
        
        courseMaterialRepository.save(material);
        
        log.info("课程资料上传成功: courseId={}, courseType={}, fileName={}, size={}bytes", 
                courseId, courseType, fileName, fileData.length);
        
        return CommonMethod.getReturnMessageOK();
    }

    private void validateMultipartFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodes.COURSE_MATERIAL_FILE_INVALID, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCodes.COURSE_MATERIAL_FILE_INVALID, "文件大小不能超过50MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(ErrorCodes.COURSE_MATERIAL_FILE_INVALID, "文件名不能为空");
        }
        if (!originalFilename.contains(".")) {
            throw new BusinessException(ErrorCodes.COURSE_MATERIAL_FILE_INVALID, "文件扩展名不能为空");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCodes.COURSE_MATERIAL_FILE_INVALID, "只支持PDF/PPT/Word格式的文件");
        }
    }

    private void validateBinaryUpload(byte[] fileData, String courseType, Integer courseId, String fileName) {
        if (fileData == null || fileData.length == 0) {
            throw new BusinessException(ErrorCodes.COURSE_MATERIAL_FILE_INVALID, "文件不能为空");
        }
        if (courseType == null || courseType.isEmpty()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "courseType不能为空，可选值：textbook/courseware/reference");
        }
        if (courseId == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "courseId不能为空");
        }
        if (fileName == null || fileName.isEmpty()) {
            throw new BusinessException(ErrorCodes.COURSE_MATERIAL_FILE_INVALID, "fileName不能为空");
        }
        if (!fileName.contains(".")) {
            throw new BusinessException(ErrorCodes.COURSE_MATERIAL_FILE_INVALID, "文件扩展名不能为空");
        }
        if (fileData.length > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCodes.COURSE_MATERIAL_FILE_INVALID, "文件大小不能超过50MB");
        }
        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCodes.COURSE_MATERIAL_FILE_INVALID, "只支持PDF/PPT/Word格式的文件，当前文件扩展名：" + extension);
        }
        Set<String> validCourseTypes = Set.of("textbook", "courseware", "reference");
        if (!validCourseTypes.contains(courseType)) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "courseType无效，可选值：textbook/courseware/reference");
        }
    }
    
    /**
     * 保存文件到本地（MultipartFile方式）
     */
    private String saveFile(MultipartFile file) {
        try {
            // 创建上传目录
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // 生成唯一文件名
            String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            
            // 保存文件
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            Files.write(filePath, file.getBytes());
            
            return fileName;
        } catch (IOException e) {
            log.error("保存文件失败", e);
            return null;
        }
    }
    
    /**
     * 保存文件到本地（二进制流方式）
     */
    private String saveFileFromBytes(byte[] fileData, String originalFileName) {
        try {
            // 创建上传目录
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // 生成唯一文件名
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            
            // 保存文件
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            Files.write(filePath, fileData);
            
            return fileName;
        } catch (IOException e) {
            log.error("保存文件失败", e);
            return null;
        }
    }
    
    /**
     * 根据 courseId 查找课程名称，若 material 中 courseName 已赋值则直接返回
     */
    private String resolveCourseName(CourseMaterial material) {
        if (material.getCourseName() != null && !material.getCourseName().isEmpty()) {
            return material.getCourseName();
        }
        if (material.getCourseId() != null) {
            Optional<Course> course = courseRepository.findById(material.getCourseId());
            if (course.isPresent()) {
                return course.get().getName();
            }
        }
        return "";
    }

    /**
     * 将 CourseMaterial 转换为 Map
     */
    private Map<String, Object> getMapFromMaterial(CourseMaterial material) {
        Map<String, Object> map = new HashMap<>();
        map.put("materialId", material.getMaterialId());
        map.put("courseId", material.getCourseId());
        map.put("courseName", resolveCourseName(material));
        map.put("materialName", material.getMaterialName());
        map.put("courseType", material.getCourseType());
        map.put("courseTypeName", getCourseTypeName(material.getCourseType()));
        map.put("fileName", material.getFileName());
        map.put("fileSize", formatFileSize(material.getFileSize()));
        map.put("description", material.getDescription());
        map.put("uploaderId", material.getUploaderId());
        map.put("uploaderName", material.getUploaderName());
        map.put("uploadTime", material.getUploadTime());
        return map;
    }
    
    /**
     * 获取课程类型名称
     */
    private String getCourseTypeName(String courseType) {
        if (courseType == null) {
            return "其他";
        }
        return switch (courseType) {
            case "textbook" -> "教材";
            case "courseware" -> "课件";
            case "reference" -> "参考资料";
            default -> "其他";
        };
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long size) {
        if (size == null || size == 0) {
            return "0 B";
        }
        final String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double fileSize = size;
        while (fileSize >= 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }
        return String.format("%.1f %s", fileSize, units[unitIndex]);
    }
}
