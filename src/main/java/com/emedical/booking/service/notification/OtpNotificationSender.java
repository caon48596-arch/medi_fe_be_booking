package com.emedical.booking.service.notification;

public interface OtpNotificationSender {

    void sendLoginOtp(String recipient, int otp, String roleName);
}
