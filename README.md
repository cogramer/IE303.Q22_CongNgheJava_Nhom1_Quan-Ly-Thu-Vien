# IE303.Q22_CongNgheJava_Nhom1_Quan-Ly-Thu-Vien

Repo đồ án môn học Công Nghệ Java IE303.Q22 của nhóm 1. GVHD: Nguyễn Thành Luân

## Hệ thống quản lý thư viện tích hợp Collaborative Filtering

Hệ thống Quản lý Thư viện trực tuyến được xây dựng bằng Java Spring Boot, cho phép thủ thư quản lý sách, độc giả và quá trình mượn/trả sách thông qua giao diện web. Hệ thống tích hợp thuật toán **Collaborative Filtering** để gợi ý sách phù hợp với từng độc giả dựa trên hành vi mượn sách của cộng đồng.

---

## Chức năng chính

### Quản lý người dùng

- Đăng ký, đăng nhập, đăng xuất
- Phân quyền: Admin / Thủ thư / Độc giả
- Quản lý hồ sơ cá nhân

### Quản lý sách

- Thêm, sửa, xóa sách
- Quản lý danh mục, tác giả
- Tìm kiếm và lọc sách theo nhiều tiêu chí
- Xem chi tiết sách, trạng thái còn/hết

### Quản lý mượn/trả

- Tạo phiếu mượn sách
- Xác nhận trả sách
- Cảnh báo sách quá hạn
- Lịch sử mượn/trả của từng độc giả

### Thống kê & Báo cáo

- Sách được mượn nhiều nhất
- Độc giả tích cực nhất
- Thống kê theo tháng/năm
- Xuất báo cáo PDF/Excel

### Gợi ý sách thông minh (AI)

Hệ thống sử dụng thuật toán **Collaborative Filtering** để gợi ý sách:

- Phân tích hành vi mượn sách của tất cả độc giả
- Tìm những độc giả có sở thích tương tự (dùng độ đo **Cosine Similarity**)
- Gợi ý sách mà độc giả tương tự đã mượn nhưng người dùng hiện tại chưa đọc
- Kết quả gợi ý được cá nhân hóa cho từng tài khoản

---

## Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 3.x |
| Frontend | Thymeleaf, Bootstrap 5 |
| Database | MySQL 8 |
| Bảo mật | Spring Security |
| ORM | Spring Data JPA / Hibernate |
| Build tool | Maven |
| Version control | Git / GitHub |

---

## Cấu trúc thư mục

