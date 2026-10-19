# LearnHub Languages — Spring Boot

Nền tảng học **tiếng Anh và tiếng Trung** viết bằng **Java 21, Spring Boot 3.5.16, Spring Security, Spring Data JPA, Thymeleaf và Flyway**. Giao diện tiếng Việt có sidebar, responsive, giữ các nhóm chức năng của bản trước.

**Database hiện tại: MySQL 8.4.** Làm theo [MYSQL_SETUP.md](docs/MYSQL_SETUP.md) trước khi chạy. Phải đổi DB_URL/DB_USER/DB_PASSWORD trong `.env` cũ; dữ liệu H2 không tự chuyển sang MySQL.

**Cập nhật sửa 7 lỗi còn lại — 19/10/2026:** sửa xác nhận/hủy đơn đồng thời, link payOS hết hiệu lực, trò chơi mất học thử, lựa chọn cũ trên 2.000 mục, vô hiệu hóa reset token đồng thời, hàng đợi tự lưu đáp án và xử lý đơn payOS cũ khi đổi sang manual. Đã chạy `mvn verify`: 65 kiểm thử Java đạt; các kiểm thử JavaScript đều đạt. Xem [chi tiết sửa lỗi](docs/SEVEN_LOGIC_FIXES.md) và [kết quả kiểm chứng](docs/VERIFICATION.md).

**Bản sửa logic trước đó — 19/10/2026:** đã sửa 10 lỗi về tài liệu, hoàn tiền, admin cuối cùng, xuất bản quiz, chấm bài hết giờ, đối soát thanh toán, cập nhật hồ sơ, đồng hồ quiz và flashcard. Giữ nguyên hành vi lưu đáp án khi mở cùng quiz ở hai tab theo yêu cầu. Đã chạy thành công 53 kiểm thử Java trên H2, kiểm tra phát âm và bộ đếm giờ JavaScript; `mvn verify` tạo JAR thành công. Chi tiết và giới hạn kiểm chứng: `docs/VERIFICATION.md`; thay đổi mới nhất: `docs/SEVEN_LOGIC_FIXES.md`.

Hướng dẫn sửa H2 trong `docs/H2_STARTUP_FIX.md` và `docs/FAILED_V1_RECOVERY.md` chỉ áp dụng cho bản cũ. Khi chuyển sang MySQL, giữ file H2 để sao lưu; không cần xóa nó.

Nếu log báo `Detected failed migration to version 1`, xem [hướng dẫn khôi phục](docs/FAILED_V1_RECOVERY.md). Gói ZIP hiện tại không kèm database hay tài liệu tải lên; các bản đầy đủ trước đây vô tình có kèm database lỗi của lần chạy thử.

Bản mới có lựa chọn Tiếng Anh / Tiếng Trung, bộ lọc nội dung và giọng đọc theo ngôn ngữ. Xem `docs/TWO_LANGUAGES.md` để cập nhật; migration V2 chạy tự động trong database đang dùng, không cần xóa database.

Hướng dẫn dùng video, tài liệu và cập nhật an toàn: [VIDEO_AND_DOCUMENTS.md](docs/VIDEO_AND_DOCUMENTS.md). Bộ migration trong `db/mysql` dành riêng MySQL; nội dung H2 cũ không được nhập tự động. Khi cập nhật phải chép cả `pom.xml` để Maven nhận Lombok.

## 1. Chạy nhanh trên Windows

### Bước 1 — Chuẩn bị Java

Cài **JDK 21**, không chỉ JRE. Mở terminal và kiểm tra:

```powershell
java -version
```

Nếu dùng IntelliJ IDEA, mở `pom.xml` dưới dạng Maven project và chọn Project SDK 21 / Maven Runner JDK 21.

### Bước 2 — Tạo MySQL và cấu hình

Tạo database/người dùng `learnhub` bằng MySQL Workbench theo `docs/MYSQL_SETUP.md`. Flyway sẽ tạo các bảng sau khi kết nối thành công.

Giải nén rồi mở PowerShell tại thư mục **`D:\Learnhub`**, nơi có `pom.xml`:

```powershell
Copy-Item .env.example .env
notepad .env
```

Điền email và mật khẩu quản trị **của bạn**:

```properties
ADMIN_EMAIL=email-cua-ban@example.com
ADMIN_PASSWORD=tu-dat-mat-khau-it-nhat-8-ky-tu
```

