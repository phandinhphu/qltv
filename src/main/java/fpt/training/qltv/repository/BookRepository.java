package fpt.training.qltv.repository;

import fpt.training.qltv.entity.Book;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    // ---- Trash bin queries (bypass @SQLRestriction bằng native SQL) ----

    @Query(value = "SELECT * FROM books WHERE id = :id AND deleted = true", nativeQuery = true)
    Optional<Book> findByIdDeleted(@Param("id") Long id);

    @Query(
            value = "SELECT * FROM books WHERE deleted = true ORDER BY updated_at DESC",
            nativeQuery = true)
    List<Book> findAllDeleted();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdWithLock(@Param("id") Long id);

    /**
     * Load Book kèm categories, authors và borrowRecords trong 1 query. Dùng cho GET /books/{id}
     * (detail view) — tránh 3 lazy query phụ khi toDetailResponse() truy cập 3 collection này.
     * reviews không cần là vì toDetailResponse() gọi reviewRepository riêng (projection).
     */
    @EntityGraph(attributePaths = {"categories", "authors", "borrowRecords"})
    Optional<Book> findWithDetailById(Long id);

    /**
     * Load danh sách Book theo ID kèm categories, authors và reviews trong 1 query. Dùng cho
     * Dashboard top-borrowed: sau khi lấy IDs từ aggregate query, load đầy đủ associations để
     * toBookResponse() không trigger lazy load.
     */
    @EntityGraph(attributePaths = {"categories", "authors", "reviews"})
    List<Book> findAllByIdIn(Collection<Long> ids);

    @Modifying
    @Query(
            "UPDATE Book b SET b.availableCopies = COALESCE(b.availableCopies, 0) + 1, "
                    + "b.status = CASE WHEN b.status != 'INACTIVE' THEN 'AVAILABLE' ELSE b.status END "
                    + "WHERE b.id = :bookId")
    int incrementAvailableCopies(@Param("bookId") Long bookId);
}
