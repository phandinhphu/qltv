# Copilot Instructions — Spring Boot 3 Full-Stack Project
# Language: java
# Framework: spring-boot 3.x
# Build Tool: maven
# File Pattern: **/*.java, **/pom.xml, **/resources/**

---

## 1. Project Overview

Đây là ứng dụng Full-Stack xây dựng bằng Spring Boot 3.x, bao gồm:
- **REST API** cho các client bên ngoài (mobile, frontend SPA)
- **Web MVC (Thymeleaf)** cho giao diện quản trị nội bộ

Hai luồng này tách biệt hoàn toàn ở tầng Controller, dùng chung Service và Repository.

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Build | Maven |
| Security | Spring Security 6 + JWT |
| Persistence | Spring Data JPA + Hibernate 6 |
| Database | MySQL |
| Migration | Liquibase |
| Mapping | MapStruct |
| View | Thymeleaf |
| File Storage | Cloudinary |

---

## 3. Folder Structure

```
src/main/java/com/example/app/
├── config/                  # Cấu hình Spring (Security, Beans, CORS, Cloudinary,...)
├── controller/
│   ├── api/                 # REST API controllers (@RestController)
│   └── web/                 # Thymeleaf web controllers (@Controller)
├── dto/
│   ├── request/             # DTO nhận dữ liệu đầu vào + FilterRequest
│   └── response/            # DTO trả về cho client + PageResponse
├── entity/                  # JPA Entity classes
├── exception/
│   ├── common/              # Exceptions dùng chung (ResourceNotFoundException, BusinessException,...)
│   └── handler/             # ApiExceptionHandler (JSON) + WebExceptionHandler (HTML)
├── mapper/                  # MapStruct mapper interfaces
├── repository/
│   ├── spec/                # JPA Specification classes (filter động)
│   └── (repository interfaces)
├── security/                # JWT filter, UserDetails, SecurityConfig
├── service/
│   ├── impl/                # Implement của service
│   └── (interface)          # Interface service
└── util/                    # Utility classes (nếu cần)

src/main/resources/
├── db/changelog/            # Liquibase migration files
├── templates/               # Thymeleaf HTML templates
├── static/                  # CSS, JS, images
└── application.yml          # Cấu hình ứng dụng
```

---

## 4. Naming Conventions

### Java
- **Class**: PascalCase — `ProductService`, `UserController`
- **Method / Variable**: camelCase — `findById`, `totalPrice`
- **Constant**: UPPER_SNAKE_CASE — `MAX_PAGE_SIZE`
- **Package**: lowercase — `com.example.app.service`

### DTO
- Request DTO: `[Entity]Request` — `CreateProductRequest`, `UpdateUserRequest`
- Response DTO: `[Entity]Response` — `ProductResponse`, `UserResponse`
- Filter DTO: `[Entity]FilterRequest` — `ProductFilterRequest`
- Paginated response: dùng `PageResponse<T>` wrapper dùng chung toàn project
- Đặt đúng package: `dto/request/` và `dto/response/`

### Controller
- API controller: `[Entity]ApiController` — `ProductApiController`
- Web controller: `[Entity]WebController` — `ProductWebController`
- API mapping prefix: `/api/v1/[entity]`
- Web mapping prefix: `/admin/[entity]` hoặc `/[entity]`

### Specification
- Tên class: `[Entity]Specification` — `ProductSpecification`
- Đặt trong `repository/spec/`

### Liquibase
- File changelog: `YYYYMMDD_HHMMSS_mo_ta_ngan.xml`
- Ví dụ: `20240601_000001_create_table_users.xml`

---

## 5. Coding Conventions

### General
- Tên biến, method, class: **tiếng Anh**
- Comment giải thích logic: **tiếng Việt**
- Không dùng `var` trừ khi kiểu quá dài và rõ ràng
- Không dùng `@Autowired` trên field — dùng **constructor injection**
- Không để business logic trong Controller
- Không dùng raw type

