package fpt.training.qltv.service;

import fpt.training.qltv.dto.request.BookFilterRequest;
import fpt.training.qltv.dto.request.CreateBookRequest;
import fpt.training.qltv.dto.request.UpdateBookRequest;
import fpt.training.qltv.dto.response.BookDetailResponse;
import fpt.training.qltv.dto.response.BookResponse;
import fpt.training.qltv.dto.response.PageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface BookService {

    PageResponse<BookResponse> findAll(BookFilterRequest filter, int page, int size);

    PageResponse<BookResponse> findAllDeleted(int page, int size);

    BookDetailResponse findById(Long id);

    BookResponse create(CreateBookRequest request, MultipartFile cover, MultipartFile file);

    BookResponse update(Long id, UpdateBookRequest request, MultipartFile cover, MultipartFile file);

    void delete(Long id);

    void restore(Long id);

    void forceDelete(Long id);
}
