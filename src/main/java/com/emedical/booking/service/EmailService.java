package com.emedical.booking.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtp(String email, int otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("mã otp đăng kí tài khoản");
        message.setText(
                "mã otp của bạn là :" + otp + "\nMã có hạn trong thời gian giới hạn");

        mailSender.send(message);

    }

}