### Entity
- Luôn dùng `jakarta.persistence.*` (không phải `javax.*`)
- Mọi entity đều kế thừa `BaseEntity` (có `createdAt`, `updatedAt`, `createdBy`, `updatedBy`)
- Dùng `@Column(nullable = false)` rõ ràng
- Không dùng `@Data` của Lombok trên Entity — dùng `@Getter @Setter` riêng để tránh lỗi Hibernate
- Field lưu ảnh Cloudinary đặt tên là `imageUrl`, kiểu `String`

```java
@Getter
@Setter
@Entity
@Table(name = "products")
public class Product extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    // Lưu URL ảnh trả về từ Cloudinary
    @Column(name = "image_url")
    private String imageUrl;
}
```

### Service
- Interface nằm ở `service/`, implementation nằm ở `service/impl/`
- Đánh `@Transactional` ở method trong ServiceImpl khi cần, không đánh trên toàn class
- Ném exception kiểu `ResourceNotFoundException` hoặc `BusinessException` thay vì return null

```java
// Đúng — ném exception thay vì return null
public ProductResponse findById(Long id) {
    // Tìm sản phẩm theo id, ném lỗi nếu không tồn tại
    return productRepository.findById(id)
        .map(productMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Product", id));
}
```

### Controller — API
- Luôn trả về `ResponseEntity<ApiResponse<T>>`
- Dùng `@Valid` để validate DTO đầu vào
- Không xử lý exception trong controller — để ApiExceptionHandler lo
- Endpoint có upload file dùng `consumes = MediaType.MULTIPART_FORM_DATA_VALUE`

```java
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductApiController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getList(
            @ModelAttribute ProductFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(productService.findAll(filter, page, size)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @ModelAttribute CreateProductRequest request,
            @RequestParam(required = false) MultipartFile image) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(productService.create(request, image)));
    }
}
```

### Controller — Web (Thymeleaf)
- Return tên template string
- Dùng `Model` để truyền dữ liệu sang view
- Redirect sau POST để tránh resubmit
- Truyền `PageResponse` và `FilterRequest` vào model để Thymeleaf render pagination + giữ trạng thái filter

```java
@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class ProductWebController {

    private final ProductService productService;

    @GetMapping
    public String index(@ModelAttribute ProductFilterRequest filter,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        // Lấy danh sách có phân trang và filter, giữ lại filter để hiển thị trên form
        model.addAttribute("pageData", productService.findAll(filter, page, 10));
        model.addAttribute("filter", filter);
        return "admin/product/index";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute CreateProductRequest request,
                         @RequestParam(required = false) MultipartFile image,
                         RedirectAttributes redirectAttributes) {
        productService.create(request, image);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo thành công");
        return "redirect:/admin/products";
    }
}
```

---

## 6. Security — Spring Security 6 + JWT

- Dùng `SecurityFilterChain` bean (không extend `WebSecurityConfigurerAdapter`)
- `antMatchers()` → `requestMatchers()`
- `authorizeRequests()` → `authorizeHttpRequests()`
- JWT filter kế thừa `OncePerRequestFilter`
- API endpoints: stateless, xác thực bằng JWT header `Authorization: Bearer <token>`
- Web endpoints: stateful (session), xác thực bằng form login

