package project.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class S3Service {

    private static final String IMAGE_DIR = "images";

    @Value("${file.dir}")
    private String fileDir;

    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        Path uploadDir = getUploadDir();
        Files.createDirectories(uploadDir);

        String storeFileName = createStoreFileName(file.getOriginalFilename());
        Path targetPath = uploadDir.resolve(storeFileName).normalize();
        if (!targetPath.startsWith(uploadDir)) {
            throw new IOException("Invalid upload path: " + targetPath);
        }

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String savedPath = IMAGE_DIR + "/" + storeFileName;
        log.info("Local file uploaded: original={}, saved={}", file.getOriginalFilename(), savedPath);
        return savedPath;
    }

    public List<String> storeFiles(List<MultipartFile> files) throws IOException {
        List<String> urls = new ArrayList<>();
        if (files == null) {
            return urls;
        }

        for (MultipartFile file : files) {
            String savedPath = uploadFile(file);
            if (savedPath != null) {
                urls.add(savedPath);
            }
        }
        return urls;
    }

    public void deleteFile(String key) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }

        try {
            Path targetPath = resolveLocalPath(key);
            Files.deleteIfExists(targetPath);
            log.info("Local file deleted: {}", key);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Failed to delete local file: {}, error={}", key, e.getMessage());
        }
    }

    public void deleteFiles(List<String> keys) {
        if (keys == null) {
            return;
        }

        for (String key : keys) {
            deleteFile(key);
        }
    }

    public void deleteFilesByUrls(List<String> urls) {
        if (urls == null) {
            return;
        }

        for (String url : urls) {
            deleteByUrl(url);
        }
    }

    public void deleteByUrl(String url) {
        deleteFile(extractLocalKey(url));
    }

    private Path getUploadDir() {
        return Paths.get(fileDir).toAbsolutePath().normalize().resolve(IMAGE_DIR);
    }

    private Path resolveLocalPath(String key) throws IOException {
        String normalizedKey = normalizeKey(key);
        Path baseDir = Paths.get(fileDir).toAbsolutePath().normalize();
        Path targetPath = baseDir.resolve(normalizedKey).normalize();

        if (!targetPath.startsWith(baseDir)) {
            throw new IOException("Path traversal is not allowed: " + key);
        }
        return targetPath;
    }

    private String extractLocalKey(String url) {
        if (url == null) {
            return null;
        }

        String value = url.trim();
        if (value.isEmpty()) {
            return value;
        }

        try {
            URI uri = new URI(value);
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
                return normalizeKey(path);
            }
        } catch (URISyntaxException ignored) {
            // Fall through and treat the value as an already-local key.
        }

        return normalizeKey(value);
    }

    private String normalizeKey(String key) {
        String normalized = key.replace("\\", "/");
        if (normalized.startsWith("/img/")) {
            normalized = normalized.substring("/img/".length());
        } else if (normalized.startsWith("img/")) {
            normalized = normalized.substring("img/".length());
        } else if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String createStoreFileName(String originalFilename) {
        String ext = extractExtension(originalFilename);
        return UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }

        String filename = Paths.get(originalFilename).getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1);
    }
}
