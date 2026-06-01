package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.exception.BusinessException;
import cn.edu.sdu.java.server.exception.ErrorCodes;
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
        Integer personId = getCurrentPersonId();
        validateFile(file);

        String extension = getFileExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString() + "." + extension;
        String relativePath = "avatars/" + fileName;

        File directory = new File(attachFolder + "avatars");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        Path path = Paths.get(attachFolder + relativePath);
        Files.write(path, file.getBytes());

        Person person = findPersonOrThrow(personId);

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
        Person person = findPersonOrThrow(personId);

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
        Person person = findPersonOrThrow(personId);

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
        Person person = findPersonOrThrow(personId);

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
            throw new BusinessException(ErrorCodes.AVATAR_FILE_INVALID, "文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCodes.AVATAR_FILE_INVALID, "文件大小不能超过5MB");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException(ErrorCodes.AVATAR_FILE_INVALID, "文件格式不支持，仅支持jpg/png/gif");
        }
    }

    private Integer getCurrentPersonId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new BusinessException(ErrorCodes.AUTH_FAILED, "当前登录状态无效，请重新登录");
        }
        return userDetails.getId();
    }

    private Person findPersonOrThrow(Integer personId) {
        return personRepository.findById(personId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.AVATAR_USER_NOT_FOUND, "用户不存在"));
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
