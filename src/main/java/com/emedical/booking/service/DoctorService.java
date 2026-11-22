package com.emedical.booking.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    // Bộ nhớ tạm lưu OTP dùng để xác nhận ĐĂNG NHẬP bác sĩ
    private final Map<String, OtpData> doctorLoginOtpStorage = new ConcurrentHashMap<>();

    /**
     * Đăng ký tài khoản bác sĩ nội bộ:
     * Lưu thông tin trực tiếp vào cơ sở dữ liệu (không bắt buộc OTP khi đăng ký).
     */
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
                : "Khoa Tim Mạch";

        String room = (registerDoctorRequest.getRoom_number() != null && !registerDoctorRequest.getRoom_number().isBlank())
                ? registerDoctorRequest.getRoom_number().trim()
                : "Phòng 201 - Tầng 2";

        Integer price = registerDoctorRequest.getPrice() != null && registerDoctorRequest.getPrice() > 0
                ? registerDoctorRequest.getPrice()
                : 250000;

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

    /**
     * Xác thực thông tin tài khoản bác sĩ (username + password) và sinh mã OTP gửi về Email để xác nhận ĐĂNG NHẬP.
     */
    public Doctor validateCredentialsAndCreateOtp(LoginRequest loginRequest) {
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

        // Sinh mã OTP ngẫu nhiên 6 chữ số
        int otp = 100000 + new Random().nextInt(900000);
        OtpData otpData = new OtpData(otp);

        doctorLoginOtpStorage.put(doctor.getUsername().toLowerCase(), otpData);
        if (doctor.getEmail() != null && !doctor.getEmail().isBlank()) {
            doctorLoginOtpStorage.put(doctor.getEmail().toLowerCase(), otpData);
        }

        emailService.sendLoginOtp(doctor.getEmail(), otp, "Bác sĩ");
        log.info("Mã OTP xác nhận đăng nhập cho bác sĩ {} ({}) là: {}", username, doctor.getEmail(), otp);

        return doctor;
    }

    /**
     * Lấy mã OTP đăng nhập bác sĩ mới nhất
     */
    public Integer getLoginOtp(String usernameOrEmail) {
        if (usernameOrEmail == null) return null;
        OtpData data = doctorLoginOtpStorage.get(usernameOrEmail.trim().toLowerCase());
        return data != null ? data.getOtp() : null;
    }

    /**
     * Xác thực mã OTP để hoàn tất ĐĂNG NHẬP BÁC SĨ.
     */
    public Doctor verifyLoginOtp(String usernameOrEmail, Integer otp) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new RuntimeException("Tên đăng nhập hoặc email không được để trống");
        }
        if (otp == null) {
            throw new RuntimeException("Vui lòng nhập mã OTP xác nhận đăng nhập");
        }

        String key = usernameOrEmail.trim().toLowerCase();
        OtpData otpData = doctorLoginOtpStorage.get(key);

        if (otpData == null) {
            Optional<Doctor> docOpt = doctorRepository.findByUsername(usernameOrEmail.trim());
            if (docOpt.isPresent() && docOpt.get().getEmail() != null) {
                otpData = doctorLoginOtpStorage.get(docOpt.get().getEmail().toLowerCase());
            }
        }

        if (otpData == null) {
            throw new RuntimeException("Mã OTP không tồn tại hoặc phiên đăng nhập đã hết hạn. Vui lòng thử đăng nhập lại.");
        }

        if (System.currentTimeMillis() > otpData.getExpireTime()) {
            doctorLoginOtpStorage.remove(key);
            throw new RuntimeException("Mã OTP đã hết hiệu lực. Vui lòng đăng nhập lại để nhận mã mới.");
        }

        if (!otpData.getOtp().equals(otp)) {
            otpData.increaseAttempt();
            if (otpData.getAttempts() >= 5) {
                doctorLoginOtpStorage.remove(key);
                throw new RuntimeException("Bạn đã nhập sai mã OTP quá 5 lần. Yêu cầu đăng nhập bị hủy.");
            }
            throw new RuntimeException("Mã OTP không chính xác. Bạn còn " + (5 - otpData.getAttempts()) + " lần thử.");
        }

        doctorLoginOtpStorage.remove(key);

        Doctor doctor = doctorRepository.findByUsername(usernameOrEmail.trim())
                .orElseGet(() -> doctorRepository.findAll().stream()
                        .filter(d -> key.equalsIgnoreCase(d.getEmail()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản bác sĩ")));

        doctorLoginOtpStorage.remove(doctor.getUsername().toLowerCase());
        if (doctor.getEmail() != null) {
            doctorLoginOtpStorage.remove(doctor.getEmail().toLowerCase());
        }

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
        doctorRepository.findByUsername(logoutDoctorResquest.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
    }

    public List<Doctor> getAllDoctors() {
        log.info("Đang lấy danh sách toàn bộ bác sĩ nội bộ bệnh viện");
        return doctorRepository.findAll();
    }

    public List<DoctorSlot> ReadingDoctoSlots(String username) {
        log.info("Đang tải danh sách slot khám của bác sĩ {}", username);
        if (username == null || username.isBlank() || "all".equalsIgnoreCase(username)) {
            return doctorSlotRepository.findAll();
        }
        List<DoctorSlot> doctorSlot = doctorSlotRepository.findByDoctorUsername(username);
        log.info("Bác sĩ {} hiện có {} slot khám", username, doctorSlot.size());
        return doctorSlot;
    }

    public List<Appoinment> ReadingAppoinmentsUser(Long doctorId) {
        log.info("Đang tải dữ liệu appointment của bác sĩ ID {}", doctorId);
        if (doctorId == null) {
            return appoinmentRepository.findAll();
        }
        List<Appoinment> list = appoinmentRepository.findByDoctorId(doctorId);
        if (list.isEmpty()) {
            doctorRepository.findById(doctorId).ifPresent(d -> {
                list.addAll(appoinmentRepository.findByDoctorUsername(d.getUsername()));
            });
        }
        return list;
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
