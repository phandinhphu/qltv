# Project Context — Hệ Thống Quản Lý Thư Viện Số (DRM-lite)
# Dùng kèm với copilot-instructions.md
# File này mô tả NGHIỆP VỤ của project — Copilot đọc để hiểu domain

---

## 1. Mô Tả Hệ Thống

Hệ thống quản lý thư viện sách số, cho phép:
- **ADMIN** quản lý kho sách, danh mục, tác giả, người dùng và xem thống kê
- **USER** tìm kiếm sách, mượn/trả sách số, download file trong thời hạn, đánh giá sách

**Cơ chế DRM-lite**: khi mượn sách, hệ thống cấp `downloadToken` (UUID) kèm thời hạn 7 ngày.
User chỉ download được file PDF khi token còn hợp lệ. Khi trả sách hoặc hết hạn, token bị vô hiệu hóa.

---

## 2. Domain Model — Entities & Relationships

### Entity chính

```
User ──< BorrowRecord >── Book >──< Category
                              Book >──< Author
                              Book ──< Review ── User
```

### Chi tiết từng Entity

#### User
```java
// Bảng: users
Long id
String username        // unique, không null
String email           // unique, không null
String password        // BCrypt encoded
Role role              // enum: ADMIN, USER
boolean isActive       // mặc định true
// kế thừa BaseEntity (createdAt, updatedAt, createdBy, updatedBy)
```

#### Book (Sách số)
```java
// Bảng: books
Long id
String title           // tên sách, không null
String isbn            // mã ISBN, unique
String description     // mô tả nội dung
String coverImageUrl   // URL ảnh bìa từ Cloudinary (folder: "book-covers")
String fileUrl         // URL file PDF/EPUB từ Cloudinary (folder: "book-files")
Integer publishYear    // năm xuất bản
String language        // ngôn ngữ, mặc định "Tiếng Việt"
Integer totalCopies    // tổng số bản (mặc định 5)
Integer availableCopies // số bản còn có thể mượn (tự động cập nhật)
BookStatus status      // enum: AVAILABLE, OUT_OF_STOCK, INACTIVE

// Quan hệ
@ManyToMany Set<Category> categories
@ManyToMany Set<Author> authors
@OneToMany List<BorrowRecord> borrowRecords
@OneToMany List<Review> reviews
```

#### Category (Danh mục)
```java
// Bảng: categories
Long id
String name            // tên danh mục, unique, không null
String description
String slug            // URL-friendly, unique (vd: "khoa-hoc-cong-nghe")

// Quan hệ
@ManyToMany(mappedBy = "categories") Set<Book> books
```

#### Author (Tác giả)
```java
// Bảng: authors
Long id
String name            // không null
String bio             // tiểu sử
String avatarUrl       // URL ảnh từ Cloudinary (folder: "author-avatars")

// Quan hệ
@ManyToMany(mappedBy = "authors") Set<Book> books
```

#### BorrowRecord (Lịch sử mượn) — Entity trung tâm của nghiệp vụ
```java
// Bảng: borrow_records
Long id
LocalDateTime borrowDate    // ngày mượn, tự động set khi tạo
LocalDateTime dueDate       // hạn trả = borrowDate + 7 ngày
LocalDateTime returnDate    // ngày trả thực tế (null nếu chưa trả)
BorrowStatus status         // enum: BORROWING, RETURNED, OVERDUE
String downloadToken        // UUID ngẫu nhiên, dùng để download file
LocalDateTime tokenExpiredAt // = dueDate (token hết hạn cùng lúc với hạn trả)

// Quan hệ
@ManyToOne User user
@ManyToOne Book book
```

#### Review (Đánh giá)
```java
// Bảng: reviews
Long id
Integer rating         // 1-5 sao, không null
String comment         // nội dung đánh giá
boolean isVisible      // ADMIN có thể ẩn review (mặc định true)

// Quan hệ
@ManyToOne User user
@ManyToOne Book book
// Ràng buộc: mỗi user chỉ review 1 lần / 1 sách (@UniqueConstraint)
// Ràng buộc: chỉ user đã mượn sách mới được review
```

---

## 3. Enum Definitions

