package fpt.training.qltv.service.impl;

import fpt.training.qltv.dto.request.BorrowFilterRequest;
import fpt.training.qltv.dto.response.BorrowRecordResponse;
import fpt.training.qltv.dto.response.PageResponse;
import fpt.training.qltv.entity.Book;
import fpt.training.qltv.entity.BookStatus;
import fpt.training.qltv.entity.BorrowRecord;
import fpt.training.qltv.entity.BorrowStatus;
import fpt.training.qltv.entity.User;
import fpt.training.qltv.exception.common.BusinessException;
import fpt.training.qltv.exception.common.FileAccessDeniedException;
import fpt.training.qltv.exception.common.ResourceNotFoundException;
import fpt.training.qltv.repository.BookRepository;
import fpt.training.qltv.repository.BorrowRecordRepository;
import fpt.training.qltv.repository.UserRepository;
import fpt.training.qltv.repository.projection.BorrowRecordProjection;
import fpt.training.qltv.repository.projection.BorrowTokenProjection;
import fpt.training.qltv.service.BorrowService;
import fpt.training.qltv.service.PrivateFileService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final PrivateFileService privateFileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                @CacheEvict(value = "books", key = "#bookId"),
                @CacheEvict(value = "dashboard", allEntries = true)
            })
    public BorrowRecordResponse borrow(Long bookId, Long userId) {
        User user = getUserOrThrow(userId);
        Book book =
                bookRepository
                        .findByIdWithLock(bookId)
                        .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));

        if (book.isDeleted()) {
            throw new BusinessException("Sách này đã bị xóa tạm, không thể mượn");
        }

        if (borrowRecordRepository.countByUserIdAndStatus(userId, BorrowStatus.OVERDUE) > 0) {
            throw new BusinessException("Bạn có sách quá hạn chưa trả, không thể mượn thêm");
        }

        long activeBorrowCount =
                borrowRecordRepository.countByUserIdAndStatus(userId, BorrowStatus.BORROWING);
        if (activeBorrowCount >= 3) {
            throw new BusinessException("Bạn đang mượn tối đa 3 sách");
        }

        if (borrowRecordRepository.existsByUserIdAndBookIdAndStatus(
                userId, bookId, BorrowStatus.BORROWING)) {
            throw new BusinessException("Bạn đang mượn sách này rồi");
        }

        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw new BusinessException("Sách hiện không còn bản để mượn");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = now.plusDays(7);

        BorrowRecord borrowRecord = new BorrowRecord();
        borrowRecord.setUser(user);
        borrowRecord.setBook(book);
        borrowRecord.setBorrowDate(now);
        borrowRecord.setDueDate(dueDate);
        borrowRecord.setStatus(BorrowStatus.BORROWING);
        borrowRecord.setDownloadToken(UUID.randomUUID().toString());
        borrowRecord.setTokenExpiredAt(dueDate);
        borrowRecord.setCreatedAt(now);
        borrowRecord.setUpdatedAt(now);

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        if (book.getAvailableCopies() <= 0) {
            book.setStatus(BookStatus.OUT_OF_STOCK);
        } else {
            book.setStatus(BookStatus.AVAILABLE);
        }

        bookRepository.save(book);
        return toResponse(borrowRecordRepository.save(borrowRecord));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                @CacheEvict(value = "books", key = "#result.bookId"),
                @CacheEvict(value = "dashboard", allEntries = true)
            })
    public BorrowRecordResponse returnBook(Long borrowId, Long userId) {
        BorrowRecord borrowRecord = getBorrowRecordOrThrow(borrowId);
        if (!borrowRecord.getUser().getId().equals(userId)) {
            throw new FileAccessDeniedException("Bạn không có quyền trả bản ghi mượn này");
        }

        if (borrowRecord.getStatus() == BorrowStatus.RETURNED) {
            throw new BusinessException("Sách đã được trả");
        }

        if (borrowRecord.getStatus() != BorrowStatus.BORROWING
                && borrowRecord.getStatus() != BorrowStatus.OVERDUE) {
            throw new BusinessException("Sách đã được trả");
        }

        borrowRecord.setReturnDate(LocalDateTime.now());
        borrowRecord.setStatus(BorrowStatus.RETURNED);
        borrowRecord.setDownloadToken(null);
        borrowRecord.setTokenExpiredAt(null);
        borrowRecord.setUpdatedAt(LocalDateTime.now());

        bookRepository.incrementAvailableCopies(borrowRecord.getBook().getId());

        return toResponse(borrowRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource readFile(String downloadToken, Long userId) {
        BorrowTokenProjection token = validateDownloadTokenProjection(downloadToken, userId);
        return resolveFile(token.getBook().getFileUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFile(String downloadToken, Long userId) {
        BorrowTokenProjection token = validateDownloadTokenProjection(downloadToken, userId);
        return resolveFile(token.getBook().getFileUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BorrowRecordResponse> getMyBorrows(Long userId, int page, int size) {
        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.max(size, 1),
                        Sort.by(Sort.Direction.DESC, "borrowDate"));

        Page<BorrowRecordResponse> result =
                borrowRecordRepository
                        .findAllProjected(userId, null, null, null, null, pageable)
                        .map(this::toResponse);
        return PageResponse.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BorrowRecordResponse> getAllBorrows(
            BorrowFilterRequest filter, int page, int size) {
        BorrowFilterRequest safeFilter = filter == null ? new BorrowFilterRequest() : filter;
        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.max(size, 1),
                        Sort.by(Sort.Direction.DESC, "borrowDate"));

        Page<BorrowRecordResponse> result =
                borrowRecordRepository
                        .findAllProjected(
                                safeFilter.getUserId(),
                                safeFilter.getBookId(),
                                safeFilter.getStatus(),
                                safeFilter.getFromDate(),
                                safeFilter.getToDate(),
                                pageable)
                        .map(this::toResponse);
        return PageResponse.of(result);
    }

    private User getUserOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("Id user không được để trống");
        }
        return userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private BorrowRecord getBorrowRecordOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("Id borrow không được để trống");
        }
        // findWithUserAndBookById dùng @EntityGraph(user, book)
        // → 1 query thay vì 3 (findById + lazy user + lazy book)
        // user cần để check ownership; book cần để evict cache và incrementAvailableCopies
        return borrowRecordRepository
                .findWithUserAndBookById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BorrowRecord", id));
    }

    private BorrowTokenProjection validateDownloadTokenProjection(
            String downloadToken, Long userId) {
        BorrowTokenProjection token =
                borrowRecordRepository
                        .findByDownloadToken(downloadToken, BorrowTokenProjection.class)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "BorrowRecord not found with download token: "
                                                        + downloadToken));

        if (!token.getUser().getId().equals(userId)) {
            throw new FileAccessDeniedException("Bạn không có quyền tải file này");
        }

        if (token.getStatus() != BorrowStatus.BORROWING) {
            throw new FileAccessDeniedException("Sách đã được trả");
        }

        if (token.getTokenExpiredAt() == null
                || !token.getTokenExpiredAt().isAfter(LocalDateTime.now())) {
            throw new FileAccessDeniedException("Token đã hết hạn");
        }

        return token;
    }

    private Resource resolveFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank() || "null".equalsIgnoreCase(fileUrl.trim())) {
            throw new BusinessException("Sách chưa có file để tải");
        }
        return privateFileService.loadFile(fileUrl);
    }

    private BorrowRecordResponse toResponse(BorrowRecord borrowRecord) {
        BorrowRecordResponse response = new BorrowRecordResponse();
        response.setId(borrowRecord.getId());
        response.setUserId(borrowRecord.getUser().getId());
        response.setUsername(borrowRecord.getUser().getUsername());
        response.setBookId(borrowRecord.getBook().getId());
        response.setBookTitle(borrowRecord.getBook().getTitle());
        response.setBorrowDate(borrowRecord.getBorrowDate());
        response.setDueDate(borrowRecord.getDueDate());
        response.setReturnDate(borrowRecord.getReturnDate());
        response.setStatus(borrowRecord.getStatus());
        response.setDownloadToken(borrowRecord.getDownloadToken());
        response.setTokenExpiredAt(borrowRecord.getTokenExpiredAt());

        LocalDateTime now = LocalDateTime.now();
        response.setDaysRemaining(
                borrowRecord.getDueDate() == null
                        ? null
                        : (int)
                                ChronoUnit.DAYS.between(
                                        now.toLocalDate(),
                                        borrowRecord.getDueDate().toLocalDate()));
        response.setHasValidToken(
                borrowRecord.getStatus() == BorrowStatus.BORROWING
                        && borrowRecord.getDownloadToken() != null
                        && borrowRecord.getTokenExpiredAt() != null
                        && borrowRecord.getTokenExpiredAt().isAfter(now));
        response.setHasFile(
                borrowRecord.getBook().getFileUrl() != null
                        && !borrowRecord.getBook().getFileUrl().isBlank());
        return response;
    }

    private BorrowRecordResponse toResponse(BorrowRecordProjection projection) {
        BorrowRecordResponse response = new BorrowRecordResponse();
        response.setId(projection.getId());
        response.setUserId(projection.getUserId());
        response.setUsername(projection.getUsername());
        response.setBookId(projection.getBookId());
        response.setBookTitle(projection.getBookTitle());
        response.setBorrowDate(projection.getBorrowDate());
        response.setDueDate(projection.getDueDate());
        response.setReturnDate(projection.getReturnDate());
        response.setStatus(projection.getStatus());
        response.setDownloadToken(projection.getDownloadToken());
        response.setTokenExpiredAt(projection.getTokenExpiredAt());

        LocalDateTime now = LocalDateTime.now();
        response.setDaysRemaining(
                projection.getDueDate() == null
                        ? null
                        : (int)
                                ChronoUnit.DAYS.between(
                                        now.toLocalDate(), projection.getDueDate().toLocalDate()));
        response.setHasValidToken(
                projection.getStatus() == BorrowStatus.BORROWING
                        && projection.getDownloadToken() != null
                        && projection.getTokenExpiredAt() != null
                        && projection.getTokenExpiredAt().isAfter(now));
        response.setHasFile(
                projection.getBookFileUrl() != null && !projection.getBookFileUrl().isBlank());
        return response;
    }

    @Override
    public List<BorrowRecordResponse> findByUserIdAndBookId(Long userId, Long bookId) {
        // findByUserIdAndBookIdFetched dùng JOIN FETCH user và book
        // → 1 query thay vì 1 + N×2 lazy queries khi toResponse() truy cập user/book
        return borrowRecordRepository.findByUserIdAndBookIdFetched(userId, bookId).stream()
                .map(this::toResponse)
                .toList();
    }
}
