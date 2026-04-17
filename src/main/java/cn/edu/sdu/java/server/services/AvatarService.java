package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.repositorys.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarService {

    private final PersonRepository personRepository;

    @Value("${attach.folder}")
    private String attachFolder;

    // 允许的扩展名
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");

    // 最大文件大小 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 上传头像
     * @param file 上传的图片文件
     * @return 头像的相对路径(如 /avatars/xxx.jpg)
     */
    public String uploadAvatar(MultipartFile file) throws Exception {
        // 1. 从SecurityContext获取当前用户ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        Integer personId = userDetails.getId();

        // 2. 验证文件
        validateFile(file);

        // 3. 生成唯一文件名
        String extension = getFileExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString() + "." + extension;
        String relativePath = "avatars/" + fileName;

        // 4. 确保目录存在并保存文件
        File directory = new File(attachFolder + "avatars");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        Path path = Paths.get(attachFolder + relativePath);
        Files.write(path, file.getBytes());

        // 5. 更新数据库(先删除旧头像)
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (person.getPhotoPath() != null) {
            try {
                Files.deleteIfExists(Paths.get(attachFolder + person.getPhotoPath()));
            } catch (Exception ignored) {
                // 忽略删除旧文件的异常
            }
        }

        person.setPhotoPath(relativePath);
        personRepository.save(person);

        return "/" + relativePath;
    }

    /**
     * 获取头像二进制数据
     * @param personId 人员ID
     * @return 头像字节数组,如果不存在返回null
     */
    public byte[] getAvatar(Integer personId) throws Exception {
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (person.getPhotoPath() == null) {
            return null;
        }

        Path path = Paths.get(attachFolder + person.getPhotoPath());
        if (!Files.exists(path)) {
            return null;
        }

        return Files.readAllBytes(path);
    }

    /**
     * 获取头像的Content-Type
     * @param personId 人员ID
     * @return Content-Type字符串
     */
    public String getAvatarContentType(Integer personId) throws Exception {
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (person.getPhotoPath() == null) {
            return "image/jpeg"; // 默认
        }

        String extension = getFileExtension(person.getPhotoPath());
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "image/jpeg";
        };
    }

    /**
     * 删除头像
     * @param personId 人员ID
     */
    public void deleteAvatar(Integer personId) throws Exception {
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (person.getPhotoPath() != null) {
            try {
                Files.deleteIfExists(Paths.get(attachFolder + person.getPhotoPath()));
            } catch (Exception ignored) {
                // 忽略删除文件的异常
            }

            person.setPhotoPath(null);
            personRepository.save(person);
        }
    }

    /**
     * 验证上传的文件
     */
    private void validateFile(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new Exception("文件大小不能超过5MB");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new Exception("文件格式不支持,仅支持jpg/png/gif");
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
