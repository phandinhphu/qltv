package fpt.training.qltv.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface PrivateFileService {

    String saveBookFile(MultipartFile file);

    Resource loadFile(String relativePath);

    void deleteFile(String relativePath);
}
