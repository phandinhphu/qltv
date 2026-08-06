package fpt.training.qltv.service;

import fpt.training.qltv.dto.request.AuthorFilterRequest;
import fpt.training.qltv.dto.request.CreateAuthorRequest;
import fpt.training.qltv.dto.request.UpdateAuthorRequest;
import fpt.training.qltv.dto.response.AuthorResponse;
import fpt.training.qltv.dto.response.PageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AuthorService {

    PageResponse<AuthorResponse> findAll(AuthorFilterRequest filter, int page, int size);

    PageResponse<AuthorResponse> findAllDeleted(int page, int size);

    AuthorResponse findById(Long id);

    AuthorResponse create(CreateAuthorRequest request, MultipartFile avatar);

    AuthorResponse update(Long id, UpdateAuthorRequest request, MultipartFile avatar);

    void delete(Long id);

    void restore(Long id);

    void forceDelete(Long id);
}