```java
public enum Role { ADMIN, USER }

public enum BookStatus {
    AVAILABLE,      // còn bản cho mượn (availableCopies > 0)
    OUT_OF_STOCK,   // hết bản cho mượn (availableCopies == 0)
    INACTIVE        // ADMIN ẩn sách, không hiển thị cho USER
}

public enum BorrowStatus {
    BORROWING,  // đang mượn, token còn hợp lệ
    RETURNED,   // đã trả, token bị vô hiệu hóa
    OVERDUE     // quá hạn, chưa trả (được @Scheduled cập nhật tự động hàng ngày)
}
```

---

## 4. Business Rules — Quy Tắc Nghiệp Vụ

> ⚠️ **Copilot lưu ý**: Đây là các rule bắt buộc, PHẢI implement đúng trong Service layer.

### Quy tắc mượn sách
- User chỉ được mượn tối đa **3 sách cùng lúc** (đếm record có status = BORROWING)
- User **không được mượn** sách đang mượn dở (đã có record BORROWING của sách đó)
- User có record **OVERDUE** → **không được mượn thêm** sách mới
- Khi mượn thành công: `availableCopies--`, tạo `downloadToken` (UUID), set `dueDate = now + 7 ngày`

### Quy tắc trả sách
- Chỉ trả được record có status = BORROWING hoặc OVERDUE
- Khi trả: set `returnDate = now`, `status = RETURNED`, xóa `downloadToken` (set null), `availableCopies++`
- Tự động cập nhật `BookStatus`: nếu `availableCopies > 0` → AVAILABLE

### Quy tắc download file
- Kiểm tra `downloadToken` có tồn tại trong DB không
- Kiểm tra `tokenExpiredAt > now` (token chưa hết hạn)
- Kiểm tra `status = BORROWING` (chưa trả)
- Nếu đủ 3 điều kiện → redirect đến `fileUrl` Cloudinary
- Vi phạm bất kỳ điều kiện nào → ném `FileAccessDeniedException`

### Quy tắc đánh giá
- Chỉ user đã có BorrowRecord (bất kỳ status nào) của sách đó mới được review
- Mỗi user chỉ review 1 lần / 1 sách (unique constraint)
- Rating phải từ 1 đến 5

### Overdue detection (Scheduled Job)
- Chạy **mỗi ngày lúc 00:05** bằng `@Scheduled(cron = "0 5 0 * * *")`
- Query tất cả BorrowRecord có `status = BORROWING` và `dueDate < now`
- Cập nhật hàng loạt thành `status = OVERDUE`
- Class: `OverdueScheduler` trong package `scheduler/`

---

## 5. Package Structure (bổ sung cho copilot-instructions.md)

