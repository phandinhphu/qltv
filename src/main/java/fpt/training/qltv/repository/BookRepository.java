package fpt.training.qltv.repository;

import fpt.training.qltv.entity.Book;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    // ---- Trash bin queries (bypass @SQLRestriction bằng native SQL) ----

    @Query(value = "SELECT * FROM books WHERE id = :id AND deleted = true", nativeQuery = true)
    Optional<Book> findByIdDeleted(@Param("id") Long id);

    @Query(value = "SELECT * FROM books WHERE deleted = true ORDER BY updated_at DESC", nativeQuery = true)
    List<Book> findAllDeleted();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdWithLock(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Book b SET b.availableCopies = COALESCE(b.availableCopies, 0) + 1, " +
            "b.status = CASE WHEN b.status != 'INACTIVE' THEN 'AVAILABLE' ELSE b.status END " +
            "WHERE b.id = :bookId")
    int incrementAvailableCopies(@Param("bookId") Long bookId);
}
