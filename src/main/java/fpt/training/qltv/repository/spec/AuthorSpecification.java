package fpt.training.qltv.repository.spec;

import fpt.training.qltv.entity.Author;
import org.springframework.data.jpa.domain.Specification;

public final class AuthorSpecification {

    private AuthorSpecification() {}

    /**
     * Xây dựng Specification filter cho Author. Điều kiện "deleted = false" được @SQLRestriction
     * trên entity lo tự động, không cần thêm ở đây nữa.
     */
    public static Specification<Author> of(String name) {
        return hasName(name);
    }

    private static Specification<Author> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }
}