```
src/main/java/com/example/library/
├── config/
├── controller/
│   ├── api/
│   │   ├── BookApiController         # /api/v1/books
│   │   ├── CategoryApiController     # /api/v1/categories
│   │   ├── AuthorApiController       # /api/v1/authors
│   │   ├── BorrowApiController       # /api/v1/borrows
│   │   ├── ReviewApiController       # /api/v1/reviews
│   │   └── AuthApiController         # /api/v1/auth (login, register)
│   └── web/
│       ├── HomeWebController         # / (trang chủ USER)
│       ├── BookWebController         # /books (danh sách, chi tiết)
│       ├── BorrowWebController       # /borrows (lịch sử, mượn/trả)
│       └── admin/
│           ├── AdminBookWebController      # /admin/books
│           ├── AdminCategoryWebController  # /admin/categories
│           ├── AdminAuthorWebController    # /admin/authors
│           ├── AdminUserWebController      # /admin/users
│           └── AdminDashboardWebController # /admin/dashboard
├── dto/
│   ├── request/
│   │   ├── BookFilterRequest         # filter: title, categoryId, authorId, language, status
│   │   ├── CreateBookRequest
│   │   ├── UpdateBookRequest
│   │   ├── CreateCategoryRequest
│   │   ├── CreateAuthorRequest
│   │   ├── BorrowRequest             # chỉ cần bookId
│   │   ├── CreateReviewRequest       # rating + comment
│   │   └── RegisterRequest / LoginRequest
│   └── response/
│       ├── BookResponse              # gồm cả categoryNames, authorNames, avgRating
│       ├── BookDetailResponse        # thêm reviews, borrowCount
│       ├── CategoryResponse
│       ├── AuthorResponse
│       ├── BorrowRecordResponse      # gồm bookTitle, daysRemaining, hasValidToken
│       ├── ReviewResponse
│       ├── UserResponse
│       ├── DashboardResponse         # thống kê tổng hợp
│       └── DownloadTokenResponse     # downloadUrl (có thể dùng sau)
├── entity/
│   ├── User, Book, Category, Author
│   ├── BorrowRecord, Review
│   └── BaseEntity
├── exception/
│   ├── common/
│   │   ├── ResourceNotFoundException
│   │   ├── BusinessException
│   │   ├── FileAccessDeniedException  # download không hợp lệ
│   │   └── FileUploadException
│   └── handler/
│       ├── ApiExceptionHandler
│       └── WebExceptionHandler
├── mapper/
│   ├── BookMapper, CategoryMapper, AuthorMapper
│   ├── BorrowRecordMapper, ReviewMapper, UserMapper
├── repository/
│   ├── spec/
│   │   └── BookSpecification         # filter: title(LIKE), categoryId, authorId, language, status
│   ├── BookRepository                # + JpaSpecificationExecutor
│   ├── CategoryRepository
│   ├── AuthorRepository
│   ├── BorrowRecordRepository        # có custom query findOverdue, countActiveBorrows
│   ├── ReviewRepository
│   └── UserRepository
├── scheduler/
│   └── OverdueScheduler              # @Scheduled cập nhật OVERDUE hàng ngày
├── security/
│   ├── JwtFilter, JwtUtil
│   ├── CustomUserDetailsService
│   └── SecurityConfig
└── service/
    ├── BookService / impl/BookServiceImpl
    ├── CategoryService / impl/CategoryServiceImpl
    ├── AuthorService / impl/AuthorServiceImpl
    ├── BorrowService / impl/BorrowServiceImpl    # nghiệp vụ phức tạp nhất
    ├── ReviewService / impl/ReviewServiceImpl
    ├── UserService / impl/UserServiceImpl
    ├── CloudinaryService / impl/CloudinaryServiceImpl
    └── DashboardService / impl/DashboardServiceImpl
```

---

## 6. API Endpoints

### Public (không cần auth)
| Method | URL | Mô tả |
|---|---|---|
| POST | `/api/v1/auth/register` | Đăng ký tài khoản USER |
| POST | `/api/v1/auth/login` | Đăng nhập, nhận JWT |
| GET | `/api/v1/books` | Danh sách sách (có filter, paginate) |
| GET | `/api/v1/books/{id}` | Chi tiết sách |
| GET | `/api/v1/categories` | Danh sách danh mục |
| GET | `/api/v1/authors` | Danh sách tác giả |

### USER (cần JWT)
| Method | URL | Mô tả |
|---|---|---|
| POST | `/api/v1/borrows` | Mượn sách |
| PUT | `/api/v1/borrows/{id}/return` | Trả sách |
| GET | `/api/v1/borrows/my` | Lịch sử mượn của tôi |
| GET | `/api/v1/borrows/{id}/download` | Download file (kiểm tra token) |
| POST | `/api/v1/reviews` | Đánh giá sách |
| GET | `/api/v1/reviews/book/{bookId}` | Xem review của sách |

### ADMIN only
| Method | URL | Mô tả |
|---|---|---|
| POST/PUT/DELETE | `/api/v1/books/**` | Quản lý sách |
| POST/PUT/DELETE | `/api/v1/categories/**` | Quản lý danh mục |
| POST/PUT/DELETE | `/api/v1/authors/**` | Quản lý tác giả |
| GET | `/api/v1/borrows` | Tất cả lịch sử mượn |
| GET/PUT | `/api/v1/users/**` | Quản lý user |
| GET | `/api/v1/dashboard` | Thống kê tổng hợp |

---

## 7. Web Routes (Thymeleaf)

