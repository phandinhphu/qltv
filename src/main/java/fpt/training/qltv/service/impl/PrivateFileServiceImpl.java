package fpt.training.qltv.service.impl;

import fpt.training.qltv.exception.common.FileUploadException;
import fpt.training.qltv.service.PrivateFileService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PrivateFileServiceImpl implements PrivateFileService {

    private final Path basePath;

    public PrivateFileServiceImpl(@Value("${app.storage.private-path}") String baseDir) {
        Path path = Paths.get(baseDir);
        Path resolvedPath;
        if (path.isAbsolute()) {
            resolvedPath = path.normalize();
        } else {
            try {
                // Classpath root (target/classes) -> lên 2 cấp để lấy project root
                Path classesDir = new ClassPathResource("").getFile().toPath();
                Path projectRoot = classesDir.getParent().getParent();
                resolvedPath = projectRoot.resolve(baseDir).normalize();
            } catch (IOException e) {
                // Fallback: dùng working directory
                resolvedPath = path.toAbsolutePath().normalize();
            }
        }
        this.basePath = resolvedPath;
        try {
            Files.createDirectories(basePath.resolve("books"));
        } catch (IOException ex) {
            throw new FileUploadException("Không thể tạo thư mục lưu trữ file: " + ex.getMessage());
        }
    }

    @Override
    public String saveBookFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File sách không được để trống");
        }

        String originalName = file.getOriginalFilename() == null ? "book.pdf" : file.getOriginalFilename();
        String safeName = sanitizeFileName(originalName);
        if (!safeName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            safeName = safeName + ".pdf";
        }

        String fileName = UUID.randomUUID() + "_" + safeName;
        Path target = basePath.resolve(Paths.get("books", fileName)).normalize();
        ensureUnderBasePath(target);

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new FileUploadException("Không thể lưu file sách: " + ex.getMessage());
        }

        return "books/" + fileName;
    }

    @Override
    public Resource loadFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new FileUploadException("Đường dẫn file không hợp lệ");
        }
        Path target = basePath.resolve(relativePath).normalize();
        ensureUnderBasePath(target);
        try {
            Resource resource = new UrlResource(target.toUri());
            System.out.println("URI: " + target.toUri());
            if (!resource.exists()) {
                throw new FileUploadException("Không tìm thấy file sách");
            }
            return resource;
        } catch (IOException ex) {
            throw new FileUploadException("Không thể đọc file sách: " + ex.getMessage());
        }
    }

    @Override
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        Path target = basePath.resolve(relativePath).normalize();
        ensureUnderBasePath(target);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new FileUploadException("Không thể xóa file sách: " + ex.getMessage());
        }
    }

    private void ensureUnderBasePath(Path target) {
        if (!target.startsWith(basePath)) {
            throw new FileUploadException("Đường dẫn file không hợp lệ");
        }
    }

    private String sanitizeFileName(String fileName) {
        String sanitized = fileName.replaceAll("\\s+", "_");
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "");
        if (sanitized.isBlank()) {
            return "book.pdf";
        }
        return sanitized;
    }
}
