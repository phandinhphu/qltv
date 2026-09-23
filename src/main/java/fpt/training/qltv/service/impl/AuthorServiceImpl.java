package fpt.training.qltv.service.impl;

import fpt.training.qltv.dto.request.AuthorFilterRequest;
import fpt.training.qltv.dto.request.CreateAuthorRequest;
import fpt.training.qltv.dto.request.UpdateAuthorRequest;
import fpt.training.qltv.dto.response.AuthorResponse;
import fpt.training.qltv.dto.response.PageResponse;
import fpt.training.qltv.entity.Author;
import fpt.training.qltv.exception.common.BusinessException;
import fpt.training.qltv.exception.common.FileUploadException;
import fpt.training.qltv.exception.common.ResourceNotFoundException;
import fpt.training.qltv.repository.AuthorRepository;
import fpt.training.qltv.repository.projection.AuthorSummaryProjection;
import fpt.training.qltv.repository.spec.AuthorSpecification;
import fpt.training.qltv.service.AuthorService;
import fpt.training.qltv.service.CloudinaryService;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private static final long MAX_AVATAR_SIZE = 5L * 1024L * 1024L;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final AuthorRepository authorRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuthorResponse> findAll(AuthorFilterRequest filter, int page, int size) {
        AuthorFilterRequest safeFilter = filter == null ? new AuthorFilterRequest() : filter;
        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.max(size, 1),
                        Sort.by(Sort.Direction.DESC, "createdAt"));
        // @SQLRestriction tự động thêm "deleted = false" — không cần truyền tham số deleted nữa
        Specification<Author> specification = AuthorSpecification.of(safeFilter.getName());

        Page<AuthorSummaryProjection> result =
                authorRepository.findBy(
                        specification, q -> q.as(AuthorSummaryProjection.class).page(pageable));

        return PageResponse.of(result.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuthorResponse> findAllDeleted(int page, int size) {
        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.max(size, 1),
                        Sort.by(Sort.Direction.DESC, "updatedAt"));
        // Native query bypass @SQLRestriction để lấy trash bin
        List<Author> deletedAuthors = authorRepository.findAllDeleted();
        // Thực hiện phân trang thủ công trên kết quả native query
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), deletedAuthors.size());
        List<Author> pageContent =
                start >= deletedAuthors.size() ? List.of() : deletedAuthors.subList(start, end);
        Page<Author> result = new PageImpl<>(pageContent, pageable, deletedAuthors.size());
        return PageResponse.of(result.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "authors", key = "#id")
    public AuthorResponse findById(Long id) {
        // findById đã bị @SQLRestriction filter — nếu đã xóa sẽ trả về empty → 404
        return toResponse(getAuthorOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthorResponse create(CreateAuthorRequest request, MultipartFile avatar) {
        ensureNameUnique(request.getName(), null);
        Author author = new Author();
        author.setName(request.getName().trim());
        author.setBio(request.getBio());
        if (avatar != null && !avatar.isEmpty()) {
            validateAvatar(avatar);
            author.setAvatarUrl(uploadAvatar(avatar));
        }

        return toResponse(authorRepository.save(author));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "authors", key = "#id")
    public AuthorResponse update(Long id, UpdateAuthorRequest request, MultipartFile avatar) {
        Author author = getAuthorOrThrow(id);
        if (request.getName() != null) {
            ensureNameUnique(request.getName(), author.getId());
            author.setName(request.getName().trim());
        }

        if (request.getBio() != null) {
            author.setBio(request.getBio());
        }

        if (avatar != null && !avatar.isEmpty()) {
            validateAvatar(avatar);
            String currentAvatarUrl = author.getAvatarUrl();
            String publicId = extractPublicId(currentAvatarUrl);
            if (publicId != null && !publicId.isBlank()) {
                cloudinaryService.deleteImage(publicId);
            }
            author.setAvatarUrl(uploadAvatar(avatar));
        }

        return toResponse(author);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "authors", key = "#id")
    public void delete(Long id) {
        Author author = getAuthorOrThrow(id);
        // Dùng native query đếm sách active — không bị ảnh hưởng bởi @SQLRestriction trên
        // collection
        long activeBookCount = authorRepository.countActiveBooksByAuthorId(id);
        if (activeBookCount > 0) {
            throw new BusinessException(
                    "Không thể xóa tác giả vì vẫn còn sách đang hoạt động liên kết với tác giả này");
        }
        author.setDeleted(true);
        authorRepository.save(author);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "authors", key = "#id")
    public void restore(Long id) {
        // Cần bypass @SQLRestriction để tìm bản ghi đã xóa
        Author author = getDeletedAuthorOrThrow(id);
        if (authorRepository.existsByName(author.getName())) {
            throw new BusinessException(
                    "Không thể khôi phục vì tên tác giả này đã được sử dụng bởi một tác giả khác");
        }
        author.setDeleted(false);
        authorRepository.save(author);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "authors", key = "#id")
    public void forceDelete(Long id) {
        Author author = getDeletedAuthorOrThrow(id);
        // Kiểm tra sách liên kết bằng native query (bao gồm cả sách đã xóa mềm)
        long totalBookCount = authorRepository.countActiveBooksByAuthorId(id);
        if (totalBookCount > 0) {
            throw new BusinessException(
                    "Không thể xóa vĩnh viễn tác giả vì vẫn còn sách liên kết (kể cả sách đã xóa mềm)");
        }
        String currentAvatarUrl = author.getAvatarUrl();
        String publicId = extractPublicId(currentAvatarUrl);
        if (publicId != null && !publicId.isBlank()) {
            cloudinaryService.deleteImage(publicId);
        }
        authorRepository.delete(author);
    }

    // ---- Helper: lấy author active (bị @SQLRestriction filter, 404 nếu đã xóa) ----

    private Author getAuthorOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("Id tác giả không được để trống");
        }
        return authorRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", id));
    }

    // ---- Helper: lấy author đã xóa (bypass @SQLRestriction bằng native query) ----

    private Author getDeletedAuthorOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("Id tác giả không được để trống");
        }
        return authorRepository
                .findByIdDeleted(id)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        "Tác giả đã xóa không tồn tại hoặc chưa được xóa mềm"));
    }

    private void ensureNameUnique(String name, Long excludedAuthorId) {
        String trimmedName = name.trim();
        authorRepository
                .findByName(trimmedName)
                .ifPresent(
                        existing -> {
                            if (!existing.getId().equals(excludedAuthorId)) {
                                // findByName đã chỉ tìm active (nhờ @SQLRestriction) nên không cần
                                // check isDeleted()
                                throw new BusinessException(
                                        "Tác giả đã tồn tại với tên: " + trimmedName);
                            }
                        });
    }

    private void validateAvatar(MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new FileUploadException("Ảnh tác giả không được để trống");
        }
        if (avatar.getSize() > MAX_AVATAR_SIZE) {
            throw new FileUploadException("Ảnh tác giả không được vượt quá 5MB");
        }
        String contentType =
                avatar.getContentType() == null
                        ? ""
                        : avatar.getContentType().trim().toLowerCase(Locale.ROOT);
        if (!IMAGE_TYPES.contains(contentType)) {
            throw new FileUploadException("Ảnh tác giả chỉ chấp nhận JPG, PNG, WEBP");
        }
    }

    private String uploadAvatar(MultipartFile avatar) {
        return cloudinaryService.uploadImage(avatar, "author-avatars");
    }

    private String extractPublicId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            int uploadIndex = path.indexOf("/upload/");
            String trimmed = uploadIndex >= 0 ? path.substring(uploadIndex + 8) : path;
            if (trimmed.startsWith("/")) {
                trimmed = trimmed.substring(1);
            }
            if (trimmed.startsWith("v")
                    && trimmed.length() > 1
                    && Character.isDigit(trimmed.charAt(1))) {
                int slashIndex = trimmed.indexOf('/');
                if (slashIndex > 0) {
                    trimmed = trimmed.substring(slashIndex + 1);
                }
            }
            int dotIndex = trimmed.lastIndexOf('.');
            return dotIndex > 0 ? trimmed.substring(0, dotIndex) : trimmed;
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private AuthorResponse toResponse(Author author) {
        AuthorResponse response = new AuthorResponse();
        response.setId(author.getId());
        response.setName(author.getName());
        response.setBio(author.getBio());
        response.setAvatarUrl(author.getAvatarUrl());
        response.setCreatedAt(author.getCreatedAt());
        response.setUpdatedAt(author.getUpdatedAt());
        return response;
    }

    private AuthorResponse toResponse(AuthorSummaryProjection projection) {
        AuthorResponse response = new AuthorResponse();
        response.setId(projection.getId());
        response.setName(projection.getName());
        response.setBio(projection.getBio());
        response.setAvatarUrl(projection.getAvatarUrl());
        response.setCreatedAt(projection.getCreatedAt());
        response.setUpdatedAt(projection.getUpdatedAt());
        return response;
    }
}
