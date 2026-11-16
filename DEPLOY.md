# HƯỚNG DẪN TRIỂN KHAI VÀ CHẠY DỰ ÁN (DEPLOYMENT GUIDE)

Dự án gồm **Spring Boot 3.2.5 (Backend)** + **ReactJS Vite (Frontend)** + **PostgreSQL & Redis (Docker)**.

---

## ⚡ Cách 1: Chạy và Deploy nhanh nhất (Gộp Frontend vào 1 file JAR Spring Boot)

Đây là cách đơn giản và tiện nhất để deploy: Spring Boot sẽ tự phục vụ trực tiếp giao diện React trên cổng `8080`.

### Bước 1: Khởi động Database & Redis
```bash
docker-compose up -d
```
*(Lệnh này khởi động Postgres cổng 5432 và Redis cổng 6379)*

### Bước 2: Build Frontend vào thư mục static của Backend
```bash
cd frontend
npm install
npm run build:backend
cd ..
```

### Bước 3: Đóng gói và chạy file JAR duy nhất
```bash
# Windows:
.\mvnw.cmd clean package -DskipTests
java -jar target/booking-0.0.1-SNAPSHOT.jar

# Linux / Mac:
./mvnw clean package -DskipTests
java -jar target/booking-0.0.1-SNAPSHOT.jar
```
Truy cập ngay trên trình duyệt: **`http://localhost:8080`**

---

## ⚡ Cách 2: Chạy song song cả Frontend & Backend khi lập trình (Dev Mode)

Nếu muốn code và xem thay đổi ngay lập tức (Hot Reload):

Chỉ cần chạy **1 lệnh duy nhất** ở thư mục gốc (hoặc trong thư mục `frontend`):
```bash
npm run dev
```
*(Lệnh này tự động khởi chạy song song cả **Backend Spring Boot** tại `http://localhost:8080` và **Frontend React Vite** tại `http://localhost:5173` với log phân màu rõ ràng).*

Nếu muốn chạy riêng lẻ từng phần:
- Chỉ chạy Backend: `npm run dev:backend` (hoặc `.\mvnw.cmd spring-boot:run`)
- Chỉ chạy Frontend: `npm run dev:frontend` (hoặc vào `frontend` gõ `npm run dev:fe`)

---

## ⚡ Cách 3: Deploy Frontend lên Vercel / Netlify / Render

- **Build command**: `npm run build`
- **Output directory**: `dist`
- **Root directory**: `frontend`
- Trong giao diện web, nhấn biểu tượng **Bánh răng (Cài đặt)** trên thanh Navbar để điền URL của Backend khi deploy (ví dụ: `https://your-backend.railway.app`).

---

## 🔒 Các lỗi bảo mật & cấu hình đã được xử lý sẵn:
1. Đã bật cấu hình **CORS** cho phép Frontend gọi API và lưu phiên đăng nhập Cookie Session.
2. Đã tắt **CSRF** mặc định gây lỗi 403 Forbidden trên REST API.
3. Đã sửa phân quyền truy cập cho các API mở: `/user/register`, `/user/verify-otp`, `/user/login`, `/doctor/register`, `/doctor/verify-otp`, `/doctor/login`.
4. Đã bổ sung bean `SecurityContextRepository` để Spring Security quản lý Redis Session chuẩn xác.
