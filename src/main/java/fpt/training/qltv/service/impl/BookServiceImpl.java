package fpt.training.qltv.service.impl;

import fpt.training.qltv.dto.request.BookFilterRequest;
import fpt.training.qltv.dto.request.CreateBookRequest;
import fpt.training.qltv.dto.request.UpdateBookRequest;
import fpt.training.qltv.dto.response.BookDetailResponse;
import fpt.training.qltv.dto.response.BookResponse;
import fpt.training.qltv.dto.response.PageResponse;
import fpt.training.qltv.dto.response.ReviewResponse;
import fpt.training.qltv.entity.Author;
import fpt.training.qltv.entity.Book;
import fpt.training.qltv.entity.BookStatus;
import fpt.training.qltv.entity.BorrowStatus;
import fpt.training.qltv.entity.Category;
import fpt.training.qltv.entity.Review;
import fpt.training.qltv.exception.common.BusinessException;
import fpt.training.qltv.exception.common.FileUploadException;
import fpt.training.qltv.exception.common.ResourceNotFoundException;
import fpt.training.qltv.repository.AuthorRepository;
import fpt.training.qltv.repository.BookRepository;
import fpt.training.qltv.repository.BorrowRecordRepository;
import fpt.training.qltv.repository.CategoryRepository;
import fpt.training.qltv.repository.ReviewRepository;
import fpt.training.qltv.repository.projection.BookSummaryProjection;
import fpt.training.qltv.repository.projection.ReviewSummaryProjection;
import fpt.training.qltv.repository.spec.BookSpecification;
import fpt.training.qltv.service.BookService;
import fpt.training.qltv.service.CloudinaryService;
import fpt.training.qltv.service.PrivateFileService;
import java.time.LocalDateTime;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024L * 1024L;
    private static final long MAX_FILE_SIZE = 50L * 1024L * 1024L;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final ReviewRepository reviewRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final CloudinaryService cloudinaryService;
    private final PrivateFileService privateFileService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookResponse> findAll(BookFilterRequest filter, int page, int size) {
        BookFilterRequest safeFilter = filter == null ? new BookFilterRequest() : filter;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Book> specification = BookSpecification.of(
                safeFilter.getTitle(),
                safeFilter.getCategoryId(),
                safeFilter.getAuthorId(),
                safeFilter.getLanguage(),
                safeFilter.getStatus(),
                safeFilter.getPublishYear(),
                safeFilter.getDeleted());

        Page<BookSummaryProjection> result = bookRepository.findBy(specification, q -> q
                .as(BookSummaryProjection.class)
                .page(pageable));

        return PageResponse.of(result.map(this::toListResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "books", key = "#id")
    public BookDetailResponse findById(Long id) {
        Book book = getBookOrThrow(id);
        if (book.isDeleted()) {
            throw new ResourceNotFoundException("Book", id);
        }
        return toDetailResponse(book);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dashboard", allEntries = true)
    public BookResponse create(CreateBookRequest request, MultipartFile cover, MultipartFile file) {
        validateCreateRequest(request, cover, file);

        if (request.getIsbn() != null && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new BusinessException("ISBN đã tồn tại");
        }

        Book book = new Book();
        applyScalarFields(book, request.getTitle(), request.getIsbn(), request.getDescription(),
                request.getPublishYear(), request.getLanguage(), request.getTotalCopies(), request.getStatus());
        book.setCoverImageUrl(uploadCover(cover));
        book.setFileUrl(privateFileService.saveBookFile(file));
        applyAssociations(book, request.getCategoryIds(), request.getAuthorIds());
        normalizeAvailabilityAndStatus(book);
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());

        return toSummaryResponse(bookRepository.save(book));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = "books", key = "#id"),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public BookResponse update(Long id, UpdateBookRequest request, MultipartFile cover, MultipartFile file) {
        Book book = getBookOrThrow(id);

        if (request.getIsbn() != null && !request.getIsbn().equals(book.getIsbn())) {
            if (bookRepository.existsByIsbn(request.getIsbn())) {
                throw new BusinessException("ISBN đã tồn tại");
            }
        }

        applyScalarFields(
                book,
                request.getTitle() != null ? request.getTitle() : book.getTitle(),
                request.getIsbn() != null ? request.getIsbn() : book.getIsbn(),
                request.getDescription() != null ? request.getDescription() : book.getDescription(),
                request.getPublishYear() != null ? request.getPublishYear() : book.getPublishYear(),
                request.getLanguage() != null ? request.getLanguage() : book.getLanguage(),
                request.getTotalCopies() != null ? request.getTotalCopies() : book.getTotalCopies(),
                request.getStatus() != null ? request.getStatus() : book.getStatus());

        if (cover != null && !cover.isEmpty()) {
            validateImage(cover);
            String currentCoverUrl = book.getCoverImageUrl();
            String publicId = extractPublicId(currentCoverUrl);
            if (publicId != null && !publicId.isBlank()) {
                cloudinaryService.deleteImage(publicId);
            }
            book.setCoverImageUrl(uploadCover(cover));
        }

        if (file != null && !file.isEmpty()) {
            validateBookFile(file);
            privateFileService.deleteFile(book.getFileUrl());
            book.setFileUrl(privateFileService.saveBookFile(file));
        }

        if (request.getCategoryIds() != null) {
            applyCategories(book, request.getCategoryIds());
        }

        if (request.getAuthorIds() != null) {
            applyAuthors(book, request.getAuthorIds());
        }

        book.setUpdatedAt(LocalDateTime.now());
        normalizeAvailabilityAndStatus(book);
        return toSummaryResponse(bookRepository.save(book));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = "books", key = "#id"),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public void delete(Long id) {
        Book book = getBookOrThrow(id);
        if (book.isDeleted()) {
            throw new BusinessException("Sách đã được xóa từ trước");
        }

        boolean hasActiveBorrows = borrowRecordRepository.existsByBookIdAndStatusIn(
                id,
                List.of(BorrowStatus.BORROWING, BorrowStatus.OVERDUE));
        if (hasActiveBorrows) {
            throw new BusinessException("Không thể xóa sách đang được mượn chưa trả");
        }

        book.setDeleted(true);
        book.setStatus(BookStatus.INACTIVE);
        book.setAvailableCopies(0);
        book.setUpdatedAt(LocalDateTime.now());
        bookRepository.save(book);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = "books", key = "#id"),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public void restore(Long id) {
        Book book = getBookOrThrow(id);
        if (!book.isDeleted()) {
            throw new BusinessException("Sách chưa bị xóa");
        }

        // Kiểm tra xem có sách nào khác đang sử dụng ISBN này không
        if (bookRepository.existsByIsbnAndDeletedFalse(book.getIsbn())) {
            throw new BusinessException("Không thể khôi phục vì ISBN của sách này đã tồn tại trên một cuốn sách khác");
        }

        book.setDeleted(false);
        book.setStatus(BookStatus.AVAILABLE);
        book.setAvailableCopies(book.getTotalCopies());
        book.setUpdatedAt(LocalDateTime.now());
        bookRepository.save(book);
    }

    private void validateCreateRequest(CreateBookRequest request, MultipartFile cover, MultipartFile file) {
        validateImage(cover);
        validateBookFile(file);
    }

    private void applyScalarFields(Book book, String title, String isbn, String description, Integer publishYear,
            String language, Integer totalCopies, BookStatus requestedStatus) {
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setDescription(description);
        book.setPublishYear(publishYear);
        book.setLanguage(isBlank(language) ? "Tiếng Việt" : language);
        book.setTotalCopies(totalCopies == null ? 5 : totalCopies);
        if (book.getAvailableCopies() == null) {
            book.setAvailableCopies(book.getTotalCopies());
        }
        if (requestedStatus != null) {
            book.setStatus(requestedStatus);
        }
    }

    private void applyAssociations(Book book, List<Long> categoryIds, List<Long> authorIds) {
        applyCategories(book, categoryIds == null ? List.of() : categoryIds);
        applyAuthors(book, authorIds == null ? List.of() : authorIds);
    }

    private void applyCategories(Book book, List<Long> categoryIds) {
        if (categoryIds == null) {
            return;
        }
        if (categoryIds.isEmpty()) {
            book.getCategories().clear();
            return;
        }
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        ensureAllIdsResolved(categoryIds, categories.stream().map(Category::getId).collect(Collectors.toSet()),
                "Category");
        book.setCategories(new HashSet<>(categories));
    }

    private void applyAuthors(Book book, List<Long> authorIds) {
        if (authorIds == null) {
            return;
        }
        if (authorIds.isEmpty()) {
            book.getAuthors().clear();
            return;
        }
        List<Author> authors = authorRepository.findAllById(authorIds);
        ensureAllIdsResolved(authorIds, authors.stream().map(Author::getId).collect(Collectors.toSet()), "Author");
        book.setAuthors(new HashSet<>(authors));
    }

    private void ensureAllIdsResolved(List<Long> requestedIds, Set<Long> resolvedIds, String entityName) {
        for (Long requestedId : requestedIds) {
            if (!resolvedIds.contains(requestedId)) {
                throw new ResourceNotFoundException(entityName, requestedId);
            }
        }
    }

    private void normalizeAvailabilityAndStatus(Book book) {
        if (book.getTotalCopies() == null || book.getTotalCopies() < 0) {
            throw new BusinessException("Số lượng bản sách không hợp lệ");
        }

        if (book.getAvailableCopies() == null) {
            book.setAvailableCopies(book.getTotalCopies());
        }

        if (book.getAvailableCopies() > book.getTotalCopies()) {
            book.setAvailableCopies(book.getTotalCopies());
        }

        if (book.getStatus() == BookStatus.INACTIVE) {
            return;
        }

        book.setStatus(book.getAvailableCopies() > 0 ? BookStatus.AVAILABLE : BookStatus.OUT_OF_STOCK);
    }

    private Book getBookOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("Id sách không được để trống");
        }
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
    }

    private BookResponse toListResponse(BookSummaryProjection projection) {
        BookResponse response = new BookResponse();
        response.setId(projection.getId());
        response.setTitle(projection.getTitle());
        response.setIsbn(projection.getIsbn());
        response.setDescription(projection.getDescription());
        response.setCoverImageUrl(projection.getCoverImageUrl());
        response.setFileUrl(projection.getFileUrl());
        response.setPublishYear(projection.getPublishYear());
        response.setLanguage(projection.getLanguage());
        response.setTotalCopies(projection.getTotalCopies());
        response.setAvailableCopies(projection.getAvailableCopies());
        response.setStatus(projection.getStatus());
        response.setCreatedAt(projection.getCreatedAt());
        response.setUpdatedAt(projection.getUpdatedAt());
        // List view không cần categoryNames/authorNames/avgRating — đã giới hạn từ
        // query.
        response.setCategoryNames(null);
        response.setAuthorNames(null);
        response.setAvgRating(null);
        return response;
    }

    private BookResponse toSummaryResponse(Book book) {
        BookResponse response = new BookResponse();
        copyCommonFields(book, response);
        response.setCategoryNames(book.getCategories().stream()
                .map(Category::getName)
                .sorted()
                .toList());
        response.setAuthorNames(book.getAuthors().stream()
                .map(Author::getName)
                .sorted()
                .toList());
        response.setAvgRating(calculateAverageRating(book));
        return response;
    }

    private BookDetailResponse toDetailResponse(Book book) {
        BookDetailResponse response = new BookDetailResponse();
        copyCommonFields(book, response);
        response.setCategoryNames(book.getCategories().stream().map(Category::getName).sorted().toList());
        response.setAuthorNames(book.getAuthors().stream().map(Author::getName).sorted().toList());
        response.setCategoryIds(book.getCategories().stream()
                .map(Category::getId)
                .sorted()
                .toList());
        response.setAuthorIds(book.getAuthors().stream()
                .map(Author::getId)
                .sorted()
                .toList());
        response.setAvgRating(calculateAverageRating(book));
        response.setBorrowCount((long) book.getBorrowRecords().size());
        // Dùng projection — chỉ SELECT các cột cần của review + user.username, không
        // load toàn bộ entity.
        response.setReviews(reviewRepository.findProjectedByBookIdAndVisibleTrue(book.getId()).stream()
                .map(this::toReviewResponse)
                .sorted(Comparator
                        .comparing(ReviewResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .toList());
        return response;
    }

    private void copyCommonFields(Book book, BookResponse response) {
        response.setId(book.getId());
        response.setTitle(book.getTitle());
        response.setIsbn(book.getIsbn());
        response.setDescription(book.getDescription());
        response.setCoverImageUrl(book.getCoverImageUrl());
        response.setFileUrl(book.getFileUrl());
        response.setPublishYear(book.getPublishYear());
        response.setLanguage(book.getLanguage());
        response.setTotalCopies(book.getTotalCopies());
        response.setAvailableCopies(book.getAvailableCopies());
        response.setStatus(book.getStatus());
        response.setCreatedAt(book.getCreatedAt());
        response.setUpdatedAt(book.getUpdatedAt());
    }

    private Double calculateAverageRating(Book book) {
        List<Review> visibleReviews = book.getReviews() == null
                ? List.of()
                : book.getReviews().stream().filter(Review::isVisible).toList();

        if (visibleReviews.isEmpty()) {
            return 0.0;
        }

        return visibleReviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    private ReviewResponse toReviewResponse(ReviewSummaryProjection projection) {
        ReviewResponse response = new ReviewResponse();
        response.setId(projection.getId());
        response.setRating(projection.getRating());
        response.setComment(projection.getComment());
        response.setVisible(projection.isVisible());
        response.setUserId(projection.getUser().getId());
        response.setUsername(projection.getUser().getUsername());
        response.setBookId(projection.getBook().getId());
        response.setCreatedAt(projection.getCreatedAt());
        return response;
    }

    private String uploadCover(MultipartFile file) {
        validateImage(file);
        return cloudinaryService.uploadImage(file, "book-covers");
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("Ảnh bìa không được để trống");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new FileUploadException("Ảnh bìa không được vượt quá 5MB");
        }
        if (!IMAGE_TYPES.contains(normalizeContentType(file.getContentType()))) {
            throw new FileUploadException("Ảnh bìa chỉ chấp nhận JPG, PNG, WEBP");
        }
    }

    private void validateBookFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File sách không được để trống");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException("File sách không được vượt quá 50MB");
        }
        if (!"application/pdf".equalsIgnoreCase(normalizeContentType(file.getContentType()))) {
            throw new FileUploadException("File sách chỉ chấp nhận định dạng PDF");
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String extractPublicId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            int uploadIndex = path.indexOf("/upload/");
            String trimmed = uploadIndex >= 0 ? path.substring(uploadIndex + 8) : path;
            if (trimmed.startsWith("/")) {
                trimmed = trimmed.substring(1);
            }
            if (trimmed.startsWith("v") && trimmed.length() > 1 && Character.isDigit(trimmed.charAt(1))) {
                int slashIndex = trimmed.indexOf('/');
                if (slashIndex > 0) {
                    trimmed = trimmed.substring(slashIndex + 1);
                }
            }
            int dotIndex = trimmed.lastIndexOf('.');
            return dotIndex > 0 ? trimmed.substring(0, dotIndex) : trimmed;
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
