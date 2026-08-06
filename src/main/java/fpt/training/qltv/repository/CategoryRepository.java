package fpt.training.qltv.repository;

import fpt.training.qltv.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Tự động chỉ tìm active (nhờ @SQLRestriction)
    Optional<Category> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    // ---- Trash bin queries (bypass @SQLRestriction bằng native SQL) ----

    @Query(value = "SELECT * FROM categories WHERE deleted = true ORDER BY updated_at DESC", nativeQuery = true)
    List<Category> findAllDeleted();

    @Query(value = "SELECT * FROM categories WHERE id = :id AND deleted = true", nativeQuery = true)
    Optional<Category> findByIdDeleted(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM book_categories bc " +
                   "JOIN books b ON bc.book_id = b.id " +
                   "WHERE bc.category_id = :categoryId AND b.deleted = false", nativeQuery = true)
    long countActiveBooksByCategoryId(@Param("categoryId") Long categoryId);
}
