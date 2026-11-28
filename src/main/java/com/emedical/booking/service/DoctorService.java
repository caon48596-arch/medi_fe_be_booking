package com.emedical.booking.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.emedical.booking.dto.DoctorDTO.RegisterDoctorRequest;
import com.emedical.booking.dto.DoctorDTO.logoutDoctorResquest;
import com.emedical.booking.dto.UserDTO.LoginRequest;
import com.emedical.booking.dto.UserDTO.VerifyOtpRequest;
import com.emedical.booking.entity.Appoinment;
import com.emedical.booking.entity.Doctor;
import com.emedical.booking.entity.DoctorSlot;
import com.emedical.booking.repository.DoctorRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DoctorService {

    private static final String DEFAULT_SPECIALTY = "Khoa Tim Mạch";
    private static final String DEFAULT_ROOM = "Phòng 201 - Tầng 2";
    private static final int DEFAULT_PRICE = 250000;

    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final DoctorSlotsService doctorSlotsService;
    private final AppoinmentService appoinmentService;

    public Doctor register(RegisterDoctorRequest registerDoctorRequest) {
        if (registerDoctorRequest.getUsername() == null || registerDoctorRequest.getUsername().isBlank()) {
            throw new RuntimeException("Tên đăng nhập không được để trống");
        }

        String username = registerDoctorRequest.getUsername().trim();
        log.info("Đang xử lí tạo tài khoản cho bác sĩ mới: {}", username);

        if (doctorRepository.existsByUsername(username)) {
            log.warn("Username bác sĩ {} đã tồn tại", username);
            throw new RuntimeException("Tài khoản bác sĩ " + username + " đã được sử dụng.");
        }

        String confirmPassword = registerDoctorRequest.getEffectiveConfirmPassword();
        if (registerDoctorRequest.getPassword() == null || !registerDoctorRequest.getPassword().equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }

        if (registerDoctorRequest.getEmail() == null || registerDoctorRequest.getEmail().isBlank()) {
            throw new RuntimeException("Email không được để trống");
        }

        String specialty = (registerDoctorRequest.getSpecialty() != null && !registerDoctorRequest.getSpecialty().isBlank())
                ? registerDoctorRequest.getSpecialty().trim()
                : DEFAULT_SPECIALTY;

        String room = (registerDoctorRequest.getRoom_number() != null && !registerDoctorRequest.getRoom_number().isBlank())
                ? registerDoctorRequest.getRoom_number().trim()
                : DEFAULT_ROOM;

        Integer price = registerDoctorRequest.getPrice() != null && registerDoctorRequest.getPrice() > 0
                ? registerDoctorRequest.getPrice()
                : DEFAULT_PRICE;

        Doctor doctor = Doctor.builder()
                .username(username)
                .password(passwordEncoder.encode(registerDoctorRequest.getPassword()))
                .email(registerDoctorRequest.getEmail().trim())
                .specialty(specialty)
                .room_number(room)
                .price(price)
                .active(true)
                .build();

        Doctor savedDoctor = doctorRepository.save(doctor);
        log.info("Đăng ký thành công tài khoản Bác sĩ ID #{}: {}", savedDoctor.getDoctor_id(), savedDoctor.getUsername());
        return savedDoctor;
    }

    public Doctor validateCredentialsAndCreateOtp(LoginRequest loginRequest) {
        Doctor doctor = authenticateCredentials(loginRequest);
        otpService.issueLoginOtp(OtpService.DOCTOR_SCOPE, doctor.getUsername(), doctor.getEmail(), "Bác sĩ");
        return doctor;
    }

    public Integer getLoginOtp(String usernameOrEmail) {
        return otpService.getLoginOtp(OtpService.DOCTOR_SCOPE, usernameOrEmail);
    }

    public Doctor verifyLoginOtp(String usernameOrEmail, Integer otp) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new RuntimeException("Tên đăng nhập hoặc email không được để trống");
        }
        String identifier = usernameOrEmail.trim();
        Optional<Doctor> docOpt = findByUsernameOrEmail(identifier);
        String fallbackIdentifier = docOpt
                .map(d -> identifier.equalsIgnoreCase(d.getUsername()) ? d.getEmail() : d.getUsername())
                .orElse(null);

        otpService.verifyAndConsumeOtp(OtpService.DOCTOR_SCOPE, identifier, fallbackIdentifier, otp);

        Doctor doctor = docOpt.orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản bác sĩ"));
        otpService.clearOtp(OtpService.DOCTOR_SCOPE, doctor.getUsername(), doctor.getEmail());
        log.info("Xác thực OTP đăng nhập thành công cho bác sĩ: {}", doctor.getUsername());
        return doctor;
    }

    public boolean verifyOtp(VerifyOtpRequest request) {
        String id = request.getUsername() != null && !request.getUsername().isBlank()
                ? request.getUsername()
                : request.getEmail();
        verifyLoginOtp(id, request.getOtp());
        return true;
    }

    public Doctor login(LoginRequest loginRequest) {
        Doctor doctor = authenticateCredentials(loginRequest);
        log.info("Bác sĩ đăng nhập thành công: {}", doctor.getUsername());
        return doctor;
    }

    public void logout(String username) {
        log.info("Đang xử lí đăng xuất tài khoản bác sĩ: {}", username);
        doctorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
    }

    public void logout(logoutDoctorResquest logoutDoctorResquest) {
        if (logoutDoctorResquest != null) {
            logout(logoutDoctorResquest.getUsername());
        }
    }

    public List<Doctor> getAllDoctors() {
        log.info("Đang lấy danh sách toàn bộ bác sĩ nội bộ bệnh viện");
        return doctorRepository.findAll();
    }

    public List<DoctorSlot> getDoctorSlots(String username) {
        return doctorSlotsService.getSlotsByDoctorUsername(username);
    }

    public List<DoctorSlot> ReadingDoctoSlots(String username) {
        return getDoctorSlots(username);
    }

    public List<Appoinment> getDoctorAppointments(Long doctorId) {
        return appoinmentService.getAppointmentsByDoctor(doctorId);
    }

    public List<Appoinment> ReadingAppoinmentsUser(Long doctorId) {
        return getDoctorAppointments(doctorId);
    }

    public List<Appoinment> rejectAppointment(Long doctorId, Long appointmentId) {
        return appoinmentService.rejectAndReassignAppointment(doctorId, appointmentId);
    }

    public List<Appoinment> RejectAppointment(Long doctorId, Long appoinmentId) {
        return rejectAppointment(doctorId, appoinmentId);
    }

    private Doctor authenticateCredentials(LoginRequest loginRequest) {
        if (loginRequest.getUsername() == null || loginRequest.getUsername().isBlank()) {
            throw new RuntimeException("Tên đăng nhập không được để trống");
        }

        String username = loginRequest.getUsername().trim();
        log.info("Đang kiểm tra thông tin đăng nhập cho bác sĩ: {}", username);

        Doctor doctor = doctorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản bác sĩ: " + username));

        if (!passwordEncoder.matches(loginRequest.getPassword(), doctor.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác");
        }

        if (!doctor.isActive()) {
            throw new RuntimeException("Tài khoản bác sĩ hiện đang bị vô hiệu hóa");
        }

        return doctor;
    }

    private Optional<Doctor> findByUsernameOrEmail(String identifier) {
        return doctorRepository.findByUsername(identifier)
                .or(() -> doctorRepository.findByEmailIgnoreCase(identifier));
    }
}
