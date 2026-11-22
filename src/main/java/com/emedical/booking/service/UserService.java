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
import com.emedical.booking.entity.DoctorSlot;
import com.emedical.booking.entity.User;
import com.emedical.booking.repository.AppoinmentRepository;
import com.emedical.booking.repository.DoctorRepository;
import com.emedical.booking.repository.DoctorSlotRepository;
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
    private final DoctorSlotRepository doctorSlotRepository;

    // Bộ nhớ tạm lưu OTP dùng để xác nhận ĐĂNG NHẬP
    private final Map<String, OtpData> loginOtpStorage = new ConcurrentHashMap<>();

    /**
     * Đăng ký tài khoản bệnh nhân nội bộ:
     * Tạo và lưu tài khoản trực tiếp vào cơ sở dữ liệu (không bắt buộc OTP khi đăng ký).
     */
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

        Integer phone = null;
        if (registerRequest.getPhone() != null && !registerRequest.getPhone().isBlank()) {
            try {
                phone = Integer.parseInt(registerRequest.getPhone().replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {}
        }

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

    /**
     * Xác thực thông tin tài khoản (username + password) và sinh mã OTP gửi về Email để xác nhận ĐĂNG NHẬP.
     */
    public User validateCredentialsAndCreateOtp(LoginRequest loginRequest) {
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

        // Sinh mã OTP 6 chữ số ngẫu nhiên
        int otp = 100000 + new Random().nextInt(900000);
        OtpData otpData = new OtpData(otp);

        loginOtpStorage.put(user.getUsername().toLowerCase(), otpData);
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            loginOtpStorage.put(user.getEmail().toLowerCase(), otpData);
        }

        emailService.sendLoginOtp(user.getEmail(), otp, "Bệnh nhân");
        log.info("Mã OTP xác nhận đăng nhập cho bệnh nhân {} ({}) là: {}", username, user.getEmail(), otp);

        return user;
    }

    /**
     * Lấy mã OTP đăng nhập mới nhất (phục vụ hiển thị trên môi trường thử nghiệm / dev khi chưa có SMTP thực tế)
     */
    public Integer getLoginOtp(String usernameOrEmail) {
        if (usernameOrEmail == null) return null;
        OtpData data = loginOtpStorage.get(usernameOrEmail.trim().toLowerCase());
        return data != null ? data.getOtp() : null;
    }

    /**
     * Xác thực mã OTP để hoàn tất ĐĂNG NHẬP.
     */
    public User verifyLoginOtp(String usernameOrEmail, Integer otp) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new RuntimeException("Tên đăng nhập hoặc email không được để trống");
        }
        if (otp == null) {
            throw new RuntimeException("Vui lòng nhập mã OTP xác nhận đăng nhập");
        }

        String key = usernameOrEmail.trim().toLowerCase();
        OtpData otpData = loginOtpStorage.get(key);

        // Nếu tìm bằng username chưa thấy, thử tìm bằng email
        if (otpData == null) {
            Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail.trim());
            if (userOpt.isPresent() && userOpt.get().getEmail() != null) {
                otpData = loginOtpStorage.get(userOpt.get().getEmail().toLowerCase());
            }
        }

        if (otpData == null) {
            throw new RuntimeException("Mã OTP không tồn tại hoặc phiên đăng nhập đã hết hạn. Vui lòng gửi lại yêu cầu đăng nhập.");
        }

        if (System.currentTimeMillis() > otpData.getExpireTime()) {
            loginOtpStorage.remove(key);
            throw new RuntimeException("Mã OTP đã hết hiệu lực. Vui lòng đăng nhập lại để nhận mã mới.");
        }

        if (!otpData.getOtp().equals(otp)) {
            otpData.increaseAttempt();
            if (otpData.getAttempts() >= 5) {
                loginOtpStorage.remove(key);
                throw new RuntimeException("Bạn đã nhập sai mã OTP quá 5 lần. Yêu cầu đăng nhập bị hủy.");
            }
            throw new RuntimeException("Mã OTP không chính xác. Bạn còn " + (5 - otpData.getAttempts()) + " lần thử.");
        }

        // OTP đúng -> Dọn dẹp OTP đã dùng
        loginOtpStorage.remove(key);

        User user = userRepository.findByUsername(usernameOrEmail.trim())
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(u -> key.equalsIgnoreCase(u.getEmail()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản")));

        loginOtpStorage.remove(user.getUsername().toLowerCase());
        if (user.getEmail() != null) {
            loginOtpStorage.remove(user.getEmail().toLowerCase());
        }

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
        return userRepository.findByUsername(logoutRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
    }

    public List<Appoinment> getAppoinments(GetAppointmentResponse getAppointmentResponse) {
        log.info("Đang tải danh sách lịch hẹn khám bệnh: {}", getAppointmentResponse.getUsername());
        if (getAppointmentResponse.getUsername() != null && !getAppointmentResponse.getUsername().isBlank()) {
            Optional<User> userOpt = userRepository.findByUsername(getAppointmentResponse.getUsername());
            if (userOpt.isPresent()) {
                Long userId = getAppointmentResponse.getId() != null ? getAppointmentResponse.getId() : userOpt.get().getUser_id();
                List<Appoinment> list = appoinmentRepository.findByUser_Id(userId);
                if (list.isEmpty()) {
                    list = appoinmentRepository.findByPatientName(getAppointmentResponse.getUsername());
                }
                return list;
            }
            return appoinmentRepository.findByPatientName(getAppointmentResponse.getUsername());
        }

        if (getAppointmentResponse.getId() != null) {
            return appoinmentRepository.findByUser_Id(getAppointmentResponse.getId());
        }

        return appoinmentRepository.findAll();
    }

    public Appoinment booking(BookingRequest bookingRequest) {
        log.info("User {} đang đặt lịch với chuyên khoa: {}", bookingRequest.getUsername(), bookingRequest.getSpecialty());
        User user = null;
        if (bookingRequest.getUsername() != null && !bookingRequest.getUsername().isBlank()) {
            user = userRepository.findByUsername(bookingRequest.getUsername()).orElse(null);
        }

        DoctorSlot doctorSlot = null;
        if (bookingRequest.getSlotId() != null) {
            doctorSlot = doctorSlotRepository.findByDoctorSlot_Id(bookingRequest.getSlotId()).orElse(null);
            if (doctorSlot != null) {
                doctorSlot.setBook(true);
                doctorSlotRepository.save(doctorSlot);
            }
        }

        Doctor doctor = null;
        if (doctorSlot != null && doctorSlot.getDoctor() != null) {
            doctor = doctorSlot.getDoctor();
        } else if (bookingRequest.getDoctorId() != null) {
            doctor = doctorRepository.findById(bookingRequest.getDoctorId()).orElse(null);
        } else if (bookingRequest.getSpecialty() != null && !bookingRequest.getSpecialty().isBlank()) {
            doctor = doctorRepository.findBySpecialty(bookingRequest.getSpecialty()).orElse(null);
        }

        java.time.LocalTime startTime = null;
        if (doctorSlot != null && doctorSlot.getStartTime() != null) {
            startTime = doctorSlot.getStartTime();
        } else if (bookingRequest.getStartTime() != null && !bookingRequest.getStartTime().isBlank()) {
            try {
                String st = bookingRequest.getStartTime();
                if (st.contains(" - ")) {
                    st = st.split(" - ")[0].trim();
                }
                if (st.length() == 5) st += ":00";
                startTime = java.time.LocalTime.parse(st);
            } catch (Exception ignored) {}
        }

        java.time.LocalDate date = bookingRequest.getDate();
        if (date == null && doctorSlot != null) {
            date = doctorSlot.getWorkDate() != null ? doctorSlot.getWorkDate() : doctorSlot.getDate();
        }
        if (date == null) {
            date = java.time.LocalDate.now();
        }

        String patientName = bookingRequest.getPatientName();
        if (patientName == null || patientName.isBlank()) {
            patientName = (user != null && user.getFull_name() != null) ? user.getFull_name()
                    : (user != null ? user.getUsername() : bookingRequest.getUsername());
        }

        String email = bookingRequest.getEmail();
        if (email == null || email.isBlank()) {
            email = user != null ? user.getEmail() : null;
        }

        Integer phone = null;
        if (bookingRequest.getPhone() != null && !bookingRequest.getPhone().isBlank()) {
            try {
                phone = Integer.parseInt(bookingRequest.getPhone().replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {}
        } else if (user != null) {
            phone = user.getPhone();
        }

        Appoinment appoinment = Appoinment.builder()
                .user(user)
                .doctor(doctor)
                .doctorSlot(doctorSlot)
                .patientName(patientName)
                .patient_id(user != null ? user.getUser_id() : null)
                .doctorUsername(doctor != null ? doctor.getUsername() : null)
                .email(email)
                .phone(phone)
                .date(date)
                .startTime(startTime)
                .reason(bookingRequest.getReason())
                .status("CONFIRMED")
                .active(true)
                .build();

        return appoinmentRepository.save(appoinment);
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
