package fpt.training.qltv.repository;

import fpt.training.qltv.entity.BorrowRecord;
import fpt.training.qltv.entity.BorrowStatus;
import fpt.training.qltv.repository.projection.BorrowRecordProjection;
import fpt.training.qltv.repository.projection.BorrowTokenProjection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long>, JpaSpecificationExecutor<BorrowRecord> {

    Optional<BorrowRecord> findByDownloadToken(String downloadToken);

    List<BorrowRecord> findByUserIdAndStatus(Long userId, BorrowStatus status);

    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, BorrowStatus status);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    boolean existsByBookIdAndStatusIn(Long bookId, java.util.Collection<BorrowStatus> statuses);

    long countByUserIdAndStatus(Long userId, BorrowStatus status);

    long countByStatus(BorrowStatus status);

    @Query("""
        select br
        from BorrowRecord br
        where br.status = fpt.training.qltv.entity.BorrowStatus.BORROWING
          and br.dueDate < :now
        """)
    List<BorrowRecord> findOverdueRecords(@Param("now") LocalDateTime now);

    @Modifying
    @Query("""
        update BorrowRecord br
        set br.status = fpt.training.qltv.entity.BorrowStatus.OVERDUE
        where br.status = fpt.training.qltv.entity.BorrowStatus.BORROWING
          and br.dueDate < :now
        """)
    int markOverdue(@Param("now") LocalDateTime now);

    @Query("""
        select count(br)
        from BorrowRecord br
        where br.status = fpt.training.qltv.entity.BorrowStatus.BORROWING
        """)
    long countActiveBorrows();

    @Query("""
      select br.book
      from BorrowRecord br
      group by br.book
      order by count(br) desc
      """)
    List<fpt.training.qltv.entity.Book> findTopBorrowedBooks(Pageable pageable);

    @Query("""
      select function('date_format', br.borrowDate, '%Y-%m'), count(br)
      from BorrowRecord br
      where br.borrowDate >= :fromDate
      group by function('date_format', br.borrowDate, '%Y-%m')
      order by function('date_format', br.borrowDate, '%Y-%m')
      """)
    List<Object[]> countBorrowByMonth(@Param("fromDate") LocalDateTime fromDate);

    List<BorrowRecord> findByUserIdAndBookId(Long userId, Long bookId);

    /**
     * Trả về danh sách borrow record với projection (SELECT NEW) — flatten cross-table fields
     * từ user (username) và book (title, fileUrl) thành một object phẳng.
     * Thay thế Specification-based findAll cho các query read-only list/page.
     * Các tham số null được bỏ qua (optional filter).
     */
    @Query(value = """
        select new fpt.training.qltv.repository.projection.BorrowRecordProjection(
            br.id,
            br.user.id,
            u.username,
            br.book.id,
            b.title,
            b.fileUrl,
            br.borrowDate,
            br.dueDate,
            br.returnDate,
            br.status,
            br.downloadToken,
            br.tokenExpiredAt
        )
        from BorrowRecord br
        join br.user u
        join br.book b
        where (:userId is null or br.user.id = :userId)
          and (:bookId is null or br.book.id = :bookId)
          and (:status is null or br.status = :status)
          and (:fromDate is null or br.borrowDate >= :fromDate)
          and (:toDate is null or br.borrowDate <= :toDate)
        """,
        countQuery = """
        select count(br)
        from BorrowRecord br
        where (:userId is null or br.user.id = :userId)
          and (:bookId is null or br.book.id = :bookId)
          and (:status is null or br.status = :status)
          and (:fromDate is null or br.borrowDate >= :fromDate)
          and (:toDate is null or br.borrowDate <= :toDate)
        """)
    Page<BorrowRecordProjection> findAllProjected(
        @Param("userId") Long userId,
        @Param("bookId") Long bookId,
        @Param("status") BorrowStatus status,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
    );

    Optional<BorrowTokenProjection> findByDownloadToken(String downloadToken, Class<BorrowTokenProjection> type);
}
