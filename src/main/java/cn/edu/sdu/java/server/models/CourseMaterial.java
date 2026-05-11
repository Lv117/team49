package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * CourseMaterial 课程资料实体类
 * 保存课程资源的基本信息
 */
@Getter
@Setter
@Entity
@Table(name = "course_material")
public class CourseMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer materialId;

    @Column(name = "course_id")
    private Integer courseId;

    @Column(name = "course_name", length = 100)
    private String courseName;

    @Column(name = "material_name", length = 200, nullable = false)
    private String materialName;

    @Column(name = "course_type", length = 50)
    private String courseType;  // textbook-教材, courseware-课件, reference-参考资料

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_name", length = 200)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "uploader_id")
    private Integer uploaderId;

    @Column(name = "uploader_name", length = 50)
    private String uploaderName;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    @PrePersist
    protected void onCreate() {
        if (uploadTime == null) {
            uploadTime = LocalDateTime.now();
        }
    }
}
