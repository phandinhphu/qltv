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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories_all")
    public List<CategoryResponse> findAll() {
        // @SQLRestriction tự động chỉ lấy deleted = false — không cần findAllByDeletedFalse nữa
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllDeleted() {
        // Native query bypass @SQLRestriction để lấy trash bin
        return categoryRepository.findAllDeleted().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#id")
    public CategoryResponse findById(Long id) {
        // findById đã bị @SQLRestriction filter — nếu đã xóa sẽ trả về empty → 404
        return toResponse(getCategoryOrThrow(id));
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

        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                @CacheEvict(value = "categories", key = "#id"),
                @CacheEvict(value = "categories_all", allEntries = true)
            })
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        Category category = getCategoryOrThrow(id);

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
        return toResponse(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                @CacheEvict(value = "categories", key = "#id"),
                @CacheEvict(value = "categories_all", allEntries = true)
            })
    public void delete(Long id) {
        Category category = getCategoryOrThrow(id);
        // Dùng native query đếm sách active — không bị ảnh hưởng bởi @SQLRestriction trên
        // collection
        long activeBookCount = categoryRepository.countActiveBooksByCategoryId(id);
        if (activeBookCount > 0) {
            throw new BusinessException(
                    "Không thể xóa danh mục vì vẫn còn sách đang hoạt động thuộc danh mục này");
        }
        category.setDeleted(true);
        categoryRepository.save(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                @CacheEvict(value = "categories", key = "#id"),
                @CacheEvict(value = "categories_all", allEntries = true)
            })
    public void restore(Long id) {
        // Cần bypass @SQLRestriction để tìm bản ghi đã xóa
        Category category = getDeletedCategoryOrThrow(id);
        if (categoryRepository.existsByName(category.getName())) {
            throw new BusinessException(
                    "Không thể khôi phục vì tên danh mục này đã được sử dụng bởi một danh mục khác");
        }
        if (categoryRepository.existsBySlug(category.getSlug())) {
            throw new BusinessException(
                    "Không thể khôi phục vì slug danh mục này đã được sử dụng bởi một danh mục khác");
        }
        category.setDeleted(false);
        categoryRepository.save(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                @CacheEvict(value = "categories", key = "#id"),
                @CacheEvict(value = "categories_all", allEntries = true)
            })
    public void forceDelete(Long id) {
        Category category = getDeletedCategoryOrThrow(id);
        if (category.getBooks() != null && !category.getBooks().isEmpty()) {
            throw new BusinessException(
                    "Không thể xóa vĩnh viễn danh mục vì vẫn còn sách liên kết (kể cả sách đã xóa mềm)");
        }
        categoryRepository.delete(category);
    }

    // ---- Helper: lấy category active (bị @SQLRestriction filter, 404 nếu đã xóa) ----

    private Category getCategoryOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("Id danh mục không được để trống");
        }
        return categoryRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    // ---- Helper: lấy category đã xóa (bypass @SQLRestriction bằng native query) ----

    private Category getDeletedCategoryOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("Id danh mục không được để trống");
        }
        return categoryRepository
                .findByIdDeleted(id)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        "Danh mục đã xóa không tồn tại hoặc chưa được xóa mềm"));
    }

    private void ensureSlugUnique(String slug, Long excludedCategoryId) {
        categoryRepository
                .findBySlug(slug)
                .ifPresent(
                        existing -> {
                            if (!Objects.equals(existing.getId(), excludedCategoryId)) {
                                // findBySlug đã chỉ tìm active (nhờ @SQLRestriction) nên không cần
                                // check isDeleted()
                                throw new BusinessException("Slug danh mục đã tồn tại: " + slug);
                            }
                        });
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
