package fpt.training.qltv.repository;

import fpt.training.qltv.entity.Book;
import java.util.Optional;
// import fpt.training.qltv.repository.projection.BookSummaryProjection;
// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.Pageable;
// import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    long countByDeletedFalse();

    boolean existsByIsbnAndDeletedFalse(String isbn);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdWithLock(@Param("id") Long id);
}
