package fpt.training.qltv.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import fpt.training.qltv.exception.common.FileUploadException;
import fpt.training.qltv.service.CloudinaryService;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {
    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        return upload(file, folder, "image");
    }

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        return upload(file, folder, "raw");
    }

    @Override
    public void deleteImage(String publicId) {
        delete(publicId, "image");
    }

    @Override
    public void deleteFile(String publicId) {
        delete(publicId, "raw");
    }

    private String upload(MultipartFile file, String folder, String resourceType) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", resourceType
                )
            );
            Object url = result.get("secure_url");
            if (url == null) {
                throw new FileUploadException("Không thể lấy được URL khi tải lên");
            }
            return url.toString();
        } catch (IOException ex) {
            throw new FileUploadException("Không thể tải lên file: " + ex.getMessage());
        }
    }

    private void delete(String publicId, String resourceType) {
        try {
            cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap("resource_type", resourceType)
            );
        } catch (IOException ex) {
            throw new FileUploadException("Không thể xóa file: " + ex.getMessage());
        }
    }
}
