package fpt.training.qltv.service.impl;

import fpt.training.qltv.dto.response.BookResponse;
import fpt.training.qltv.dto.response.DashboardResponse;
import fpt.training.qltv.entity.Book;
import fpt.training.qltv.entity.BorrowStatus;
import fpt.training.qltv.entity.Role;
import fpt.training.qltv.repository.BookRepository;
import fpt.training.qltv.repository.BorrowRecordRepository;
import fpt.training.qltv.repository.UserRepository;
import fpt.training.qltv.service.DashboardService;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard")
    public DashboardResponse getDashboard() {
        DashboardResponse response = new DashboardResponse();
        response.setTotalBooks(bookRepository.count());
        response.setTotalUsers(userRepository.countByRole(Role.USER));
        response.setTotalActiveBorrows(
                borrowRecordRepository.countByStatus(BorrowStatus.BORROWING));
        response.setTotalOverdue(borrowRecordRepository.countByStatus(BorrowStatus.OVERDUE));
        response.setTopBorrowedBooks(getTopBorrowedBooks());
        response.setBorrowCountByMonth(getBorrowCountByMonth());
        return response;
    }

    private List<BookResponse> getTopBorrowedBooks() {
        // Bước 1: aggregate query nhẹ — chỉ lấy IDs, tránh conflict JOIN FETCH + pagination
        List<Long> topIds = borrowRecordRepository.findTopBorrowedBookIds(PageRequest.of(0, 5));
        if (topIds.isEmpty()) {
            return List.of();
        }
        // Bước 2: load Book đầy đủ (categories + authors + reviews) trong 1 query @EntityGraph
        // Giữ thứ tự theo topIds (borrow count desc)
        Map<Long, Book> bookById =
                bookRepository.findAllByIdIn(topIds).stream()
                        .collect(Collectors.toMap(Book::getId, b -> b));
        return topIds.stream()
                .filter(bookById::containsKey)
                .map(id -> toBookResponse(bookById.get(id)))
                .toList();
    }

    private Map<String, Long> getBorrowCountByMonth() {
        LocalDateTime fromDate = YearMonth.now().minusMonths(11).atDay(1).atStartOfDay();
        Map<String, Long> monthlyCounts = new LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            monthlyCounts.put(month.toString(), 0L);
        }

        for (Object[] row : borrowRecordRepository.countBorrowByMonth(fromDate)) {
            String monthKey = String.valueOf(row[0]);
            Long count = ((Number) row[1]).longValue();
            monthlyCounts.put(monthKey, count);
        }

        return monthlyCounts;
    }

    private BookResponse toBookResponse(Book book) {
        BookResponse response = new BookResponse();
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
        response.setCategoryNames(
                book.getCategories().stream().map(category -> category.getName()).toList());
        response.setAuthorNames(
                book.getAuthors().stream().map(author -> author.getName()).toList());
        response.setAvgRating(
                book.getReviews().isEmpty()
                        ? 0.0
                        : book.getReviews().stream()
                                .filter(review -> review.isVisible())
                                .mapToInt(review -> review.getRating())
                                .average()
                                .orElse(0.0));
        response.setCreatedAt(book.getCreatedAt());
        response.setUpdatedAt(book.getUpdatedAt());
        return response;
    }
}
