package com.emedical.booking.service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.emedical.booking.dto.UserDTO.OtpData;
import com.emedical.booking.service.notification.OtpNotificationSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    public static final String USER_SCOPE = "USER";
    public static final String DOCTOR_SCOPE = "DOCTOR";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OtpNotificationSender otpNotificationSender;
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    public int issueLoginOtp(String scope, String username, String email, String roleDisplayName) {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        OtpData otpData = new OtpData(otp);

        otpStorage.put(buildKey(scope, username), otpData);
        if (email != null && !email.isBlank()) {
            otpStorage.put(buildKey(scope, email), otpData);
        }

        otpNotificationSender.sendLoginOtp(email, otp, roleDisplayName);
        log.info("Đã sinh mã OTP đăng nhập cho {} {} ({}): {}", roleDisplayName, username, email, otp);
        return otp;
    }

    public Integer getLoginOtp(String scope, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        OtpData data = otpStorage.get(buildKey(scope, identifier));
        if (data == null || data.isExpired()) {
            return null;
        }
        return data.getOtp();
    }

    public void verifyAndConsumeOtp(String scope, String primaryIdentifier, String secondaryIdentifier, Integer otp) {
        if (primaryIdentifier == null || primaryIdentifier.isBlank()) {
            throw new RuntimeException("Tên đăng nhập hoặc email không được để trống");
        }
        if (otp == null) {
            throw new RuntimeException("Vui lòng nhập mã OTP xác nhận đăng nhập");
        }

        String primaryKey = buildKey(scope, primaryIdentifier);
        OtpData otpData = otpStorage.get(primaryKey);
        if (otpData == null && secondaryIdentifier != null && !secondaryIdentifier.isBlank()) {
            otpData = otpStorage.get(buildKey(scope, secondaryIdentifier));
        }

        if (otpData == null) {
            throw new RuntimeException("Mã OTP không tồn tại hoặc phiên đăng nhập đã hết hạn. Vui lòng thử đăng nhập lại.");
        }

        if (otpData.isExpired()) {
            clearOtp(scope, primaryIdentifier, secondaryIdentifier);
            throw new RuntimeException("Mã OTP đã hết hiệu lực. Vui lòng đăng nhập lại để nhận mã mới.");
        }

        if (!otpData.getOtp().equals(otp)) {
            otpData.increaseAttempt();
            if (otpData.getAttempts() >= OtpData.MAX_ATTEMPTS) {
                clearOtp(scope, primaryIdentifier, secondaryIdentifier);
                throw new RuntimeException("Bạn đã nhập sai mã OTP quá 5 lần. Yêu cầu đăng nhập bị hủy.");
            }
            throw new RuntimeException(
                    "Mã OTP không chính xác. Bạn còn " + (OtpData.MAX_ATTEMPTS - otpData.getAttempts()) + " lần thử.");
        }

        clearOtp(scope, primaryIdentifier, secondaryIdentifier);
    }

    public void clearOtp(String scope, String... identifiers) {
        if (identifiers == null) {
            return;
        }
        for (String id : identifiers) {
            if (id != null && !id.isBlank()) {
                otpStorage.remove(buildKey(scope, id));
            }
        }
    }

    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email != null ? email : "email";
        }
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 3) {
            return name.charAt(0) + "***@" + parts[1];
        }
        return name.substring(0, 2) + "***" + name.charAt(name.length() - 1) + "@" + parts[1];
    }

    private String buildKey(String scope, String identifier) {
        return scope + ":" + identifier.trim().toLowerCase();
    }
}
