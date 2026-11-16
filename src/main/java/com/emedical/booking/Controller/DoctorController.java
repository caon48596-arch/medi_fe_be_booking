package com.emedical.booking.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.emedical.booking.service.DoctorService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    // Đăng ký bác sĩ
    @PostMapping("/register")
    public ResponseEntity<ResponseDTO<Doctor>> register(
            @RequestBody RegisterDoctorRequest request) {
        try {
            Doctor doctor = doctorService.register(request);
            return ResponseEntity.ok(
                    new ResponseDTO<>(true, "Đăng ký thành công", doctor));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    // Xác thực OTP bác sĩ
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

    // Đăng nhập bác sĩ với Spring Security & Redis Session
    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<LoginResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            // 1. Xác thực thông tin qua AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

            // 2. Chống Session Fixation
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                httpRequest.changeSessionId();
            } else {
                session = httpRequest.getSession(true);
            }

            // 3. Tạo SecurityContext
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // 4. Lưu trạng thái đăng nhập vào Redis Session
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            List<String> roles = authentication.getAuthorities()
                    .stream()
                    .map(a -> a.getAuthority())
                    .toList();

            LoginResponse loginResponse = new LoginResponse(authentication.getName(), roles);
            return ResponseEntity.ok(
                    new ResponseDTO<>(true, "Đăng nhập thành công", loginResponse));

        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(401).body(
                    new ResponseDTO<>(false, "Username hoặc password không đúng", null));
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
}