Không dùng nguyên mật khẩu ví dụ trên. Mật khẩu tối đa 72 byte UTF-8; nên dùng mật khẩu ASCII ngẫu nhiên dài. Điền `DB_URL`, `DB_USER`, `DB_PASSWORD` tương ứng tài khoản MySQL; không dùng mật khẩu website làm mặc định cho database. Giá trị `.env` dùng cú pháp Java `.properties`: không thêm dấu nháy bao quanh và không có dòng `export`.

Giữ `APP_PROFILE=dev`, `MAIL_MODE=console`, `SEED_DEMO=true` để thử dữ liệu mẫu. Chưa cần payOS hay SMTP thật.

### Bước 3 — Khởi động

```powershell
.\mvnw.cmd spring-boot:run
```

Script dùng Maven đã cài nếu có. Nếu chưa có, script tải Apache Maven 3.9.11 từ Maven Central, kiểm tra SHA-512 và lưu trong `.tools/`. Đây là launcher bootstrap của project; không yêu cầu cài Maven hệ thống. Lần đầu cần Internet để tải Maven và dependencies, có thể mất vài phút. Script PowerShell chỉ đặt ExecutionPolicy cho tiến trình được gọi, không đổi cấu hình hệ thống.

Nếu đã có Maven:

```powershell
mvn spring-boot:run
```

**Website:** http://localhost:8080/

**Quản trị:** http://localhost:8080/admin

Các bảng được Flyway tạo tự động khi khởi động. Tài khoản admin chỉ được tạo nếu email cấu hình chưa tồn tại; thay `.env` không tự đổi mật khẩu tài khoản đã có. Sau lần tạo thành công, xóa giá trị `ADMIN_PASSWORD` khỏi `.env` rồi dùng chức năng đặt lại mật khẩu khi cần.

Để dừng, nhấn **Ctrl+C**. Những lần sau chỉ cần chạy lại lệnh khởi động.

### Thử tài khoản học sinh

Đăng ký trên web → ứng dụng xếp email xác minh vào hàng đợi → trong khoảng 10 giây, email/link xuất hiện ở terminal vì đang dùng `MAIL_MODE=console`. Mở link, nhấn xác nhận rồi đăng nhập. Console mode không gửi email thật.

**Không mở terminal worker riêng.** Tác vụ nền chạy bằng Spring Scheduling trong cùng ứng dụng.

## 2. Linux/macOS

JDK 21 và curl/tar cần có sẵn. Sau khi tạo `.env` và điền thông tin:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Launcher tự tải Maven khi máy chưa cài; yêu cầu `sha512sum` hoặc `shasum`.

## 3. Docker + MySQL