```java
@Bean
@Order(1)
public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/api/**")
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

### Phân quyền
- Roles: `ROLE_ADMIN`, `ROLE_USER`
- Dùng `@PreAuthorize("hasRole('ADMIN')")` ở method service hoặc controller
- Luôn bật `@EnableMethodSecurity` trong config

---

## 7. DTO & MapStruct

- Mọi dữ liệu vào/ra đều qua DTO, không expose Entity trực tiếp
- MapStruct mapper đặt trong package `mapper/`
- Interface mapper đánh `@Mapper(componentModel = "spring")`
- Thêm method `updateEntity` để dùng trong update (tránh tạo object mới)

```java
@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductResponse toResponse(Product product);
    Product toEntity(CreateProductRequest request);

    // Dùng khi update — map vào entity đã tồn tại, không tạo mới
    @MappingTarget
    void updateEntity(UpdateProductRequest request, @MappingTarget Product product);
}
```

---

## 8. Exception Handling

### Nguyên tắc thiết kế — QUAN TRỌNG

Project có **2 Global Exception Handler riêng biệt**, KHÔNG dùng chung:

| | `ApiExceptionHandler` | `WebExceptionHandler` |
|---|---|---|
| Annotation | `@RestControllerAdvice` | `@ControllerAdvice` |
| Áp dụng cho | `controller.api.*` | `controller.web.*` |
| Trả về | `ResponseEntity<ApiResponse<T>>` (JSON) | Tên template Thymeleaf (HTML) |
| Scope | `basePackages = "...controller.api"` | `basePackages = "...controller.web"` |

> ⚠️ **Copilot lưu ý**: KHÔNG tạo một `@RestControllerAdvice` dùng chung cho cả API lẫn Web. Luôn tạo 2 class riêng biệt và chỉ định `basePackages` đúng.

### Cấu trúc package exception

```
exception/
├── common/
│   ├── ResourceNotFoundException   # 404
│   ├── BusinessException           # 400 — lỗi nghiệp vụ
│   ├── ForbiddenException          # 403
│   └── FileUploadException         # lỗi upload Cloudinary
└── handler/
    ├── ApiExceptionHandler         # JSON — controller.api.*
    └── WebExceptionHandler         # HTML — controller.web.*
```

### ApiExceptionHandler

```java
@RestControllerAdvice(basePackages = "com.example.app.controller.api")
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileUpload(FileUploadException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {
        // Gom tất cả lỗi field thành map
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (existing, replacement) -> existing
            ));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiResponse.error("Dữ liệu không hợp lệ", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Lỗi hệ thống, vui lòng thử lại sau"));
    }
}
```

### WebExceptionHandler

```java
@ControllerAdvice(basePackages = "com.example.app.controller.web")
public class WebExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", 404);
        return "error/404";
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, RedirectAttributes redirectAttributes) {
        // Flash attribute để hiển thị sau redirect
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/error";
    }

    @ExceptionHandler(FileUploadException.class)
    public String handleFileUpload(FileUploadException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        model.addAttribute("errorMessage", "Lỗi hệ thống, vui lòng thử lại sau");
        model.addAttribute("errorCode", 500);
        return "error/500";
    }
}
```

---

## 9. Audit (BaseEntity)

```java
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;
}
```

Luôn bật `@EnableJpaAuditing` trong `@SpringBootApplication` hoặc config class.

---

## 10. Liquibase

- Mỗi thay đổi schema là một file XML riêng trong `db/changelog/`
- File master: `db/changelog/db.changelog-master.xml` include từng file con
- Mỗi changeset có `id` unique và `author`
- Không sửa changeset đã chạy — tạo changeset mới để alter

```xml
<!-- 20240601_000001_create_table_products.xml -->
<changeSet id="20240601_000001" author="dev">
    <createTable tableName="products">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="name" type="VARCHAR(255)">
            <constraints nullable="false"/>
        </column>
        <column name="image_url" type="VARCHAR(500)"/>
        <column name="created_at" type="DATETIME"/>
        <column name="updated_at" type="DATETIME"/>
        <column name="created_by" type="VARCHAR(100)"/>
        <column name="updated_by" type="VARCHAR(100)"/>
    </createTable>
