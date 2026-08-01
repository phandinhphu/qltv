package fpt.training.qltv.repository.spec;

import fpt.training.qltv.entity.Book;
import fpt.training.qltv.entity.BookStatus;
import org.springframework.data.jpa.domain.Specification;

public final class BookSpecification {

    private BookSpecification() {
    }

    public static Specification<Book> of(
            String title,
            Long categoryId,
            Long authorId,
            String language,
            BookStatus status,
            Integer publishYear,
            Boolean deleted) {
        Specification<Book> specification = (root, query, cb) -> cb.conjunction();
        specification = specification.and(hasTitle(title));
        specification = specification.and(hasCategoryId(categoryId));
        specification = specification.and(hasAuthorId(authorId));
        specification = specification.and(hasLanguage(language));
        specification = specification.and(hasStatus(status));
        specification = specification.and(hasPublishYear(publishYear));
        specification = specification.and(hasDeleted(deleted));
        return specification;
    }

    private static Specification<Book> hasTitle(String title) {
        return (root, query, cb) -> {
            if (title == null || title.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
        };
    }

    private static Specification<Book> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) {
                return null;
            }
            query.distinct(true);
            return cb.equal(root.join("categories").get("id"), categoryId);
        };
    }

    private static Specification<Book> hasAuthorId(Long authorId) {
        return (root, query, cb) -> {
            if (authorId == null) {
                return null;
            }
            query.distinct(true);
            return cb.equal(root.join("authors").get("id"), authorId);
        };
    }

    private static Specification<Book> hasLanguage(String language) {
        return (root, query, cb) -> {
            if (language == null || language.isBlank()) {
                return null;
            }
            return cb.equal(cb.lower(root.get("language")), language.toLowerCase());
        };
    }

    private static Specification<Book> hasStatus(BookStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    private static Specification<Book> hasPublishYear(Integer publishYear) {
        return (root, query, cb) -> {
            if (publishYear == null) {
                return null;
            }
            return cb.equal(root.get("publishYear"), publishYear);
        };
    }

    private static Specification<Book> hasDeleted(Boolean deleted) {
        return (root, query, cb) -> {
            if (deleted == null) {
                return cb.equal(root.get("deleted"), false);
            }
            return cb.equal(root.get("deleted"), deleted);
        };
    }
}