```ini
src
├───main
│   ├───java
│   │   └───com
│   │       └───library
│   │           │   LibraryManagementApplication.java
│   │           │
│   │           ├───ai
│   │           │       CollaborativeFilter.java
│   │           │       SimilarityUtil.java
│   │           │
│   │           ├───config
│   │           │       SecurityConfig.java
│   │           │
│   │           ├───controller
│   │           │   ├───api
│   │           │   │       AuthController.java
│   │           │   │       BookApiController.java
│   │           │   │       BorrowRecordApiController.java
│   │           │   │       CategoryApiController.java
│   │           │   │       FeedbackApiController.java
│   │           │   │       RecommendApiController.java
│   │           │   │       ReservationApiController.java
│   │           │   │       UserApiController.java
│   │           │   │
│   │           │   └───web
│   │           │           GlobalWebAdvice.java
│   │           │           WebAuthController.java
│   │           │           WebLibrarianController.java
│   │           │           WebReaderController.java
│   │           │           WebUserController.java
│   │           │
│   │           ├───dto
│   │           │       AuthDTO.java
│   │           │       BookDTO.java
│   │           │       BorrowRecordDTO.java
│   │           │       CategoryDTO.java
│   │           │       FeedbackDTO.java
│   │           │       ReservationDTO.java
│   │           │       UserDTO.java
│   │           │
│   │           ├───enums
│   │           │       LoginResult.java
│   │           │       RegisterResult.java
│   │           │       Result.java
│   │           │
│   │           ├───mapper
│   │           │       BookMapper.java
│   │           │       BorrowRecordMapper.java
│   │           │       CategoryMapper.java
│   │           │       ReservationMapper.java
│   │           │       UserMapper.java
│   │           │
│   │           ├───model
│   │           │       Book.java
│   │           │       BorrowRecord.java
│   │           │       Category.java
│   │           │       Feedback.java
│   │           │       RememberMeToken.java
│   │           │       Reservation.java
│   │           │       User.java
│   │           │
│   │           ├───repository
│   │           │       BookRepository.java
│   │           │       BorrowRecordRepository.java
│   │           │       CategoryRepository.java
│   │           │       FeedbackRepository.java
│   │           │       RememberMeRepository.java
│   │           │       ReservationRepository.java
│   │           │       UserRepository.java
│   │           │
│   │           ├───scheduler
│   │           │       BorrowRecordScheduler.java
│   │           │       BorrowReminderScheduler.java
│   │           │
│   │           ├───security
│   │           │       CustomUserDetailsService.java
│   │           │       JwtFilter.java
│   │           │       JwtUtil.java
│   │           │
│   │           └───service
│   │                   AuthService.java
│   │                   BookService.java
│   │                   BorrowRecordService.java
│   │                   CategoryService.java
│   │                   EmailNotificationService.java
│   │                   FeedbackService.java
│   │                   RecommendService.java
│   │                   RememberMeService.java
│   │                   ReservationService.java
│   │                   UserService.java
│   │
│   └───resources
│       │   application.properties
│       │
│       ├───static
│       │   ├───css
│       │   │       books.css
│       │   │       borrow.css
│       │   │       history.css
│       │   │       librarian-books.css
│       │   │       librarian-dashboard.css
│       │   │       librarian-loans.css
│       │   │       librarian-reports.css
│       │   │       librarian-reservations.css
│       │   │       librarian-users.css
│       │   │       library.css
│       │   │       style.css
│       │   │
│       │   ├───img
│       │   └───js
│       │           books.js
│       │           borrow.js
│       │           history.js
│       │           librarian-books.js
│       │           librarian-dashboard.js
│       │           librarian-loans.js
│       │           librarian-reports.js
│       │           librarian-reservations.js
│       │           librarian-users.js
│       │           library.js
│       │           recommendations.js
│       │
│       └───templates
│           │   forgotPassword.html
│           │   home.html
│           │   login.html
│           │   register.html
│           │
│           ├───fragments
│           │       footer.html
│           │       header.html
│           │       librarian-sidebar.html
│           │
│           ├───librarian
│           │       books.html
│           │       dashboard.html
│           │       loans.html
│           │       profile.html
│           │       reports.html
│           │       reservations.html
│           │       users.html
│           │
│           ├───reader
│           │       book-detail.html
│           │       books.html
│           │       borrow.html
│           │       history.html
│           │       recommendations.html
│           │
│           └───user
│                   profile.html
│
└───test
    └───java
        └───com
            └───library
                └───library_management
                        LibraryManagementApplicationTests.java
```

---

## Cài đặt & Chạy project

### Yêu cầu hệ thống

- Java JDK 21+
- MySQL 8+
- Maven 3.8+
- VSCode + Extension Pack for Java + Spring Boot Extension Pack

### Các bước cài đặt

**1. Clone repository**

```bash
git clone https://github.com/cogramer/IE303.Q22_CongNgheJava_Nhom1_Quan-Ly-Thu-Vien.git
cd library-management
```

**2. Tạo database**

```sql
CREATE DATABASE library_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**3. Cấu hình kết nối database**

Mở file `src/main/resources/application.properties` và chỉnh lại:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/library_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
server.port=8181
```

**4. Chạy project**

```bash
./mvnw spring-boot:run
```

Hoặc mở file `LibraryManagementApplication.java` trong VSCode và bấm **Run**.

**5. Truy cập ứng dụng**

```sh
http://localhost:8181
```

---

## Dependencies (pom.xml)

```xml
<!-- Spring Web + MVC -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Thymeleaf (template engine) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- Spring Security (phân quyền) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- MySQL Driver -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
```

---

## Phân công nhóm

| Thành viên | Vai trò |
|---|---|
| Thành viên 1 | Nhóm trưởng, tích hợp AI module |
| Thành viên 2 | Backend — BookService, BorrowService |
| Thành viên 3 | Backend — UserService, Database |
| Thành viên 4 | Frontend — Giao diện sách, mượn/trả |
| Thành viên 5 | Frontend — Giao diện người dùng, thống kê |
| Thành viên 6 | Testing + Báo cáo |

---

## Kế hoạch thực hiện

| Tuần | Nội dung |
|---|---|
| 1–2 | Phân tích yêu cầu, thiết kế database (ERD) |
| 3–4 | Lập trình các chức năng chính (CRUD) |
| 5 | Tích hợp Spring Security + AI module |
| 6 | Hoàn thiện giao diện, test |
| 7 | Viết báo cáo, chuẩn bị thuyết trình |
