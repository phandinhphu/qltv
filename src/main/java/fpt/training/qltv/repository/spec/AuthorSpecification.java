package fpt.training.qltv.repository.spec;

import fpt.training.qltv.entity.Author;
import org.springframework.data.jpa.domain.Specification;

public final class AuthorSpecification {

    private AuthorSpecification() {
    }

    public static Specification<Author> of(String name, Boolean deleted) {
        Specification<Author> specification = (root, query, cb) -> cb.conjunction();
        specification = specification.and(hasName(name));
        specification = specification.and(hasDeleted(deleted));
        return specification;
    }

    private static Specification<Author> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    private static Specification<Author> hasDeleted(Boolean deleted) {
        return (root, query, cb) -> {
            if (deleted == null) {
                return cb.equal(root.get("deleted"), false);
            }
            return cb.equal(root.get("deleted"), deleted);
        };
    }
}
