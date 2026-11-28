package com.emedical.booking.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.emedical.booking.dto.UserDTO.BookingRequest;
import com.emedical.booking.dto.UserDTO.GetAppointmentResponse;
import com.emedical.booking.dto.UserDTO.LoginRequest;
import com.emedical.booking.dto.UserDTO.LogoutRequest;
import com.emedical.booking.dto.UserDTO.RegisterRequest;
import com.emedical.booking.dto.UserDTO.UpdateUserRequest;
import com.emedical.booking.dto.UserDTO.VerifyOtpRequest;
import com.emedical.booking.dto.UserDTO.deleteUserRequest;
import com.emedical.booking.entity.Appoinment;
import com.emedical.booking.entity.User;
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
    private final OtpService otpService;
    private final AppoinmentService appoinmentService;

    public User register(RegisterRequest registerRequest) {
        if (registerRequest.getUsername() == null || registerRequest.getUsername().isBlank()) {
            throw new RuntimeException("Tên đăng nhập không được để trống");
        }

        String username = registerRequest.getUsername().trim();
        log.info("Đang xử lí tạo tài khoản bệnh nhân mới: {}", username);

        if (userRepository.existsByUsername(username)) {
            log.warn("Username {} đã tồn tại", username);
            throw new RuntimeException("Tài khoản " + username + " đã được sử dụng.");
        }

        String confirmPassword = registerRequest.getEffectiveConfirmPassword();
        if (registerRequest.getPassword() == null || !registerRequest.getPassword().equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }

        if (registerRequest.getEmail() == null || registerRequest.getEmail().isBlank()) {
            throw new RuntimeException("Email không được để trống");
        }

        Integer phone = parsePhoneDigits(registerRequest.getPhone());
        String fullName = (registerRequest.getFullName() != null && !registerRequest.getFullName().isBlank())
                ? registerRequest.getFullName().trim()
                : username;

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .email(registerRequest.getEmail().trim())
                .full_name(fullName)
                .phone(phone)
                .address(registerRequest.getAddress())
                .role("ROLE_USER")
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Đăng ký thành công tài khoản bệnh nhân ID #{}: {}", savedUser.getUser_id(), savedUser.getUsername());
        return savedUser;
    }

    public User validateCredentialsAndCreateOtp(LoginRequest loginRequest) {
        User user = authenticateCredentials(loginRequest);
        otpService.issueLoginOtp(OtpService.USER_SCOPE, user.getUsername(), user.getEmail(), "Bệnh nhân");
        return user;
    }

    public Integer getLoginOtp(String usernameOrEmail) {
        return otpService.getLoginOtp(OtpService.USER_SCOPE, usernameOrEmail);
    }

    public User verifyLoginOtp(String usernameOrEmail, Integer otp) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new RuntimeException("Tên đăng nhập hoặc email không được để trống");
        }
        String identifier = usernameOrEmail.trim();
        Optional<User> userOpt = findByUsernameOrEmail(identifier);
        String fallbackIdentifier = userOpt
                .map(u -> identifier.equalsIgnoreCase(u.getUsername()) ? u.getEmail() : u.getUsername())
                .orElse(null);

        otpService.verifyAndConsumeOtp(OtpService.USER_SCOPE, identifier, fallbackIdentifier, otp);

        User user = userOpt.orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản"));
        otpService.clearOtp(OtpService.USER_SCOPE, user.getUsername(), user.getEmail());
        log.info("Xác thực OTP đăng nhập thành công cho bệnh nhân: {}", user.getUsername());
        return user;
    }

    public boolean verifyOtp(VerifyOtpRequest request) {
        String id = request.getUsername() != null && !request.getUsername().isBlank()
                ? request.getUsername()
                : request.getEmail();
        verifyLoginOtp(id, request.getOtp());
        return true;
    }

    public User login(LoginRequest loginRequest) {
        User user = authenticateCredentials(loginRequest);
        log.info("Đăng nhập thành công: {}", user.getUsername());
        return user;
    }

    public User logout(String username) {
        log.info("Đang xử lí đăng xuất tài khoản: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
    }

    public User logout(LogoutRequest logoutRequest) {
        return logout(logoutRequest != null ? logoutRequest.getUsername() : null);
    }

    public List<Appoinment> getAppoinments(GetAppointmentResponse request) {
        return appoinmentService.getAppointmentsForUser(
                request != null ? request.getUsername() : null,
                request != null ? request.getId() : null);
    }

    public Appoinment booking(BookingRequest bookingRequest) {
        return appoinmentService.bookAppointment(bookingRequest);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User get(Long id) {
        log.info("Searching user with id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User id does not exist"));
    }

    public User updateUser(UpdateUserRequest updateUserRequest) {
        User user = userRepository.findById(updateUserRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("User id does not exist"));

        if (updateUserRequest.getUsername() != null) {
            user.setUsername(updateUserRequest.getUsername());
        }
        if (updateUserRequest.getEmail() != null) {
            user.setEmail(updateUserRequest.getEmail());
        }
        if (updateUserRequest.getAddress() != null) {
            user.setAddress(updateUserRequest.getAddress());
        }
        if (updateUserRequest.getPhone() != null) {
            user.setPhone(updateUserRequest.getPhone());
        } else if (updateUserRequest.getPhoneNumber() != null) {
            Integer parsed = parsePhoneDigits(updateUserRequest.getPhoneNumber());
            if (parsed != null) {
                user.setPhone(parsed);
            }
        }

        return userRepository.save(user);
    }

    public User deleteUser(Long userId) {
        log.info("Deleting user with id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        userRepository.delete(user);
        return user;
    }

    public User deleteUser(deleteUserRequest deleteUserRequest) {
        return deleteUser(deleteUserRequest != null ? deleteUserRequest.getId() : null);
    }

    private User authenticateCredentials(LoginRequest loginRequest) {
        if (loginRequest.getUsername() == null || loginRequest.getUsername().isBlank()) {
            throw new RuntimeException("Tên đăng nhập không được để trống");
        }

        String username = loginRequest.getUsername().trim();
        log.info("Đang kiểm tra thông tin đăng nhập cho bệnh nhân: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + username));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Tài khoản hiện đang bị vô hiệu hóa");
        }

        return user;
    }

    private Optional<User> findByUsernameOrEmail(String identifier) {
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier));
    }

    private Integer parsePhoneDigits(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(rawPhone.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
