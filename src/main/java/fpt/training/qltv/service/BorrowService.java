package fpt.training.qltv.service;

import fpt.training.qltv.dto.request.BorrowFilterRequest;
import fpt.training.qltv.dto.response.BorrowRecordResponse;
import fpt.training.qltv.dto.response.PageResponse;
import java.util.List;
import org.springframework.core.io.Resource;

public interface BorrowService {

    BorrowRecordResponse borrow(Long bookId, Long userId);

    BorrowRecordResponse returnBook(Long borrowId, Long userId);

    Resource readFile(String downloadToken, Long userId);

    Resource downloadFile(String downloadToken, Long userId);

    PageResponse<BorrowRecordResponse> getMyBorrows(Long userId, int page, int size);

    PageResponse<BorrowRecordResponse> getAllBorrows(BorrowFilterRequest filter, int page, int size);

    List<BorrowRecordResponse> findByUserIdAndBookId(Long userId, Long bookId);
}
