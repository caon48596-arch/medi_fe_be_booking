package com.emedical.booking.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.emedical.booking.dto.DoctorDTO.RegisterDoctorRequest;
import com.emedical.booking.dto.DoctorDTO.logoutDoctorResquest;
import com.emedical.booking.dto.ResponseDTO.ResponseDTO;
import com.emedical.booking.dto.UserDTO.LoginRequest;
import com.emedical.booking.dto.UserDTO.LoginResponse;
import com.emedical.booking.dto.UserDTO.VerifyOtpRequest;
import com.emedical.booking.entity.Appoinment;
import com.emedical.booking.entity.Doctor;
import com.emedical.booking.entity.DoctorSlot;
import com.emedical.booking.security.CustomUserDetailsService;
import com.emedical.booking.security.SessionAuthenticationService;
import com.emedical.booking.service.AppoinmentService;
import com.emedical.booking.service.DoctorService;
import com.emedical.booking.service.DoctorSlotsService;
import com.emedical.booking.service.OtpService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final DoctorSlotsService doctorSlotsService;
    private final AppoinmentService appoinmentService;
    private final OtpService otpService;
    private final CustomUserDetailsService customUserDetailsService;
    private final SessionAuthenticationService sessionAuthenticationService;

    @GetMapping({"/list", "/all"})
    public ResponseEntity<ResponseDTO<List<Doctor>>> getAllDoctors() {
        List<Doctor> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(
                new ResponseDTO<>(true, "Lấy danh sách bác sĩ nội bộ thành công", doctors));
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseDTO<Doctor>> register(@RequestBody RegisterDoctorRequest request) {
        Doctor doctor = doctorService.register(request);
        return ResponseEntity.ok(
                new ResponseDTO<>(true, "Đăng ký tài khoản bác sĩ thành công! Vui lòng đăng nhập.", doctor));
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<LoginResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        if (request.getOtp() == null) {
            Doctor doctor = doctorService.validateCredentialsAndCreateOtp(request);
            Integer otp = doctorService.getLoginOtp(doctor.getUsername());

            String maskedEmail = otpService.maskEmail(doctor.getEmail());
            String message = "Thông tin đăng nhập hợp lệ! Mã OTP xác nhận đăng nhập Bác sĩ đã được gửi tới "
                    + maskedEmail + (otp != null ? " (Mã OTP thử nghiệm: " + otp + ")" : "");

            LoginResponse pending = new LoginResponse(doctor.getUsername(), true, doctor.getEmail(), otp);
            return ResponseEntity.ok(new ResponseDTO<>(true, message, pending));
        }

        Doctor doctor = doctorService.verifyLoginOtp(request.getUsername(), request.getOtp());
        UserDetails userDetails = customUserDetailsService.loadDoctorByUsername(doctor.getUsername());
        List<String> roles = sessionAuthenticationService.establishSession(userDetails, httpRequest, httpResponse);

        LoginResponse loginResponse = new LoginResponse(doctor.getUsername(), roles);
        return ResponseEntity.ok(
                new ResponseDTO<>(true, "Xác nhận OTP thành công! Bác sĩ đăng nhập thành công.", loginResponse));
    }

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

    @PostMapping("/verify-otp")
    public ResponseEntity<ResponseDTO<String>> verifyOtp(@RequestBody VerifyOtpRequest verifyOtpRequest) {
        doctorService.verifyOtp(verifyOtpRequest);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Xác thực OTP thành công", "SUCCESS"));
    }

    @PostMapping({"/logout", "/logout/{username}"})
    public ResponseEntity<ResponseDTO<Void>> logout(
            @PathVariable(required = false) String username,
            @RequestBody(required = false) logoutDoctorResquest logoutDoctorResquest,
            HttpServletRequest request,
            HttpServletResponse response) {
        String doctorUsername = (username != null) ? username
                : (logoutDoctorResquest != null ? logoutDoctorResquest.getUsername() : null);
        if (doctorUsername != null) {
            doctorService.logout(doctorUsername);
        }
        sessionAuthenticationService.clearSession(request, response);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Đăng xuất thành công", null));
    }

    @GetMapping("/{username}/slots")
    public ResponseEntity<ResponseDTO<List<DoctorSlot>>> getDoctorSlots(@PathVariable String username) {
        List<DoctorSlot> slots = doctorSlotsService.getSlotsByDoctorUsername(username);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Lấy danh sách slot thành công", slots));
    }

    @GetMapping("/{doctorId}/appointments")
    public ResponseEntity<ResponseDTO<List<Appoinment>>> getAppointments(@PathVariable Long doctorId) {
        List<Appoinment> appointments = appoinmentService.getAppointmentsByDoctor(doctorId);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Lấy danh sách lịch hẹn thành công", appointments));
    }

    @PostMapping("/{doctorId}/appointments/{appointmentId}/reject")
    public ResponseEntity<ResponseDTO<List<Appoinment>>> rejectAppointment(
            @PathVariable Long doctorId,
            @PathVariable Long appointmentId) {
        List<Appoinment> appointments = appoinmentService.rejectAndReassignAppointment(doctorId, appointmentId);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Xử lý lịch hẹn thành công", appointments));
    }
}
