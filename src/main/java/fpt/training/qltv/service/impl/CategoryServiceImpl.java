package fpt.training.qltv.service.impl;

import fpt.training.qltv.dto.request.CreateCategoryRequest;
import fpt.training.qltv.dto.request.UpdateCategoryRequest;
import fpt.training.qltv.dto.response.CategoryResponse;
import fpt.training.qltv.entity.Category;
import fpt.training.qltv.exception.common.BusinessException;
import fpt.training.qltv.exception.common.ResourceNotFoundException;
import fpt.training.qltv.repository.CategoryRepository;
import fpt.training.qltv.service.CategoryService;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories_all")
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByDeletedFalse().stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllDeleted() {
        return categoryRepository.findAllByDeletedTrue().stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#id")
    public CategoryResponse findById(Long id) {
        return toResponse(getActiveCategoryOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "categories_all", allEntries = true)
    public CategoryResponse create(CreateCategoryRequest request) {
        Category category = new Category();
        String trimmedName = request.getName().trim();
        category.setName(trimmedName);
        category.setDescription(request.getDescription());
        category.setSlug(generateSlug(trimmedName));
        ensureSlugUnique(category.getSlug(), null);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = "categories", key = "#id"),
        @CacheEvict(value = "categories_all", allEntries = true)
    })
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        Category category = getActiveCategoryOrThrow(id);

        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            String newSlug = generateSlug(trimmedName);
            ensureSlugUnique(newSlug, category.getId());
            category.setName(trimmedName);
            category.setSlug(newSlug);
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        category.setUpdatedAt(LocalDateTime.now());
        return toResponse(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = "categories", key = "#id"),
        @CacheEvict(value = "categories_all", allEntries = true)
    })
    public void delete(Long id) {
        Category category = getActiveCategoryOrThrow(id);
        boolean hasActiveBooks = category.getBooks().stream().anyMatch(book -> !book.isDeleted());
        if (hasActiveBooks) {
            throw new BusinessException("Không thể xóa danh mục vì vẫn còn sách đang hoạt động thuộc danh mục này");
        }
        category.setDeleted(true);
        category.setUpdatedAt(LocalDateTime.now());
        categoryRepository.save(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = "categories", key = "#id"),
        @CacheEvict(value = "categories_all", allEntries = true)
    })
    public void restore(Long id) {
        Category category = getCategoryOrThrow(id);
        if (!category.isDeleted()) {
            throw new BusinessException("Danh mục chưa bị xóa");
        }
        if (categoryRepository.existsByNameAndDeletedFalse(category.getName())) {
            throw new BusinessException("Không thể khôi phục vì tên danh mục này đã được sử dụng bởi một danh mục khác");
        }
        if (categoryRepository.existsBySlugAndDeletedFalse(category.getSlug())) {
            throw new BusinessException("Không thể khôi phục vì slug danh mục này đã được sử dụng bởi một danh mục khác");
        }
        category.setDeleted(false);
        category.setUpdatedAt(LocalDateTime.now());
        categoryRepository.save(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = "categories", key = "#id"),
        @CacheEvict(value = "categories_all", allEntries = true)
    })
    public void forceDelete(Long id) {
        Category category = getCategoryOrThrow(id);
        if (!category.isDeleted()) {
            throw new BusinessException("Danh mục phải được xóa mềm trước khi xóa vĩnh viễn");
        }
        if (category.getBooks() != null && !category.getBooks().isEmpty()) {
            throw new BusinessException("Không thể xóa vĩnh viễn danh mục vì vẫn còn sách liên kết (kể cả sách đã xóa mềm)");
        }
        categoryRepository.delete(category);
    }

    private void ensureSlugUnique(String slug, Long excludedCategoryId) {
        categoryRepository.findBySlug(slug).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), excludedCategoryId)) {
                if (existing.isDeleted()) {
                    throw new BusinessException("Thể loại trùng với một thể loại đã bị xóa tạm thời trong Thùng rác. Bạn cần khôi phục lại hoặc xóa vĩnh viễn thể loại cũ để sử dụng tên này.");
                } else {
                    throw new BusinessException("Slug danh mục đã tồn tại: " + slug);
                }
            }
        });
    }

    private Category getCategoryOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("Id danh mục không được để trống");
        }
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private Category getActiveCategoryOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("Id danh mục không được để trống");
        }
        Category category = getCategoryOrThrow(id);
        if (category.isDeleted()) {
            throw new ResourceNotFoundException("Category", id);
        }
        return category;
    }

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutAccents
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
    }

    private CategoryResponse toResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setSlug(category.getSlug());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        return response;
    }
}