Cài Docker Engine/Desktop + Compose v2. Tạo `.env`, điền `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, rồi chạy:

```bash
docker compose up --build -d
docker compose logs -f app
```

- Mở http://localhost:8080/.
- Docker chạy MySQL 8.4 riêng và tự tạo database ở lần đầu; chạy Java trực tiếp cũng dùng MySQL theo `.env`.
- Docker build chạy Maven `verify` trước khi đóng gói.
- Không cần cài JDK/Maven trên máy host khi dùng Docker.
- Dừng: `docker compose down`. Không dùng `down -v` nếu muốn giữ dữ liệu.
- Cổng 8080 chỉ bind loopback trên host; public deployment dùng Caddy theo `docs/DEPLOY_EC2.md`.

## 4. Chức năng

### Học tập

- Trang chủ dashboard, hồ sơ giáo viên, lộ trình HSK nền tảng/nâng cao.
- Khóa → chương → bài; học thử hoặc học theo quyền và thời hạn.
- Video bài giảng bằng liên kết Google Drive, OneDrive và dịch vụ chia sẻ khác; xem trong bài khi được hỗ trợ hoặc mở riêng.
- Tài liệu PDF/DOCX/XLSX/PPTX/TXT dưới 1.000.000 byte tải trực tiếp vào thư mục riêng tư; file lớn dùng liên kết cloud. Quyền học được kiểm tra trước khi mở nội dung.
- Từ vựng Anh/Trung, IPA/Pinyin, nghĩa tiếng Việt và ví dụ; Hán Việt khi học tiếng Trung.
- Bản ghi âm do giáo viên cung cấp; dự phòng bằng giọng tiếng Anh/tiếng Trung của thiết bị khi khả dụng.
- Lưu từ theo tài khoản, flashcard lật/xáo trộn, lịch ôn.
- Quiz một/nhiều đáp án, tự lưu, deadline máy chủ, giới hạn lượt, đề được lưu theo phiên bản.
- Kết quả /10 và lời giải; giữ lịch sử và gửi email kết quả.
- Chuỗi ngày, XP, bảng xếp hạng ngày/tuần/tất cả; mặc định ẩn, tự bật bằng bí danh.
- Lớp học, lịch, học phí và form tư vấn; góp ý riêng tư; sản phẩm dự kiến.

### Tài khoản và thanh toán

- Spring Security, đăng ký, xác minh email, quên/đặt lại mật khẩu.
- Mật khẩu BCrypt qua DelegatingPasswordEncoder; token xác minh/reset chỉ lưu hash SHA-256.
- Đặt lại mật khẩu hoặc đổi quyền/trạng thái làm phiên cũ hết hiệu lực khi tiếp tục sử dụng web.
- CSRF cho form/AJAX; chỉ webhook payOS được miễn CSRF và phải xác minh chữ ký.
- Đơn có giá/thời hạn snapshot; reference giao dịch duy nhất; chống cấp quyền lặp.
- Chuyển khoản thủ công và adapter payOS có chữ ký/đối soát API.
- Email outbox có retry; lịch xử lý quiz hết giờ và đối soát thanh toán.

### Trang quản trị riêng

- `/admin`: quản trị nội dung qua các màn hình Thymeleaf.
- `/admin/content/...`: giáo viên, khóa, chương, bài, HSK, từ, quiz, câu hỏi, lựa chọn, lớp, game, roadmap.
- `/admin/upload`: thêm video/tài liệu bằng liên kết hoặc tải tài liệu dưới 1 MB, chỉnh sửa và ẩn/hiện nội dung.
- `/admin/results`: bảng điểm và CSV.
- `/admin/inbox`: tư vấn và góp ý.
- `/admin/finance`: xác nhận chuyển khoản, đối soát payOS, cấp quyền và ghi nhận hoàn tiền bên ngoài.
- `/admin/users`: vai trò, trạng thái tài khoản; bảo vệ admin cuối cùng.
- `/admin/operations`: email và nhật ký.

`TEACHER` được quản lý nội dung, tài liệu, kết quả, tư vấn/góp ý của **một đơn vị vận hành**. `ADMIN` có thêm tài chính, cấp quyền, tài khoản và vận hành. Chưa phân quyền theo giáo viên sở hữu từng khóa.

## 5. Thanh toán

Mặc định `PAYMENT_MODE=manual`. Điền `BANK_NAME`, `BANK_ACCOUNT`, `BANK_OWNER`. Học sinh tạo đơn rồi chuyển đúng tiền và nội dung `LH<mã đơn>`.

Admin đối chiếu sao kê → `/admin/finance` → nhập mã giao dịch ngân hàng → xác nhận. Không xác nhận chỉ dựa trên ảnh chụp. Cấp quyền và ghi nhận thanh toán nằm trong transaction; reference không được dùng lại.

Để dùng payOS, điền:

```properties
PAYMENT_MODE=payos
PAYOS_CLIENT_ID=...
PAYOS_API_KEY=...
PAYOS_CHECKSUM_KEY=...
```

Đăng ký webhook HTTPS:

```text
https://your-domain.example/payments/payos/webhook
```

Webhook xác minh chữ ký rồi đối soát qua API nhà cung cấp. URL quay lại trình duyệt không mở khóa. Khi đổi sang `PAYMENT_MODE=manual`, giữ cấu hình payOS và webhook cho đến khi mọi đơn payOS cũ đã được xử lý; đơn mới dùng manual, đơn cũ tiếp tục dùng payOS. Nếu còn đơn payOS chờ mà thiếu cấu hình, ứng dụng thông báo lỗi lúc khởi động. Cần thử giao dịch nhỏ thật, webhook trùng/chậm và đối soát trên staging trước khi nhận tiền. Bản bàn giao chưa gọi dịch vụ payOS thật.

Hoàn tiền: website **không chuyển tiền ra**. Admin hoàn qua kênh bên ngoài trước, rồi ghi nhận mã khoản hoàn/lý do để thu hồi đúng quyền có nguồn từ đơn đó. Quyền có nguồn cấp tay/đơn khác yêu cầu rà soát riêng.

## 6. Email

```properties
MAIL_MODE=smtp
SMTP_HOST=your-smtp-host
SMTP_PORT=587
SMTP_USER=your-user
SMTP_PASSWORD=your-secret
SMTP_AUTH=true
SMTP_TLS=true
FROM_EMAIL=no-reply@your-domain.example
SUPPORT_EMAIL=support@your-domain.example
```

Xác minh SPF/DKIM/DMARC theo dịch vụ email. Mọi email giao dịch dùng outbox. Thư có thể bị gửi lặp nếu SMTP thành công ngay trước sự cố commit; kết quả/Payment không bị tạo lại. Kiểm tra job có `tries=8` và chưa gửi để xử lý. Bản này chạy **một instance ứng dụng**; rate limit đang ở bộ nhớ và session không chia sẻ giữa nhiều máy.

## 7. Build và kiểm thử

Windows:

```powershell
.\mvnw.cmd verify
.\mvnw.cmd package
java -jar target\learnhub-chinese-1.0.0.jar
```

Linux/macOS:

```bash
./mvnw verify
java -jar target/learnhub-chinese-1.0.0.jar
```

CI có hai môi trường H2 và MySQL. `CoreSelfTest` kiểm tra logic thuần Java; `PlatformIntegrationTest` kiểm tra Spring MVC/Security/JPA/Flyway và render các trang. Xem trạng thái thực tế trong `docs/VERIFICATION.md`, không coi cấu hình CI là bằng chứng đã chạy thành công.

## 8. Cấu trúc

```text
src/main/java/vn/learnhub/
  auth/          Đăng nhập, token, session, bảo mật và quyền truy cập
  user/          Tài khoản, vai trò, repository và hồ sơ
  course/        Khóa học, giáo viên, chương/bài, lộ trình và tư vấn lớp
  learning/      Quyền học, dashboard và tiến độ bài học
  vocabulary/    Từ vựng, lưu từ và flashcard/lịch ôn
  quiz/          Câu hỏi, lượt làm bài, chấm điểm và trò chơi
  payment/       Đơn hàng, thanh toán, payOS và webhook
  community/     XP, streak, thống kê, bảng xếp hạng và góp ý
  admin/         Quản trị nội dung, người dùng và giao dịch
  notification/  Hàng đợi email và gửi thư
  storage/       Video liên kết cloud và tài liệu riêng tư
  portal/        Trang chủ, chính sách, kế hoạch và health
  config/        Thuộc tính cấu hình ứng dụng
  bootstrap/     Khởi tạo quản trị viên và dữ liệu demo
  jobs/          Tác vụ nền theo lịch
  shared/        Audit, persistence, JSON, chữ ký, quy tắc học và web dùng chung
