package fpt.training.qltv.repository;

import fpt.training.qltv.entity.Author;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorRepository extends JpaRepository<Author, Long>, JpaSpecificationExecutor<Author> {

    // Tự động chỉ tìm active (nhờ @SQLRestriction)
    boolean existsByName(String name);

    Optional<Author> findByName(String name);

    // ---- Trash bin queries (bypass @SQLRestriction bằng native SQL) ----

    @Query(value = "SELECT * FROM authors WHERE deleted = true ORDER BY updated_at DESC", nativeQuery = true)
    List<Author> findAllDeleted();

    @Query(value = "SELECT * FROM authors WHERE id = :id AND deleted = true", nativeQuery = true)
    Optional<Author> findByIdDeleted(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM book_authors ba " +
                   "JOIN books b ON ba.book_id = b.id " +
                   "WHERE ba.author_id = :authorId AND b.deleted = false", nativeQuery = true)
    long countActiveBooksByAuthorId(@Param("authorId") Long authorId);
}
