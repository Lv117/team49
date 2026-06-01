package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
import cn.edu.sdu.java.server.models.*;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.*;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ResumeService 个人简历生成服务
 */
@Service
public class ResumeService {
    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final StudentRepository studentRepository;
    private final ScoreRepository scoreRepository;
    private final HonorRepository honorRepository;
    private final DevelopmentRepository developmentRepository;
    private final TeacherDataScopeService teacherDataScopeService;

    public ResumeService(StudentRepository studentRepository,
                         ScoreRepository scoreRepository,
                         HonorRepository honorRepository,
                         DevelopmentRepository developmentRepository,
                         TeacherDataScopeService teacherDataScopeService) {
        this.studentRepository = studentRepository;
        this.scoreRepository = scoreRepository;
        this.honorRepository = honorRepository;
        this.developmentRepository = developmentRepository;
        this.teacherDataScopeService = teacherDataScopeService;
    }

    /**
     * 预览简历数据（JSON格式）
     */
    public DataResponse previewResumeData(DataRequest dataRequest) {
        Integer studentId = dataRequest.getInteger("studentId");

        if (studentId == null || studentId <= 0) {
            studentId = CommonMethod.getPersonId();
        }
        teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, "教师仅可查看本人授课学生的简历数据");

        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            throw new BusinessException(ErrorCodes.RESUME_STUDENT_NOT_FOUND, "学生不存在");
        }

        Student student = studentOpt.get();
        Person person = student.getPerson();
        if (person == null) {
            throw new BusinessException(ErrorCodes.RESUME_PERSON_INCOMPLETE, "学生基础信息不完整，无法生成简历");
        }

        Map<String, Object> resumeData = new LinkedHashMap<>();

        // 基本信息
        Map<String, Object> basicInfo = new LinkedHashMap<>();
        basicInfo.put("name", person.getName());
        basicInfo.put("num", person.getNum());
        basicInfo.put("gender", person.getGender());
        basicInfo.put("birthday", person.getBirthday());
        basicInfo.put("phone", person.getPhone());
        basicInfo.put("email", person.getEmail());
        basicInfo.put("major", student.getMajor());
        resumeData.put("basicInfo", basicInfo);

        // 成绩统计
        List<Score> scores = scoreRepository.findByStudentPersonId(studentId);
        Map<String, Object> scoreStats = new LinkedHashMap<>();
        if (!scores.isEmpty()) {
            double avgScore = scores.stream()
                    .mapToDouble(s -> s.getMark() != null ? s.getMark().doubleValue() : 0.0)
                    .average()
                    .orElse(0.0);
            double totalCredit = scores.stream()
                    .filter(s -> s.getCourse() != null)
                    .mapToDouble(s -> s.getCourse().getCredit() != null ? s.getCourse().getCredit() : 0)
                    .sum();
            scoreStats.put("courseCount", scores.size());
            scoreStats.put("averageScore", String.format("%.2f", avgScore));
            scoreStats.put("totalCredit", String.format("%.1f", totalCredit));
        }
        resumeData.put("scoreStats", scoreStats);

        // 荣誉与创新数据来自不同聚合：荣誉走 Honor 表，创新实践走 StudentDevelopment 表。
        List<Honor> honors = honorRepository.findByStudentId(studentId);
        List<Map<String, Object>> honorList = new ArrayList<>();
        for (Honor honor : honors) {
            if ("approved".equals(honor.getStatus())) {
                Map<String, Object> honorItem = new LinkedHashMap<>();
                honorItem.put("name", honor.getHonorName());
                honorItem.put("level", honor.getHonorLevel());
                honorItem.put("date", honor.getAwardDate());
                honorList.add(honorItem);
            }
        }
        resumeData.put("honors", honorList);

        // 创新创业
        List<StudentDevelopment> developments = developmentRepository.findByStudentId(studentId);
        List<Map<String, Object>> devList = new ArrayList<>();
        for (StudentDevelopment dev : developments) {
            if ("approved".equals(dev.getStatus())) {
                Map<String, Object> devItem = new LinkedHashMap<>();
                devItem.put("title", dev.getTitle());
                devItem.put("type", dev.getDevelopmentType());
                devList.add(devItem);
            }
        }
        resumeData.put("innovations", devList);

        // 自我评价
        resumeData.put("selfIntroduction", person.getIntroduce());

        return CommonMethod.getReturnData(resumeData);
    }

    /**
     * PDF上下文，用于管理分页
     */
    private static class PdfContext {
        PDDocument document;
        PDPage currentPage;
        PDPageContentStream contentStream;
        float yPosition;
        PDFont font;
        final float margin;
        final float pageHeight;

        PdfContext(PDDocument document, PDFont font) throws Exception {
            this.document = document;
            this.font = font;
            this.margin = 50;
            this.pageHeight = PDRectangle.A4.getHeight();
            this.currentPage = new PDPage(PDRectangle.A4);
            this.document.addPage(this.currentPage);
            this.contentStream = new PDPageContentStream(document, currentPage);
            this.yPosition = pageHeight - margin;
        }

        void checkAndCreateNewPage() throws Exception {
            if (yPosition < margin + 40) {
                contentStream.close();
                currentPage = new PDPage(PDRectangle.A4);
                document.addPage(currentPage);
                contentStream = new PDPageContentStream(document, currentPage);
                yPosition = pageHeight - margin;
            }
        }

        void writeText(String text, int fontSize) throws Exception {
            checkAndCreateNewPage();
            contentStream.setFont(font, fontSize);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText(text);
            contentStream.endText();
        }

        void writeCenteredText(String text, int fontSize) throws Exception {
            checkAndCreateNewPage();
            contentStream.setFont(font, fontSize);
            float pageWidth = PDRectangle.A4.getWidth();
            float textWidth = font.getStringWidth(text) / 1000 * fontSize;
            contentStream.beginText();
            contentStream.newLineAtOffset((pageWidth - textWidth) / 2, yPosition);
            contentStream.showText(text);
            contentStream.endText();
        }

        void drawLine() throws Exception {
            checkAndCreateNewPage();
            contentStream.setLineWidth(1f);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(PDRectangle.A4.getWidth() - margin, yPosition);
            contentStream.stroke();
        }

        void moveDown(float amount) {
            yPosition -= amount;
        }

        void close() throws Exception {
            if (contentStream != null) {
                contentStream.close();
            }
        }
    }

    /**
     * 生成个人简历PDF（使用Apache PDFBox生成真实PDF文件）
     */
    public ResponseEntity<Resource> generateResumePDF(DataRequest dataRequest) {
        Integer studentId = dataRequest.getInteger("studentId");

        if (studentId == null || studentId <= 0) {
            studentId = CommonMethod.getPersonId();
        }
        teacherDataScopeService.assertCurrentTeacherAccessStudent(studentId, "教师仅可导出本人授课学生的简历");

        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Student student = studentOpt.get();
        Person person = student.getPerson();
        if (person == null) {
            log.warn("学生基础信息不完整，无法生成PDF简历，studentId={}", studentId);
            return ResponseEntity.badRequest().build();
        }
        float lineHeight = 16f;

        try (PDDocument document = new PDDocument()) {
            // 加载中文字体
            ClassPathResource fontResource = new ClassPathResource("font/SourceHanSansSC-Regular.ttf");
            PDFont chineseFont = PDType0Font.load(document, fontResource.getInputStream(), false);

            // 创建PDF上下文
            PdfContext ctx = new PdfContext(document, chineseFont);

            // 标题
            ctx.writeCenteredText("个人简历", 24);
            ctx.moveDown(40);

            // 分割线
            ctx.drawLine();
            ctx.moveDown(20);

            // 基本信息
            ctx.writeText("基本信息", 14);
            ctx.moveDown(20);

            ctx.font = chineseFont;
            ctx.writeText("姓名: " + (person.getName() != null ? person.getName() : ""), 11);
            ctx.moveDown(lineHeight);

            ctx.writeText("学号: " + (person.getNum() != null ? person.getNum() : ""), 11);
            ctx.moveDown(lineHeight);

            ctx.writeText("性别: " + (person.getGender() != null ? person.getGender() : ""), 11);
            ctx.moveDown(lineHeight);

            ctx.writeText("专业: " + (student.getMajor() != null ? student.getMajor() : ""), 11);
            ctx.moveDown(lineHeight);

            ctx.writeText("电话: " + (person.getPhone() != null ? person.getPhone() : ""), 11);
            ctx.moveDown(lineHeight);

            ctx.writeText("邮箱: " + (person.getEmail() != null ? person.getEmail() : ""), 11);
            ctx.moveDown(25);

            // 成绩统计
            ctx.writeText("成绩统计", 14);
            ctx.moveDown(20);

            List<Score> scores = scoreRepository.findByStudentPersonId(studentId);
            if (!scores.isEmpty()) {
                double avgScore = scores.stream()
                        .mapToDouble(s -> s.getMark() != null ? s.getMark().doubleValue() : 0.0)
                        .average()
                        .orElse(0.0);
                double totalCredit = scores.stream()
                        .filter(s -> s.getCourse() != null)
                        .mapToDouble(s -> s.getCourse().getCredit() != null ? s.getCourse().getCredit() : 0)
                        .sum();

                ctx.writeText("课程数量: " + scores.size(), 11);
                ctx.moveDown(lineHeight);

                ctx.writeText("平均成绩: " + String.format("%.2f", avgScore), 11);
                ctx.moveDown(lineHeight);

                ctx.writeText("总学分: " + String.format("%.1f", totalCredit), 11);
                ctx.moveDown(25);
            } else {
                ctx.writeText("暂无成绩数据", 11);
                ctx.moveDown(25);
            }

            // 荣誉奖励
            ctx.writeText("荣誉奖励", 14);
            ctx.moveDown(20);

            List<Honor> honors = honorRepository.findByStudentId(studentId);
            boolean hasHonors = false;
            for (Honor honor : honors) {
                if ("approved".equals(honor.getStatus())) {
                    hasHonors = true;
                    ctx.checkAndCreateNewPage();
                    ctx.contentStream.beginText();
                    ctx.contentStream.newLineAtOffset(ctx.margin, ctx.yPosition);
                    ctx.contentStream.showText("• " + honor.getHonorName() + " (" +
                            (honor.getHonorLevel() != null ? honor.getHonorLevel() : "") + ")");
                    ctx.contentStream.endText();
                    ctx.moveDown(lineHeight);
                }
            }
            if (!hasHonors) {
                ctx.writeText("暂无荣誉记录", 11);
                ctx.moveDown(lineHeight);
            }
            ctx.moveDown(9);

            // 创新创业
            ctx.writeText("创新创业", 14);
            ctx.moveDown(20);

            List<StudentDevelopment> developments = developmentRepository.findByStudentId(studentId);
            boolean hasDevelopments = false;
            for (StudentDevelopment dev : developments) {
                if ("approved".equals(dev.getStatus())) {
                    hasDevelopments = true;
                    ctx.checkAndCreateNewPage();
                    ctx.contentStream.beginText();
                    ctx.contentStream.newLineAtOffset(ctx.margin, ctx.yPosition);
                    ctx.contentStream.showText("• " + dev.getTitle() + " (" +
                            (dev.getDevelopmentType() != null ? dev.getDevelopmentType() : "") + ")");
                    ctx.contentStream.endText();
                    ctx.moveDown(lineHeight);
                }
            }
            if (!hasDevelopments) {
                ctx.writeText("暂无创新创业项目", 11);
                ctx.moveDown(lineHeight);
            }
            ctx.moveDown(25);

            // 自我评价
            ctx.writeText("自我评价", 14);
            ctx.moveDown(20);

            String intro = person.getIntroduce();
            if (intro != null && !intro.isEmpty()) {
                String[] lines = wrapText(intro, 35);
                for (String line : lines) {
                    ctx.checkAndCreateNewPage();
                    ctx.contentStream.beginText();
                    ctx.contentStream.newLineAtOffset(ctx.margin, ctx.yPosition);
                    ctx.contentStream.showText(line);
                    ctx.contentStream.endText();
                    ctx.moveDown(lineHeight);
                }
            } else {
                ctx.writeText("暂无自我评价", 11);
            }

            // 关闭内容流
            ctx.close();

            // 在最后一页底部添加页脚
            int pageCount = document.getNumberOfPages();
            if (pageCount > 0) {
                PDPage lastPage = document.getPage(pageCount - 1);
                try (PDPageContentStream footerStream = new PDPageContentStream(document, lastPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    float footerY = ctx.margin + 10;
                    footerStream.setFont(chineseFont, 9);
                    footerStream.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                    String footer = "生成时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    float footerWidth = chineseFont.getStringWidth(footer) / 1000 * 9;
                    footerStream.beginText();
                    footerStream.newLineAtOffset((PDRectangle.A4.getWidth() - footerWidth) / 2, footerY);
                    footerStream.showText(footer);
                    footerStream.endText();
                }
            }

            // 转换为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            byte[] pdfBytes = baos.toByteArray();

            Resource resource = new ByteArrayResource(pdfBytes);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"resume_" +
                                    (person.getName() != null ? person.getName() : "student") + ".pdf\"")
                    .contentLength(pdfBytes.length)
                    .body(resource);

        } catch (Exception e) {
            log.error("生成PDF失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 文本自动换行处理
     */
    private String[] wrapText(String text, int maxLineLength) {
        if (text == null || text.length() <= maxLineLength) {
            return new String[]{text != null ? text : ""};
        }

        List<String> lines = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLineLength, text.length());
            lines.add(text.substring(start, end));
            start = end;
        }
        return lines.toArray(new String[0]);
    }
}