</changeSet>
```

---

## 11. Pagination, Filter & Search (JPA Specification)

> ⚠️ **Copilot lưu ý**: Project dùng **JPA Specification** để filter động. KHÔNG dùng JPQL @Query với điều kiện if/else. KHÔNG tạo nhiều method repository cho từng tổ hợp filter.

### PageResponse — wrapper dùng chung toàn project

```java
@Getter
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
            .content(page.getContent())
            .currentPage(page.getNumber())
            .totalPages(page.getTotalPages())
            .totalElements(page.getTotalElements())
            .pageSize(page.getSize())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
    }
}
```

### Filter Request DTO

```java
// Đặt trong dto/request/ — tất cả field nullable (null = không filter)
@Getter
@Setter
public class ProductFilterRequest {
    private String name;           // tìm kiếm theo tên (LIKE, không phân biệt hoa thường)
    private String status;         // lọc theo trạng thái (EQUAL)
    private LocalDate fromDate;    // lọc từ ngày tạo
    private LocalDate toDate;      // lọc đến ngày tạo
}
```

### Specification class

```java
// Đặt trong repository/spec/
public class ProductSpecification {

    // Tổng hợp tất cả điều kiện filter
    public static Specification<Product> of(ProductFilterRequest filter) {
        return Specification
            .where(hasName(filter.getName()))
            .and(hasStatus(filter.getStatus()))
            .and(fromDate(filter.getFromDate()))
            .and(toDate(filter.getToDate()));
    }

    // Tìm kiếm tên không phân biệt hoa thường
    private static Specification<Product> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) return null;
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    // Lọc theo trạng thái chính xác
    private static Specification<Product> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    // Lọc từ ngày (bao gồm ngày đó)
    private static Specification<Product> fromDate(LocalDate fromDate) {
        return (root, query, cb) -> {
            if (fromDate == null) return null;
            return cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay());
        };
    }

    // Lọc đến ngày (bao gồm ngày đó)
    private static Specification<Product> toDate(LocalDate toDate) {
        return (root, query, cb) -> {
            if (toDate == null) return null;
            return cb.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay());
        };
    }
}
```

### Repository

```java
// Extend thêm JpaSpecificationExecutor
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {
}
```

### Service

```java
public PageResponse<ProductResponse> findAll(ProductFilterRequest filter, int page, int size) {
    // Sắp xếp mặc định theo ngày tạo mới nhất
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Specification<Product> spec = ProductSpecification.of(filter);
    Page<ProductResponse> result = productRepository.findAll(spec, pageable)
        .map(productMapper::toResponse);
    return PageResponse.of(result);
}
```

---

## 12. File Upload — Cloudinary

> ⚠️ **Copilot lưu ý**: Project dùng **Cloudinary** để lưu file/ảnh. KHÔNG lưu file vào local filesystem. KHÔNG lưu byte[] vào database. Luôn upload lên Cloudinary và chỉ lưu URL trả về vào cột `image_url`.

### Cấu hình

```yaml
# application.yml
cloudinary:
  cloud-name: ${CLOUDINARY_CLOUD_NAME}
  api-key: ${CLOUDINARY_API_KEY}
  api-secret: ${CLOUDINARY_API_SECRET}
```

```java
// config/CloudinaryConfig.java
@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;
    @Value("${cloudinary.api-key}")
    private String apiKey;
    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true
        ));
    }
}
```

### CloudinaryService

```java
// service/CloudinaryService.java
public interface CloudinaryService {
    String uploadImage(MultipartFile file, String folder);
    void deleteImage(String publicId);
}

