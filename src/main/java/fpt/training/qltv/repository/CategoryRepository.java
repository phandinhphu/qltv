package fpt.training.qltv.repository;

import fpt.training.qltv.entity.Category;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);
}