### Trang USER
| URL | Controller | Template |
|---|---|---|
| `/` | HomeWebController | `home/index` — trang chủ, sách nổi bật |
| `/books` | BookWebController | `book/list` — danh sách + tìm kiếm |
| `/books/{id}` | BookWebController | `book/detail` — chi tiết + review |
| `/borrows/my` | BorrowWebController | `borrow/my-list` — lịch sử cá nhân |
| `/login`, `/register` | Spring Security | `auth/login`, `auth/register` |

### Trang ADMIN (`/admin/**`)
| URL | Template |
|---|---|
| `/admin/dashboard` | `admin/dashboard` — thống kê |
| `/admin/books` | `admin/book/list` |
| `/admin/books/new`, `/admin/books/{id}/edit` | `admin/book/form` |
| `/admin/categories` | `admin/category/list` |
| `/admin/authors` | `admin/author/list` |
| `/admin/users` | `admin/user/list` |
| `/admin/borrows` | `admin/borrow/list` — tất cả lịch sử mượn |

---

## 8. Dashboard — Thống kê ADMIN

`DashboardResponse` gồm:
- `totalBooks` — tổng số sách
- `totalUsers` — tổng số user
- `totalActiveBorrows` — đang mượn
- `totalOverdue` — quá hạn
- `topBorrowedBooks` — `List<BookResponse>` top 5 sách mượn nhiều nhất
- `borrowCountByMonth` — `Map<String, Long>` số lượng mượn theo tháng (12 tháng gần nhất)

Query `borrowCountByMonth` dùng JPQL group by tháng:
```java
@Query("SELECT FUNCTION('MONTH', b.borrowDate), FUNCTION('YEAR', b.borrowDate), COUNT(b) " +
       "FROM BorrowRecord b " +
       "WHERE b.borrowDate >= :fromDate " +
       "GROUP BY FUNCTION('YEAR', b.borrowDate), FUNCTION('MONTH', b.borrowDate) " +
       "ORDER BY FUNCTION('YEAR', b.borrowDate), FUNCTION('MONTH', b.borrowDate)")
List<Object[]> countByMonth(@Param("fromDate") LocalDateTime fromDate);
```

---

## 9. Cloudinary — Folder Convention

| Loại file | Folder Cloudinary | Ghi chú |
|---|---|---|
| Ảnh bìa sách | `book-covers` | JPG/PNG/WEBP, max 5MB |
| File sách PDF | `book-files` | PDF only, max 50MB |
| Ảnh tác giả | `author-avatars` | JPG/PNG/WEBP, max 2MB |

> ⚠️ File PDF validate: `application/pdf` only. Giới hạn 50MB (khác với ảnh 5MB).

---

## 10. Spring Profiles

```yaml
# application.yml (chung)
spring:
  profiles:
    active: dev

# application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/library_dev
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate   # Liquibase quản lý schema
logging:
  level:
    com.example.library: DEBUG

# application-prod.yml
spring:
  datasource:
    url: ${DATABASE_URL}
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
logging:
  level:
    root: WARN
```

---

## 11. Liquibase Migration Order

```
db/changelog/
├── db.changelog-master.xml
├── 20240601_000001_create_table_users.xml
├── 20240601_000002_create_table_categories.xml
├── 20240601_000003_create_table_authors.xml
├── 20240601_000004_create_table_books.xml
├── 20240601_000005_create_table_book_categories.xml   ← join table Many-to-Many
├── 20240601_000006_create_table_book_authors.xml      ← join table Many-to-Many
├── 20240601_000007_create_table_borrow_records.xml
├── 20240601_000008_create_table_reviews.xml
└── 20240601_000009_insert_data_sample.xml             ← data mẫu (dev only)
```

---

## 12. Copilot Prompt Templates — Nghiệp Vụ