// service/impl/CloudinaryServiceImpl.java
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_TYPES =
        List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        validateFile(file);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("folder", folder, "resource_type", "image")
            );
            // Trả về URL bảo mật của ảnh trên Cloudinary
            return result.get("secure_url").toString();
        } catch (IOException e) {
            throw new FileUploadException("Không thể upload ảnh: " + e.getMessage());
        }
    }

    @Override
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new FileUploadException("Không thể xóa ảnh: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new FileUploadException("File không được để trống");
        if (!ALLOWED_TYPES.contains(file.getContentType()))
            throw new FileUploadException("Chỉ chấp nhận file JPG, PNG, WEBP");
        if (file.getSize() > MAX_SIZE)
            throw new FileUploadException("File không được vượt quá 5MB");
    }
}
```

### Dùng trong Service nghiệp vụ

```java
@Transactional
public ProductResponse create(CreateProductRequest request, MultipartFile image) {
    Product product = productMapper.toEntity(request);
    // Upload ảnh lên Cloudinary nếu có, lưu URL vào entity
    if (image != null && !image.isEmpty()) {
        product.setImageUrl(cloudinaryService.uploadImage(image, "products"));
    }
    return productMapper.toResponse(productRepository.save(product));
}
```

### Quy tắc folder trên Cloudinary

| Loại file | Folder |
|---|---|
| Ảnh sản phẩm | `products` |
| Ảnh người dùng / avatar | `avatars` |
| Ảnh bài viết / banner | `banners` |

---

## 13. API Response Format

```json
{ "success": true, "message": "Thành công", "data": {} }
```

```java
@Getter
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().success(true).message("Thành công").data(data).build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder().success(false).message(message).build();
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder().success(false).message(message).data(data).build();
    }
}
```

---

## 14. Copilot Prompt Templates

### Tạo CRUD đầy đủ cho Entity mới
```
Tạo đầy đủ CRUD cho entity [TênEntity] bao gồm:
1. Entity kế thừa BaseEntity, dùng jakarta.persistence
2. CreateRequest, UpdateRequest, Response DTO
3. FilterRequest DTO với các field: [liệt kê field cần filter]
4. MapStruct mapper (có method updateEntity với @MappingTarget)
5. Repository extend JpaRepository + JpaSpecificationExecutor
6. [Entity]Specification trong repository/spec/ dùng JPA Specification
7. Service interface + ServiceImpl
   - findAll(FilterRequest, page, size) trả về PageResponse
   - findById, create(request, image), update(id, request, image), delete
8. REST API Controller tại /api/v1/[entity-name]
9. Web Controller tại /admin/[entity-name] dùng Thymeleaf
Theo đúng convention trong copilot-instructions.md
```

### Thêm upload ảnh cho Entity
```
Thêm tính năng upload ảnh Cloudinary cho entity [TênEntity]:
- Thêm field imageUrl vào Entity
- Thêm cột image_url vào Liquibase changeset mới
- Inject CloudinaryService vào [Entity]ServiceImpl
- Cập nhật create() và update() nhận thêm MultipartFile image
- Upload vào Cloudinary folder "[entity-name-lowercase]"
- API Controller: thêm @RequestParam MultipartFile image, consumes = MULTIPART_FORM_DATA
- Web Controller: nhận MultipartFile từ form Thymeleaf
Theo đúng convention trong copilot-instructions.md
```

### Tạo Liquibase changeset
```
Tạo Liquibase changeset XML cho bảng [tên_bảng] với các cột:
[liệt kê cột và kiểu dữ liệu]
Thêm các cột audit: created_at, updated_at, created_by, updated_by
Đặt tên file: YYYYMMDD_HHMMSS_create_table_[tên].xml
```

### Tạo JWT Security Config
```
Tạo cấu hình Spring Security 6:
- /api/** dùng JWT stateless (SecurityFilterChain @Order(1))
- /admin/** dùng form login stateful (SecurityFilterChain @Order(2))
- Role: ADMIN, USER
- Bật @EnableMethodSecurity
Theo đúng convention trong copilot-instructions.md
```

### Thêm phân quyền
```
Thêm @PreAuthorize cho [Controller/Service/method]:
- Rule: [mô tả quyền]
Dùng Spring Security 6, @EnableMethodSecurity đã bật.
```

### Tạo exception mới
```
Tạo custom exception [TênException] trong exception/common/:
- Extend RuntimeException
- Thêm @ExceptionHandler vào ApiExceptionHandler (JSON, basePackages = "...controller.api")
- Thêm @ExceptionHandler vào WebExceptionHandler (Thymeleaf, basePackages = "...controller.web")
- KHÔNG tạo GlobalExceptionHandler dùng chung
Theo đúng convention trong copilot-instructions.md
```

---
# End of Instructions
