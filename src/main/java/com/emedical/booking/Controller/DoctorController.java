package com.emedical.booking.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import com.emedical.booking.dto.ResponseDTO.ResponseDTO;
import com.emedical.booking.dto.DoctorDTO.RegisterDoctorRequest;
import com.emedical.booking.dto.DoctorDTO.logoutDoctorResquest;
import com.emedical.booking.dto.UserDTO.LoginRequest;
import com.emedical.booking.dto.UserDTO.LoginResponse;
import com.emedical.booking.dto.UserDTO.VerifyOtpRequest;
import com.emedical.booking.entity.Doctor;
import com.emedical.booking.entity.DoctorSlot;
import com.emedical.booking.entity.Appoinment;
import com.emedical.booking.security.CustomUserDetailsService;
import com.emedical.booking.service.DoctorService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final CustomUserDetailsService customUserDetailsService;
    private final SecurityContextRepository securityContextRepository;

    // Lấy toàn bộ danh sách bác sĩ nội bộ của bệnh viện
    @GetMapping({"/list", "/all"})
    public ResponseEntity<ResponseDTO<List<Doctor>>> getAllDoctors() {
        try {
            List<Doctor> doctors = doctorService.getAllDoctors();
            return ResponseEntity.ok(
                    new ResponseDTO<>(true, "Lấy danh sách bác sĩ nội bộ thành công", doctors));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    // 1. Đăng ký bác sĩ nội bộ (Lưu thẳng CSDL, không bắt buộc OTP khi đăng ký)
    @PostMapping("/register")
    public ResponseEntity<ResponseDTO<Doctor>> register(
            @RequestBody RegisterDoctorRequest request) {
        try {
            Doctor doctor = doctorService.register(request);
            return ResponseEntity.ok(
                    new ResponseDTO<>(true, "Đăng ký tài khoản bác sĩ thành công! Vui lòng đăng nhập.", doctor));
        } catch (Exception e) {
            log.warn("Đăng ký bác sĩ thất bại: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    // 2. Đăng nhập bác sĩ qua 2 bước bảo mật:
    // - Bước 1: Gửi username/password -> Sinh mã OTP gửi email
    // - Bước 2: Gửi kèm mã OTP -> Xác thực và thiết lập Session
    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<LoginResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            // Bước 1: Nếu chưa có OTP -> Kiểm tra username & password và tạo mã OTP
            if (request.getOtp() == null) {
                Doctor doctor = doctorService.validateCredentialsAndCreateOtp(request);
                Integer otp = doctorService.getLoginOtp(doctor.getUsername());

                String maskedEmail = maskEmail(doctor.getEmail());
                String message = "Thông tin đăng nhập hợp lệ! Mã OTP xác nhận đăng nhập Bác sĩ đã được gửi tới "
                        + maskedEmail + (otp != null ? " (Mã OTP thử nghiệm: " + otp + ")" : "");

                LoginResponse pending = new LoginResponse(doctor.getUsername(), true, doctor.getEmail(), otp);
                return ResponseEntity.ok(new ResponseDTO<>(true, message, pending));
            }

            // Bước 2: Đã có mã OTP -> Xác thực OTP và thiết lập phiên đăng nhập Bác sĩ
            Doctor doctor = doctorService.verifyLoginOtp(request.getUsername(), request.getOtp());

            UserDetails userDetails = customUserDetailsService.loadDoctorByUsername(doctor.getUsername());
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities());

            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                httpRequest.changeSessionId();
            } else {
                session = httpRequest.getSession(true);
            }

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            List<String> roles = authentication.getAuthorities()
                    .stream()
                    .map(a -> a.getAuthority())
                    .toList();

            LoginResponse loginResponse = new LoginResponse(doctor.getUsername(), roles);
            return ResponseEntity.ok(
                    new ResponseDTO<>(true, "Xác nhận OTP thành công! Bác sĩ đăng nhập thành công.", loginResponse));

        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(401).body(
                    new ResponseDTO<>(false, "Username hoặc password không đúng", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    // Endpoint riêng để xác nhận OTP đăng nhập bác sĩ
    @PostMapping("/verify-login-otp")
    public ResponseEntity<ResponseDTO<LoginResponse>> verifyLoginOtp(
            @RequestBody VerifyOtpRequest verifyRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LoginRequest loginRequest = new LoginRequest();
        String identifier = verifyRequest.getUsername() != null && !verifyRequest.getUsername().isBlank()
                ? verifyRequest.getUsername()
                : verifyRequest.getEmail();
        loginRequest.setUsername(identifier);
        loginRequest.setOtp(verifyRequest.getOtp());
        return login(loginRequest, httpRequest, httpResponse);
    }

    // Endpoint verify-otp cũ để giữ tương thích
    @PostMapping("/verify-otp")
    public ResponseEntity<ResponseDTO<String>> verifyOtp(
            @RequestBody VerifyOtpRequest verifyOtpRequest) {
        try {
            doctorService.verifyOtp(verifyOtpRequest);
            return ResponseEntity.ok(
                    new ResponseDTO<>(true, "Xác thực OTP thành công", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    // Đăng xuất bác sĩ & Hủy Redis Session
    @PostMapping({"/logout", "/logout/{username}"})
    public ResponseEntity<ResponseDTO<Void>> logout(
            @PathVariable(required = false) String username,
            @RequestBody(required = false) logoutDoctorResquest logoutDoctorResquest,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String doctorUsername = (username != null) ? username : (logoutDoctorResquest != null ? logoutDoctorResquest.getUsername() : null);
            if (doctorUsername != null) {
                doctorService.logout(new logoutDoctorResquest(doctorUsername, null));
            }

            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            securityContextRepository.saveContext(SecurityContextHolder.createEmptyContext(), request, response);

            return ResponseEntity.ok(
                    new ResponseDTO<>(true, "Đăng xuất thành công", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    // Xem các slot khám của bác sĩ
    @GetMapping("/{username}/slots")
    public ResponseEntity<ResponseDTO<List<DoctorSlot>>> getDoctorSlots(
            @PathVariable String username) {
        try {
            List<DoctorSlot> slots = doctorService.ReadingDoctoSlots(username);
            return ResponseEntity.ok(
                    new ResponseDTO<>(true, "Lấy danh sách slot thành công", slots));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    // Xem danh sách lịch hẹn theo ID bác sĩ
    @GetMapping("/{doctorId}/appointments")
    public ResponseEntity<ResponseDTO<List<Appoinment>>> getAppointments(
            @PathVariable Long doctorId) {
        try {
            List<Appoinment> appointments = doctorService.ReadingAppoinmentsUser(doctorId);
            return ResponseEntity.ok(
                    new ResponseDTO<>(true, "Lấy danh sách lịch hẹn thành công", appointments));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    // Từ chối / chuyển lịch hẹn sang bác sĩ khác
    @PostMapping("/{doctorId}/appointments/{appointmentId}/reject")
    public ResponseEntity<ResponseDTO<List<Appoinment>>> rejectAppointment(
            @PathVariable Long doctorId,
            @PathVariable Long appointmentId) {
        try {
            List<Appoinment> appointments = doctorService.RejectAppointment(doctorId, appointmentId);
            return ResponseEntity.ok(
                    new ResponseDTO<>(true, "Xử lý lịch hẹn thành công", appointments));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email != null ? email : "email";
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 3) {
            return name.charAt(0) + "***@" + parts[1];
        }
        return name.substring(0, 2) + "***" + name.charAt(name.length() - 1) + "@" + parts[1];
    }
}
