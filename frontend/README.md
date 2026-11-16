# E-Medical Booking Frontend (ReactJS)

Giao diện người dùng cho hệ thống đặt lịch khám bệnh trực tuyến kết nối với Spring Boot Backend.

## 🚀 Các tính năng đã được xây dựng theo Backend

### 1. Phân hệ Bệnh nhân (User Portal)
- **Đăng ký tài khoản**: `POST /user/register`
- **Xác thực OTP**: `POST /user/verify-otp`
- **Đăng nhập / Đăng xuất**: `POST /user/login`, `POST /user/logout` (Lưu phiên qua Redis Session)
- **Đặt lịch khám nhanh theo chuyên khoa**: `POST /user/booking`
- **Đặt lịch theo khung giờ Bác sĩ (Slot)**: `POST /appointment/create`
- **Xem lịch hẹn của tôi**: `GET /user/get?username={name}`
- **Thông tin cá nhân & Cập nhật**: `GET /user/getuser/{id}`, `PUT /user/updateUser/{id}`

### 2. Phân hệ Bác sĩ (Doctor Portal)
- **Đăng ký & Xác thực OTP Bác sĩ**: `POST /doctor/register`, `POST /doctor/verify-otp`
- **Đăng nhập Bác sĩ**: `POST /doctor/login`
- **Tạo ca khám tự động theo khung giờ (Slot Generator)**: `POST /doctor-slots/create`
- **Xem & Xóa ca khám của bác sĩ**: `GET /doctor/{username}/slots`, `DELETE /doctor-slots/delete`
- **Danh sách lịch hẹn bệnh nhân**: `GET /doctor/{doctorId}/appointments`
- **Từ chối & Tự động chuyển giao lịch hẹn**: `POST /doctor/{doctorId}/appointments/{appointmentId}/reject`

### 3. Phân hệ Hồ sơ Bệnh án (Medical Records)
- **Tạo hồ sơ bệnh án mới**: `POST /medical-records/create`
- **Xem danh sách hoặc tra cứu hồ sơ**: `POST /medical-records/read`
- **Cập nhật hồ sơ bệnh án**: `PUT /medical-records/update`
- **Xóa hồ sơ bệnh án**: `DELETE /medical-records/delete`

---

## 🛠️ Hướng dẫn Chạy & Phát triển

### 1. Chạy môi trường Development (Vite Dev Server)
```bash
cd frontend
npm install
npm run dev
```
Giao diện sẽ chạy tại: `http://localhost:5173` (đã cấu hình sẵn proxy chuyển tiếp tự động sang backend `http://localhost:8080`).

### 2. Build giao diện đóng gói chung vào Spring Boot (Khuyên dùng để deploy nhanh nhất)
```bash
cd frontend
npm run build:backend
```
Lệnh trên sẽ tự động biên dịch React sang thư mục `src/main/resources/static`.
Sau đó khi chạy Spring Boot (`mvn spring-boot:run` hoặc file JAR), cả Backend và Frontend đều chạy chung tại:
👉 `http://localhost:8080` (Không sợ lỗi CORS, không cần chạy 2 server riêng biệt).

### 3. Build Docker độc lập (Nginx)
```bash
docker build -t emedical-frontend ./frontend
docker run -p 3000:80 emedical-frontend
```
