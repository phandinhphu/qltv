package fpt.training.qltv.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
	String uploadImage(MultipartFile file, String folder);

	String uploadFile(MultipartFile file, String folder);

	void deleteImage(String publicId);

	void deleteFile(String publicId);
}
