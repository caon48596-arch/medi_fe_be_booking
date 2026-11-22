package com.emedical.booking.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendLoginOtp(String email, int otp, String roleName) {
        if (email == null || email.isBlank()) {
            log.warn("Email trống, bỏ qua gửi email OTP");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Mã OTP xác nhận đăng nhập - Bệnh Viện Đa Khoa MediSession");
            message.setText("Kính gửi Quý khách / " + roleName + ",\n\n"
                    + "Mã OTP để xác nhận đăng nhập vào hệ thống nội bộ Bệnh Viện Đa Khoa MediSession của bạn là: " + otp + "\n\n"
                    + "Mã OTP này có hiệu lực trong vòng 5 phút.\n"
                    + "Vui lòng tuyệt đối không chia sẻ mã này cho bất kỳ ai vì lý do an toàn bảo mật tài khoản.\n\n"
                    + "Trân trọng,\n"
                    + "Ban Giám Đốc & Phòng CNTT - Bệnh Viện Đa Khoa MediSession");
            mailSender.send(message);
            log.info("Đã gửi email mã OTP đăng nhập ({}) tới {}", otp, email);
        } catch (Exception e) {
            log.warn("Không thể gửi email OTP tới {} (kiểm tra cấu hình SMTP): {}", email, e.getMessage());
        }
    }

    public void sendOtp(String email, int otp) {
        sendLoginOtp(email, otp, "Bệnh nhân / Bác sĩ");
    }
}
