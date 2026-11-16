package com.emedical.booking.service;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.emedical.booking.dto.DoctorDTO.RegisterDoctorRequest;
import com.emedical.booking.dto.DoctorDTO.logoutDoctorResquest;
import com.emedical.booking.dto.UserDTO.LoginRequest;
import com.emedical.booking.dto.UserDTO.OtpData;
import com.emedical.booking.dto.UserDTO.VerifyOtpRequest;
import com.emedical.booking.entity.Appoinment;
import com.emedical.booking.entity.Doctor;
import com.emedical.booking.entity.DoctorSlot;
import com.emedical.booking.repository.AppoinmentRepository;
import com.emedical.booking.repository.DoctorRepository;
import com.emedical.booking.repository.DoctorSlotRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorSlotRepository doctorSlotRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AppoinmentRepository appoinmentRepository;

    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, RegisterDoctorRequest> registerStorage = new ConcurrentHashMap<>();

    public Doctor register(RegisterDoctorRequest registerDoctorRequest) {
        log.info("Đang xử lí tạo tài khoản cho bác sĩ mới: {}", registerDoctorRequest.getUsername());
        if (doctorRepository.existsByUsername(registerDoctorRequest.getUsername())) {
            log.warn("Username {} đã tồn tại", registerDoctorRequest.getUsername());
            throw new RuntimeException("Tài khoản " + registerDoctorRequest.getUsername() + " đã được sử dụng.");
        }

        if (registerDoctorRequest.getPassword() == null || !registerDoctorRequest.getPassword().equals(registerDoctorRequest.getConfirePassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }

        int otp = 100000 + new Random().nextInt(900000);
        otpStorage.put(registerDoctorRequest.getEmail(), new OtpData(otp));
        registerStorage.put(registerDoctorRequest.getEmail(), registerDoctorRequest);

        try {
            emailService.sendOtp(registerDoctorRequest.getEmail(), otp);
        } catch (Exception e) {
            log.warn("Không thể gửi email OTP (kiểm tra cấu hình SMTP): {}", e.getMessage());
        }
        log.info("OTP của {} là {}", registerDoctorRequest.getEmail(), otp);

        Doctor doctor = Doctor.builder()
                .username(registerDoctorRequest.getUsername())
                .password(passwordEncoder.encode(registerDoctorRequest.getPassword()))
                .email(registerDoctorRequest.getEmail())
                .active(true)
                .build();

        return doctorRepository.save(doctor);
    }

    public boolean verifyOtp(VerifyOtpRequest request) {
        OtpData otpData = otpStorage.get(request.getEmail());
        if (otpData == null) {
            throw new RuntimeException("OTP không tồn tại hoặc đã hết hạn");
        }
        if (System.currentTimeMillis() > otpData.getExpireTime()) {
            otpStorage.remove(request.getEmail());
            throw new RuntimeException("OTP đã hết hạn");
        }
        if (!otpData.getOtp().equals(request.getOtp())) {
            otpData.increaseAttempt();
            if (otpData.getAttempts() >= 5) {
                otpStorage.remove(request.getEmail());
                throw new RuntimeException("Nhập sai OTP quá 5 lần");
            }
            throw new RuntimeException("OTP không chính xác. Còn " + (5 - otpData.getAttempts()) + " lần thử");
        }
        otpStorage.remove(request.getEmail());
        return true;
    }

    public Doctor login(LoginRequest loginRequest) {
        log.info("Đang xử lí bác sĩ đăng nhập: {}", loginRequest.getUsername());
        Doctor doctor = doctorRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), doctor.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác");
        }

        if (!doctor.isActive()) {
            throw new RuntimeException("Tài khoản bác sĩ hiện đang bị vô hiệu hóa");
        }

        log.info("Bác sĩ đăng nhập thành công: {}", loginRequest.getUsername());
        return doctor;
    }

    public void logout(logoutDoctorResquest logoutDoctorResquest) {
        log.info("Đang xử lí đăng xuất tài khoản bác sĩ: {}", logoutDoctorResquest.getUsername());
        // Không set active = false vì sẽ vô hiệu hóa tài khoản bác sĩ
        doctorRepository.findByUsername(logoutDoctorResquest.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
    }

    public List<DoctorSlot> ReadingDoctoSlots(String username) {
        log.info("Đang tải danh sách slot khám của bác sĩ {}", username);
        List<DoctorSlot> doctorSlot = doctorSlotRepository.findByDoctorUsername(username);
        log.info("Bác sĩ {} hiện có {} slot khám", username, doctorSlot.size());
        return doctorSlot;
    }

    public List<Appoinment> ReadingAppoinmentsUser(Long doctorId) {
        log.info("Đang tải dữ liệu appointment của bác sĩ ID {}", doctorId);
        return appoinmentRepository.findByDoctorId(doctorId);
    }

    public List<Appoinment> RejectAppointment(Long doctorId, Long appoinmentId) {
        log.info("Bác sĩ ID {} yêu cầu từ chối và chuyển giao lịch hẹn ID: {}", doctorId, appoinmentId);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bác sĩ ID: " + doctorId));

        Appoinment appointment = appoinmentRepository.findById(appoinmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn ID: " + appoinmentId));

        List<DoctorSlot> doctorSlots = doctorSlotRepository
                .findByDoctor_IdNotAndDoctor_SpecialtyAndDateAndIsBooked(
                        doctorId,
                        doctor.getSpecialty(),
                        appointment.getDate(),
                        false);

        if (doctorSlots.isEmpty()) {
            log.warn("Không tìm thấy bác sĩ thay thế cùng chuyên khoa {} trong ngày {}",
                    doctor.getSpecialty(), appointment.getDate());
            appointment.setStatus("REJECTED_NO_DOCTOR_AVAILABLE");
            return List.of(appoinmentRepository.save(appointment));
        }

        DoctorSlot newSlot = doctorSlots.get(0);
        Doctor newDoctor = newSlot.getDoctor();

        DoctorSlot oldSlot = appointment.getDoctorSlot();
        if (oldSlot != null) {
            oldSlot.setBook(false);
            doctorSlotRepository.save(oldSlot);
        }

        newSlot.setActive(true);
        newSlot.setBook(true);
        doctorSlotRepository.save(newSlot);

        appointment.setDoctor(newDoctor);
        appointment.setDoctorSlot(newSlot);
        appointment.setStatus("REASSIGNED_PENDING");

        Appoinment savedAppointment = appoinmentRepository.save(appointment);
        log.info("Lịch hẹn ID {} đã được chuyển thành công sang Bác sĩ {} (ID: {})",
                appoinmentId, newDoctor.getUsername(), newDoctor.getDoctor_id());

        return List.of(savedAppointment);
    }
}