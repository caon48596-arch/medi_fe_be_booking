package com.emedical.booking.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.emedical.booking.dto.UserDTO.UpdateUserRequest;
import com.emedical.booking.dto.UserDTO.VerifyOtpRequest;
import com.emedical.booking.dto.UserDTO.deleteUserRequest;
import com.emedical.booking.dto.UserDTO.BookingRequest;
import com.emedical.booking.dto.UserDTO.GetAppointmentResponse;
import com.emedical.booking.dto.UserDTO.LoginRequest;
import com.emedical.booking.dto.UserDTO.LogoutRequest;
import com.emedical.booking.dto.UserDTO.OtpData;
import com.emedical.booking.dto.UserDTO.RegisterRequest;
import com.emedical.booking.entity.Appoinment;
import com.emedical.booking.entity.Doctor;
import com.emedical.booking.entity.User;
import com.emedical.booking.repository.AppoinmentRepository;
import com.emedical.booking.repository.DoctorRepository;
import com.emedical.booking.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AppoinmentRepository appoinmentRepository;
    private final DoctorRepository doctorRepository;

    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, RegisterRequest> registerStorage = new ConcurrentHashMap<>();

    public User register(RegisterRequest registerRequest) {
        log.info("Đang xử lí tạo tài khoản cho người dùng mới: {}", registerRequest.getUsername());
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            log.warn("Username {} đã tồn tại", registerRequest.getUsername());
            throw new RuntimeException("Tài khoản " + registerRequest.getUsername() + " đã được sử dụng.");
        }

        if (registerRequest.getPassword() == null || !registerRequest.getPassword().equals(registerRequest.getConfirePassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }

        int otp = 100000 + new Random().nextInt(900000);
        otpStorage.put(registerRequest.getEmail(), new OtpData(otp));
        registerStorage.put(registerRequest.getEmail(), registerRequest);

        try {
            emailService.sendOtp(registerRequest.getEmail(), otp);
        } catch (Exception e) {
            log.warn("Không thể gửi email OTP (kiểm tra cấu hình SMTP): {}", e.getMessage());
        }
        log.info("OTP của {} là {}", registerRequest.getEmail(), otp);

        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .email(registerRequest.getEmail())
                .role("ROLE_USER")
                .active(true)
                .build();

        return userRepository.save(user);
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

    public User login(LoginRequest loginRequest) {
        log.info("Đang xử lí tài khoản đăng nhập: {}", loginRequest.getUsername());
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Tài khoản hiện đang bị vô hiệu hóa");
        }

        log.info("Đăng nhập thành công: {}", loginRequest.getUsername());
        return user;
    }

    public User logout(LogoutRequest logoutRequest) {
        log.info("Đang xử lí đăng xuất tài khoản: {}", logoutRequest.getUsername());
        User user = userRepository.findByUsername(logoutRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        return user;
    }

    public List<Appoinment> getAppoinments(GetAppointmentResponse getAppointmentResponse) {
        log.info("Đang tải danh sách lịch hẹn khám bệnh: {}", getAppointmentResponse.getUsername());
        if (!userRepository.existsByUsername(getAppointmentResponse.getUsername())) {
            log.warn("Tài khoản không tồn tại");
            return List.of();
        }

        return appoinmentRepository.findByUser_Id(getAppointmentResponse.getId());
    }

    public Appoinment booking(BookingRequest bookingRequest) {
        log.info("User {} đang đặt lịch với chuyên khoa: {}", bookingRequest.getUsername(), bookingRequest.getSpecialty());
        User user = userRepository.findByUsername(bookingRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng"));

        String specialty = bookingRequest.getSpecialty();
        if (specialty == null || specialty.isBlank()) {
            throw new RuntimeException("Chuyên khoa không được để trống");
        }

        Optional<Doctor> optionalDoctor = doctorRepository.findBySpecialty(specialty);
        Doctor doctor = optionalDoctor.orElse(null);

        Appoinment appoinment = Appoinment.builder()
                .user(user)
                .patientName(user.getUsername())
                .date(bookingRequest.getDate())
                .email(user.getEmail())
                .reason(bookingRequest.getReason())
                .status("PENDING")
                .doctor(doctor)
                .active(true)
                .build();

        return appoinmentRepository.save(appoinment);
    }

    public User get(Long id) {
        log.info("Searching user with id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User id does not exist"));
    }

    public User updateUser(UpdateUserRequest updateUserRequest) {
        User user = userRepository.findById(updateUserRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("User id does not exist"));

        if (updateUserRequest.getUsername() != null) user.setUsername(updateUserRequest.getUsername());
        if (updateUserRequest.getEmail() != null) user.setEmail(updateUserRequest.getEmail());
        if (updateUserRequest.getAddress() != null) user.setAddress(updateUserRequest.getAddress());
        if (updateUserRequest.getPhone() != null) {
            user.setPhone(updateUserRequest.getPhone());
        } else if (updateUserRequest.getPhoneNumber() != null) {
            try {
                user.setPhone(Integer.parseInt(updateUserRequest.getPhoneNumber().replaceAll("[^0-9]", "")));
            } catch (NumberFormatException ignored) {}
        }

        return userRepository.save(user);
    }

    public User deleteUser(deleteUserRequest deleteUserRequest) {
        log.info("Deleting user with id: {}", deleteUserRequest.getId());
        User user = userRepository.findById(deleteUserRequest.getId())
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        userRepository.delete(user);
        return user;
    }
}
