package fpt.training.qltv.repository;

import fpt.training.qltv.entity.Author;
import fpt.training.qltv.repository.projection.AuthorSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long>, JpaSpecificationExecutor<Author> {

    boolean existsByName(String name);

    java.util.Optional<Author> findByName(String name);

    Page<AuthorSummaryProjection> findBy(Specification<Author> spec, Pageable pageable);

    Optional<Author> findByIdAndDeletedFalse(Long id);

    boolean existsByNameAndDeletedFalse(String name);
}