src/main/resources/
  db/migration/  Lịch sử Flyway giữ nguyên
  templates/     Chia theo chức năng, kèm auth/admin/fragments dùng chung
  static/        CSS/JS dùng chung, giữ nguyên URL
src/test/        Kiểm thử lõi và tích hợp toàn ứng dụng
deploy/          HTTPS và backup
docs/            Hướng dẫn triển khai, cấu trúc và nghiệm thu
```

Bản chia lại package dựa trên đúng `Learnhub.zip` người dùng gửi ngày 19/10/2026. Xem `docs/SRC_STRUCTURE.md` để tra file mới và `docs/APPLY_REFACTOR.md` để cập nhật project hiện có.

Dùng các quan hệ một chiều được tải sẵn và tắt Open-in-View để tránh phụ thuộc lazy loading trong template. Chưa tối ưu cho kho nội dung lớn; cần đo tải và tối ưu query/pagination theo dữ liệu thực tế.

## 9. Trước khi mở bán

- Chạy `verify`, build Docker và thử toàn bộ luồng trên staging.
- Thay nội dung mẫu, rà soát giáo trình, đáp án, quyền sử dụng tài liệu.
- Kiểm tra SMTP, ngân hàng/payOS, HTTPS, thông tin hỗ trợ.
- Hoàn thiện chính sách học, dữ liệu và hoàn tiền; trang chính sách hiện là dự thảo.
- Bảo vệ admin bằng MFA/lớp truy cập riêng; MFA chưa tích hợp sẵn.
- Cấu hình giám sát bên ngoài, backup độc lập và diễn tập restore.
- Thử đồng thời mua, webhook/đối soát, nộp bài và ôn từ trên MySQL.

Dữ liệu seed gồm phần tiếng Trung (10 từ mẫu, 2 bài tự học, 7 chủ đề game × 2 câu) và phần tiếng Anh (8 từ, 2 bài tự học, 1 chủ đề game cùng bài luyện tập mẫu). Không có học viên/điểm giả, không sao chép giáo trình hay hồ sơ cá nhân website khác. Đã có video bằng liên kết cloud; livestream, chấm tự luận/giọng nói bằng AI, giữ chỗ lớp tự động và bộ HSK đầy đủ chưa được triển khai.

**Nếu từng chạy bản Django:** đây là schema Java mới. Đọc `docs/MIGRATION_FROM_DJANGO.md`; không kết nối bản này trực tiếp vào database Django hoặc sử dụng lại volume database cũ.
