# EzWallet - Kế hoạch kiểm thử (Test Plan)

> Tài liệu này là khung mẫu - các thành viên cập nhật chi tiết khi thực hiện kiểm thử dự án.

## 1. Giới thiệu

### 1.1 Mục đích
Tài liệu mô tả phạm vi, phương pháp, công cụ, lịch trình và tiêu chí chấp nhận cho hoạt động kiểm thử ứng dụng ví điện tử **EzWallet**.

### 1.2 Đối tượng đọc
- Thành viên nhóm phát triển
- Người phụ trách kiểm thử
- Người tiếp quản dự án (nếu có)

### 1.3 Tham chiếu
- Tài liệu đặc tả chức năng EzWallet
- Tài liệu kiến trúc và hợp đồng API của dự án
- ISO/IEC/IEEE 29119

## 2. Phạm vi kiểm thử

### 2.1 In-scope
- 4 module nghiệp vụ chính: auth, topup-withdraw, transfer, bill-payment
- API REST (backend Spring Boot)
- Giao diện web (Angular)
- Các luồng end-to-end: đăng ký → đăng nhập → nạp tiền → chuyển khoản → thanh toán hoá đơn

### 2.2 Out-of-scope
- Tích hợp thật với cổng thanh toán ngân hàng (mock)
- Mobile app
- Kiểm thử khả năng phục hồi (disaster recovery)

## 3. Đối tượng và thành phần được kiểm thử

| ID | Module | Thành viên phụ trách | Hình thức kiểm thử |
|----|--------|---------------------|---------------------|
| M1 | Đăng ký / Đăng nhập / OTP | TV1 | Black-box, white-box, UI, security |
| M2 | Nạp tiền / Rút tiền       | TV2 | Black-box, white-box, UI |
| M3 | Chuyển tiền / QR          | TV3 (Long) | Black-box, white-box, UI, automation |
| M4 | Thanh toán hoá đơn / Lịch sử | TV4 | Black-box, white-box, UI |

## 4. Cách tiếp cận kiểm thử

### 4.1 Mức kiểm thử
- **Đơn vị (Unit)**: JUnit 5 + Mockito cho service backend; Karma + Jasmine cho service frontend.
- **Tích hợp (Integration)**: Testcontainers (PostgreSQL thật), `@SpringBootTest` trên controller + repository.
- **Hệ thống (System / E2E)**: Cypress (frontend) + RestAssured/Postman (backend).
- **Chấp nhận (UAT)**: Demo nghiệm thu cuối kỳ.

### 4.2 Kỹ thuật kiểm thử hộp đen
- Phân lớp tương đương (Equivalence Partitioning - EP)
- Phân tích giá trị biên (Boundary Value Analysis - BVA)
- Bảng quyết định (Decision Table)
- Kiểm thử theo cặp (Pairwise / All-pairs)
- Bảng chuyển trạng thái (State Transition)

### 4.3 Kỹ thuật kiểm thử hộp trắng
- Bao phủ câu lệnh (Statement Coverage)
- Bao phủ nhánh (Branch Coverage)
- Bao phủ điều kiện (Condition Coverage)

### 4.4 Kỹ thuật bổ sung (không bắt buộc, gia tăng điểm)
- Kiểm thử tự động giao diện - **Cypress / Selenium**
- Kiểm thử hiệu năng - **JMeter** (API chuyển khoản, lịch sử)
- Kiểm thử bảo mật - **OWASP ZAP** (auth endpoints)
- Áp dụng AI - dùng Copilot/ChatGPT để gợi ý test case

## 5. Tiêu chí

### 5.1 Tiêu chí bắt đầu (Entry)
- Tài liệu đặc tả module đã được rà soát (review).
- Code module đã build thành công.
- Môi trường docker-compose chạy ổn định.

### 5.2 Tiêu chí kết thúc (Exit)
- 100% test case đã được thực thi.
- ≥ 90% test case PASS.
- Test case FAIL phải có defect tương ứng được log.
- Code coverage backend ≥ 70% (statement coverage).

### 5.3 Tiêu chí tạm dừng (Suspension)
- > 30% test case FAIL trong 1 lần chạy.
- Defect nghiêm trọng (Critical / Blocker) chưa được fix.

## 6. Sản phẩm bàn giao

- `docs/qa/test-plan.md` (file này)
- `docs/qa/review-checklist.xlsx` - kết quả rà soát tài liệu/code
- `docs/qa/test-cases/<member>.xlsx` - bộ test case của từng thành viên
- `docs/qa/test-execution-report.xlsx` - báo cáo kết quả thực thi
- `docs/postman/*` - bộ test API
- Báo cáo Word tổng kết (theo template đề bài).

## 7. Môi trường kiểm thử

| Thành phần | Cấu hình |
|-----------|----------|
| OS | macOS / Windows / Linux |
| Java | 17 (Temurin) |
| Node.js | 20.x LTS |
| Database | PostgreSQL 16 (Docker) |
| Storage | MinIO (Docker) |
| Cache | Redis 7 (Docker) |
| Browser | Chrome 124+, Firefox 125+ |

## 8. Lịch trình (đề xuất, điều chỉnh theo lịch lớp)

| Tuần | Hoạt động |
|------|-----------|
| W1-2 | Hoàn thiện đặc tả + kiến trúc, rà soát chéo |
| W3-4 | Phát triển backend + frontend các module |
| W5   | Viết test case (theo đủ kỹ thuật giáo trình) |
| W6   | Thực thi unit test + integration test |
| W7   | Kiểm thử hệ thống + UI |
| W8   | Kiểm thử automation (Cypress + JMeter + ZAP) |
| W9   | Tổng hợp báo cáo + chuẩn bị bảo vệ |

## 9. Rủi ro và phương án giảm thiểu

| Rủi ro | Mức độ | Phương án |
|--------|--------|-----------|
| Lệch yêu cầu giữa các module | Cao | Định nghĩa API contract trước, review chéo |
| Chậm tiến độ do thành viên thiếu Spring Boot | Trung bình | Long hỗ trợ, dùng skeleton sẵn có |
| Conflict git khi 4 người làm song song | Cao | Module-based packaging, mỗi người 1 thư mục |
| Môi trường docker không chạy được | Thấp | Cung cấp Makefile + README |

## 10. Phân công cụ thể

| Hoạt động | Phụ trách |
|-----------|-----------|
| Lập test plan (file này) | Cả nhóm thống nhất |
| Review checklist tài liệu | Cả nhóm |
| Test case từng module | Từng thành viên tự viết |
| Tổng hợp báo cáo cuối | Trưởng nhóm |
| Demo bảo vệ | Phân chia theo module |
