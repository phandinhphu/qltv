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
import java.time.LocalDateTime;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
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
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Author> specification = AuthorSpecification.hasName(safeFilter.getName());

        Page<AuthorSummaryProjection> result = authorRepository.findBy(specification, q -> q
            .as(AuthorSummaryProjection.class)
            .page(pageable)
        );

        return PageResponse.of(result.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "authors", key = "#id")
    public AuthorResponse findById(Long id) {
        return toResponse(getAuthorOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthorResponse create(CreateAuthorRequest request, MultipartFile avatar) {
        if (authorRepository.existsByName(request.getName().trim())) {
            throw new BusinessException("Tác giả đã tồn tại với tên: " + request.getName().trim());
        }
        Author author = new Author();
        author.setName(request.getName().trim());
        author.setBio(request.getBio());
        author.setCreatedAt(LocalDateTime.now());
        author.setUpdatedAt(LocalDateTime.now());
        if (avatar != null && !avatar.isEmpty()) {
            validateAvatar(avatar);
            String currentAvatarUrl = author.getAvatarUrl();
            String publicId = extractPublicId(currentAvatarUrl);
            if (publicId != null && !publicId.isBlank()) {
                cloudinaryService.deleteImage(publicId);
            }
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
            String trimmedName = request.getName().trim();
            if (authorRepository.existsByName(trimmedName) && !trimmedName.equals(author.getName())) {
                throw new BusinessException("Tác giả đã tồn tại với tên: " + trimmedName);
            }
            author.setName(trimmedName);
        }

        if (request.getBio() != null) {
            author.setBio(request.getBio());
        }

        if (avatar != null && !avatar.isEmpty()) {
            validateAvatar(avatar);
            author.setAvatarUrl(uploadAvatar(avatar));
        }

        author.setUpdatedAt(LocalDateTime.now());
        return toResponse(authorRepository.save(author));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "authors", key = "#id")
    public void delete(Long id) {
        Author author = getAuthorOrThrow(id);
        if (author.getBooks() != null && !author.getBooks().isEmpty()) {
            throw new BusinessException("Không thể xóa tác giả vì vẫn còn sách liên kết với tác giả này");
        }
        authorRepository.delete(author);
    }

    private Author getAuthorOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("Id tác giả không được để trống");
        }
        return authorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Author", id));
    }

    private void validateAvatar(MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new FileUploadException("Ảnh tác giả không được để trống");
        }
        if (avatar.getSize() > MAX_AVATAR_SIZE) {
            throw new FileUploadException("Ảnh tác giả không được vượt quá 5MB");
        }
        String contentType = avatar.getContentType() == null ? "" : avatar.getContentType().trim().toLowerCase(Locale.ROOT);
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
            if (trimmed.startsWith("v") && trimmed.length() > 1 && Character.isDigit(trimmed.charAt(1))) {
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
