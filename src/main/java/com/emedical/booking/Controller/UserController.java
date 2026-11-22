package com.emedical.booking.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.emedical.booking.dto.ResponseDTO.ResponseDTO;
import com.emedical.booking.dto.UserDTO.BookingRequest;
import com.emedical.booking.dto.UserDTO.CreateUserResponse;
import com.emedical.booking.dto.UserDTO.GetAppointmentResponse;
import com.emedical.booking.dto.UserDTO.LoginRequest;
import com.emedical.booking.dto.UserDTO.LoginResponse;
import com.emedical.booking.dto.UserDTO.LogoutRequest;
import com.emedical.booking.dto.UserDTO.LogoutResponse;
import com.emedical.booking.dto.UserDTO.RegisterRequest;
import com.emedical.booking.dto.UserDTO.UpdateUserRequest;
import com.emedical.booking.dto.UserDTO.VerifyOtpRequest;
import com.emedical.booking.dto.UserDTO.deleteUserRequest;
import com.emedical.booking.dto.UserDTO.deleteUserResponse;
import com.emedical.booking.dto.UserDTO.updateUserResponse;
import com.emedical.booking.entity.Appoinment;
import com.emedical.booking.entity.User;
import com.emedical.booking.security.CustomUserDetailsService;
import com.emedical.booking.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.context.SecurityContextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;
    private final SecurityContextRepository securityContextRepository;

    /**
     * 1. Đăng ký tài khoản bệnh nhân nội bộ:
     * Lưu thẳng vào CSDL, không bắt buộc nhập OTP khi đăng ký.
     */
    @PostMapping("/register")
    public ResponseEntity<ResponseDTO<CreateUserResponse>> createUser(@RequestBody RegisterRequest registerRequest) {
        try {
            User user = userService.register(registerRequest);
            CreateUserResponse createUserResponse = new CreateUserResponse(
                    user.getUser_id(),
                    user.getUsername(),
                    user.getEmail(),
                    null);

            ResponseDTO<CreateUserResponse> responseDTO = new ResponseDTO<>(
                    true,
                    "Đăng ký tài khoản bệnh nhân thành công! Vui lòng tiến hành đăng nhập.",
                    createUserResponse);

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.warn("Đăng ký người dùng thất bại: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    /**
     * 2. Đăng nhập bệnh nhân qua 2 bước bảo mật:
     * - Bước 1: Gửi username + password -> Kiểm tra thông tin, tạo mã OTP 6 chữ số gửi qua Email.
     * - Bước 2: Gửi kèm mã OTP -> Xác thực OTP, thiết lập Security Session (Redis Session).
     */
    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<LoginResponse>> loginUser(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            // Bước 1: Nếu chưa gửi kèm OTP -> Kiểm tra username & password, phát sinh mã OTP
            if (loginRequest.getOtp() == null) {
                User user = userService.validateCredentialsAndCreateOtp(loginRequest);
                Integer otp = userService.getLoginOtp(user.getUsername());

                String maskedEmail = maskEmail(user.getEmail());
                String message = "Thông tin đăng nhập chính xác! Mã OTP xác nhận đăng nhập đã được gửi tới "
                        + maskedEmail + (otp != null ? " (Mã OTP thử nghiệm: " + otp + ")" : "");

                LoginResponse pendingResponse = new LoginResponse(user.getUsername(), true, user.getEmail(), otp);
                return ResponseEntity.ok(new ResponseDTO<>(true, message, pendingResponse));
            }

            // Bước 2: Đã có mã OTP -> Xác thực OTP và thiết lập phiên đăng nhập
            User user = userService.verifyLoginOtp(loginRequest.getUsername(), loginRequest.getOtp());

            UserDetails userDetails = customUserDetailsService.loadUserOnlyByUsername(user.getUsername());
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities());

            HttpSession session = request.getSession(false);
            if (session != null) {
                request.changeSessionId();
            } else {
                session = request.getSession(true);
            }

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            List<String> roles = authentication.getAuthorities()
                    .stream()
                    .map(authority -> authority.getAuthority())
                    .toList();

            LoginResponse loginResponse = new LoginResponse(user.getUsername(), roles);
            return ResponseEntity.ok(new ResponseDTO<>(true, "Xác nhận OTP thành công! Đăng nhập thành công.", loginResponse));

        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(401).body(new ResponseDTO<>(false, "Username hoặc password không đúng", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    /**
     * API xác thực OTP đăng nhập dành riêng cho client gọi riêng
     */
    @PostMapping("/verify-login-otp")
    public ResponseEntity<ResponseDTO<LoginResponse>> verifyLoginOtp(
            @RequestBody VerifyOtpRequest verifyRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        LoginRequest loginRequest = new LoginRequest();
        String identifier = verifyRequest.getUsername() != null && !verifyRequest.getUsername().isBlank()
                ? verifyRequest.getUsername()
                : verifyRequest.getEmail();
        loginRequest.setUsername(identifier);
        loginRequest.setOtp(verifyRequest.getOtp());
        return loginUser(loginRequest, request, response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ResponseDTO<String>> verifyOtp(@RequestBody VerifyOtpRequest verifyOtpRequest) {
        try {
            userService.verifyOtp(verifyOtpRequest);
            return ResponseEntity.ok(new ResponseDTO<>(true, "Xác thực OTP thành công", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseDTO<LogoutResponse>> logoutUser(
            @RequestBody(required = false) LogoutRequest logoutRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            if (logoutRequest != null && logoutRequest.getUsername() != null) {
                userService.logout(logoutRequest);
            }

            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            securityContextRepository.saveContext(SecurityContextHolder.createEmptyContext(), request, response);

            ResponseDTO<LogoutResponse> responseDTO = new ResponseDTO<>(true, "Đăng xuất thành công", null);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/get")
    public ResponseEntity<ResponseDTO<List<Appoinment>>> getAppoinmentUser(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String username,
            Authentication authentication) {
        try {
            String currentUsername = (username != null) ? username
                    : (authentication != null ? authentication.getName() : null);
            GetAppointmentResponse getAppointmentResponse = new GetAppointmentResponse(currentUsername, id);
            List<Appoinment> appoinments = userService.getAppoinments(getAppointmentResponse);

            ResponseDTO<List<Appoinment>> responseDTO = new ResponseDTO<>(true, "Lấy danh sách lịch hẹn thành công",
                    appoinments);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/booking")
    public ResponseEntity<ResponseDTO<Appoinment>> bookingUser(@RequestBody BookingRequest bookingRequest) {
        try {
            Appoinment appoinment = userService.booking(bookingRequest);
            ResponseDTO<Appoinment> responseDTO = new ResponseDTO<>(true, "Đặt lịch khám thành công", appoinment);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<ResponseDTO<User>> getUserProfile(
            @RequestParam(required = false) String username,
            Authentication authentication) {
        try {
            String currentUsername = (username != null && !username.isBlank()) ? username
                    : (authentication != null ? authentication.getName() : null);
            if (currentUsername == null) {
                return ResponseEntity.badRequest().body(new ResponseDTO<>(false, "Username is required", null));
            }
            User user = userService.getByUsername(currentUsername);
            return ResponseEntity.ok(new ResponseDTO<>(true, "Lấy thông tin bệnh nhân thành công", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ResponseDTO<List<User>>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(new ResponseDTO<>(true, "Get all users success", users));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/getuser/{id}")
    public ResponseEntity<ResponseDTO<User>> getUser(@PathVariable("id") Long id) {
        try {
            User user = userService.get(id);
            ResponseDTO<User> responseDTO = new ResponseDTO<>(true, "get user id is success", user);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @RequestMapping(value = "/updateUser/{id}", method = { org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.PUT })
    public ResponseEntity<ResponseDTO<updateUserResponse>> updateUser(
            @PathVariable("id") Long id,
            @RequestBody UpdateUserRequest updateUserRequest) {
        try {
            updateUserRequest.setUserId(id);
            User user = userService.updateUser(updateUserRequest);
            updateUserResponse updateUserResponse = new updateUserResponse(user.getUser_id());
            ResponseDTO<updateUserResponse> responseDTO = new ResponseDTO<>(true, "update user is success",
                    updateUserResponse);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @RequestMapping(value = "/deleteUser/{id}", method = { org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.DELETE })
    public ResponseEntity<ResponseDTO<deleteUserResponse>> deleteUser(@PathVariable("id") Long id) {
        try {
            deleteUserRequest deleteUserRequest = new deleteUserRequest(id);
            User user = userService.deleteUser(deleteUserRequest);
            deleteUserResponse deleteUserResponse = new deleteUserResponse(user.getUser_id());
            ResponseDTO<deleteUserResponse> responseDTO = new ResponseDTO<>(true, "delete user is success",
                    deleteUserResponse);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
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