### Tạo BorrowService với đầy đủ business rules
```
Tạo BorrowService và BorrowServiceImpl cho hệ thống thư viện số với các method:
1. borrow(Long bookId, Long userId):
   - Kiểm tra user có record OVERDUE không → ném BusinessException
   - Kiểm tra user đang mượn < 3 sách không → ném BusinessException
   - Kiểm tra sách không có record BORROWING của user này → ném BusinessException
   - Kiểm tra book.availableCopies > 0 → ném BusinessException nếu hết
   - Tạo BorrowRecord: borrowDate=now, dueDate=now+7ngày, status=BORROWING
   - Tạo downloadToken = UUID.randomUUID().toString()
   - tokenExpiredAt = dueDate
   - Giảm book.availableCopies đi 1, cập nhật BookStatus nếu cần
   - Lưu và trả BorrowRecordResponse

2. returnBook(Long borrowId, Long userId):
   - Chỉ owner mới trả được, ném ForbiddenException nếu sai
   - Chỉ trả được status BORROWING hoặc OVERDUE
   - Set returnDate=now, status=RETURNED, downloadToken=null
   - Tăng book.availableCopies lên 1, cập nhật BookStatus
   - Lưu và trả BorrowRecordResponse

3. downloadFile(String downloadToken, Long userId):
   - Tìm BorrowRecord theo downloadToken, 404 nếu không có
   - Kiểm tra record.user.id == userId → ném ForbiddenException nếu không khớp
   - Kiểm tra status == BORROWING và tokenExpiredAt > now → ném FileAccessDeniedException
   - Trả về fileUrl của book

4. getMyBorrows(Long userId, int page, int size): trả PageResponse<BorrowRecordResponse>
5. getAllBorrows(BorrowFilterRequest filter, int page, int size): ADMIN only

Theo đúng convention trong copilot-instructions.md và project-context.md
```

### Tạo OverdueScheduler
```
Tạo class OverdueScheduler trong package scheduler/:
- @Component, @RequiredArgsConstructor
- Inject BorrowRecordRepository
- Method checkAndMarkOverdue() chạy @Scheduled(cron = "0 5 0 * * *") — mỗi ngày 00:05
- Query: tìm tất cả BorrowRecord có status=BORROWING và dueDate < LocalDateTime.now()
- Cập nhật hàng loạt thành status=OVERDUE (dùng @Modifying @Query để update bulk)
- Log số lượng record bị đánh dấu OVERDUE
Bật @EnableScheduling trong @SpringBootApplication hoặc config class
```

### Tạo BookSpecification với filter đầy đủ
```
Tạo BookSpecification trong repository/spec/ với các điều kiện filter từ BookFilterRequest:
- title: LIKE không phân biệt hoa thường
- categoryId: join Book → categories, lọc theo category.id
- authorId: join Book → authors, lọc theo author.id
- language: EQUAL
- status: EQUAL (enum BookStatus)
- publishYear: EQUAL
Tất cả điều kiện đều nullable (null = bỏ qua điều kiện đó)
Theo đúng convention JPA Specification trong copilot-instructions.md
```

### Tạo DashboardService
```
Tạo DashboardService và DashboardServiceImpl:
- Method getDashboard() trả DashboardResponse gồm:
  - totalBooks: bookRepository.count()
  - totalUsers: userRepository.countByRole(Role.USER)
  - totalActiveBorrows: borrowRecordRepository.countByStatus(BorrowStatus.BORROWING)
  - totalOverdue: borrowRecordRepository.countByStatus(BorrowStatus.OVERDUE)
  - topBorrowedBooks: top 5 sách có nhiều BorrowRecord nhất (dùng @Query GROUP BY)
  - borrowCountByMonth: số lượng mượn theo tháng trong 12 tháng gần nhất
Theo đúng convention trong copilot-instructions.md và project-context.md
```

### Tạo Review với validation nghiệp vụ
```
Tạo ReviewService với method createReview(CreateReviewRequest request, Long userId):
- Kiểm tra user đã có BorrowRecord (bất kỳ status) của bookId chưa
  → ném BusinessException "Bạn phải mượn sách trước khi đánh giá"
- Kiểm tra user chưa review sách này (reviewRepository.existsByUserIdAndBookId)
  → ném BusinessException "Bạn đã đánh giá sách này rồi"
- Validate rating trong khoảng 1-5
- Lưu Review và trả ReviewResponse
Theo đúng convention trong copilot-instructions.md và project-context.md
```

---
# End of Project Context
