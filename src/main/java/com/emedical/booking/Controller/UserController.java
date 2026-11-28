package com.emedical.booking.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.emedical.booking.dto.ResponseDTO.ResponseDTO;
import com.emedical.booking.dto.UserDTO.BookingRequest;
import com.emedical.booking.dto.UserDTO.CreateUserResponse;
import com.emedical.booking.dto.UserDTO.LoginRequest;
import com.emedical.booking.dto.UserDTO.LoginResponse;
import com.emedical.booking.dto.UserDTO.LogoutRequest;
import com.emedical.booking.dto.UserDTO.LogoutResponse;
import com.emedical.booking.dto.UserDTO.RegisterRequest;
import com.emedical.booking.dto.UserDTO.UpdateUserRequest;
import com.emedical.booking.dto.UserDTO.VerifyOtpRequest;
import com.emedical.booking.dto.UserDTO.deleteUserResponse;
import com.emedical.booking.dto.UserDTO.updateUserResponse;
import com.emedical.booking.entity.Appoinment;
import com.emedical.booking.entity.User;
import com.emedical.booking.security.CustomUserDetailsService;
import com.emedical.booking.security.SessionAuthenticationService;
import com.emedical.booking.service.AppoinmentService;
import com.emedical.booking.service.OtpService;
import com.emedical.booking.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AppoinmentService appoinmentService;
    private final OtpService otpService;
    private final CustomUserDetailsService customUserDetailsService;
    private final SessionAuthenticationService sessionAuthenticationService;

    @PostMapping("/register")
    public ResponseEntity<ResponseDTO<CreateUserResponse>> createUser(@RequestBody RegisterRequest registerRequest) {
        User user = userService.register(registerRequest);
        CreateUserResponse createUserResponse = new CreateUserResponse(
                user.getUser_id(),
                user.getUsername(),
                user.getEmail(),
                null);

        return ResponseEntity.ok(new ResponseDTO<>(
                true,
                "Đăng ký tài khoản bệnh nhân thành công! Vui lòng tiến hành đăng nhập.",
                createUserResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<LoginResponse>> loginUser(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (loginRequest.getOtp() == null) {
            User user = userService.validateCredentialsAndCreateOtp(loginRequest);
            Integer otp = userService.getLoginOtp(user.getUsername());

            String maskedEmail = otpService.maskEmail(user.getEmail());
            String message = "Thông tin đăng nhập chính xác! Mã OTP xác nhận đăng nhập đã được gửi tới "
                    + maskedEmail + (otp != null ? " (Mã OTP thử nghiệm: " + otp + ")" : "");

            LoginResponse pendingResponse = new LoginResponse(user.getUsername(), true, user.getEmail(), otp);
            return ResponseEntity.ok(new ResponseDTO<>(true, message, pendingResponse));
        }

        User user = userService.verifyLoginOtp(loginRequest.getUsername(), loginRequest.getOtp());
        UserDetails userDetails = customUserDetailsService.loadUserOnlyByUsername(user.getUsername());
        List<String> roles = sessionAuthenticationService.establishSession(userDetails, request, response);

        LoginResponse loginResponse = new LoginResponse(user.getUsername(), roles);
        return ResponseEntity.ok(
                new ResponseDTO<>(true, "Xác nhận OTP thành công! Đăng nhập thành công.", loginResponse));
    }

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
        userService.verifyOtp(verifyOtpRequest);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Xác thực OTP thành công", "SUCCESS"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseDTO<LogoutResponse>> logoutUser(
            @RequestBody(required = false) LogoutRequest logoutRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (logoutRequest != null && logoutRequest.getUsername() != null) {
            userService.logout(logoutRequest.getUsername());
        }
        sessionAuthenticationService.clearSession(request, response);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Đăng xuất thành công", null));
    }

    @GetMapping("/get")
    public ResponseEntity<ResponseDTO<List<Appoinment>>> getAppoinmentUser(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String username,
            Authentication authentication) {
        String currentUsername = (username != null) ? username
                : (authentication != null ? authentication.getName() : null);
        List<Appoinment> appoinments = appoinmentService.getAppointmentsForUser(currentUsername, id);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Lấy danh sách lịch hẹn thành công", appoinments));
    }

    @PostMapping("/booking")
    public ResponseEntity<ResponseDTO<Appoinment>> bookingUser(@RequestBody BookingRequest bookingRequest) {
        Appoinment appoinment = appoinmentService.bookAppointment(bookingRequest);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Đặt lịch khám thành công", appoinment));
    }

    @GetMapping("/profile")
    public ResponseEntity<ResponseDTO<User>> getUserProfile(
            @RequestParam(required = false) String username,
            Authentication authentication) {
        String currentUsername = (username != null && !username.isBlank()) ? username
                : (authentication != null ? authentication.getName() : null);
        if (currentUsername == null) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, "Username is required", null));
        }
        User user = userService.getByUsername(currentUsername);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Lấy thông tin bệnh nhân thành công", user));
    }

    @GetMapping("/all")
    public ResponseEntity<ResponseDTO<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(new ResponseDTO<>(true, "Get all users success", users));
    }

    @GetMapping("/getuser/{id}")
    public ResponseEntity<ResponseDTO<User>> getUser(@PathVariable("id") Long id) {
        User user = userService.get(id);
        return ResponseEntity.ok(new ResponseDTO<>(true, "get user id is success", user));
    }

    @RequestMapping(value = "/updateUser/{id}", method = { RequestMethod.POST, RequestMethod.PUT })
    public ResponseEntity<ResponseDTO<updateUserResponse>> updateUser(
            @PathVariable("id") Long id,
            @RequestBody UpdateUserRequest updateUserRequest) {
        updateUserRequest.setUserId(id);
        User user = userService.updateUser(updateUserRequest);
        return ResponseEntity.ok(
                new ResponseDTO<>(true, "update user is success", new updateUserResponse(user.getUser_id())));
    }

    @RequestMapping(value = "/deleteUser/{id}", method = { RequestMethod.POST, RequestMethod.DELETE })
    public ResponseEntity<ResponseDTO<deleteUserResponse>> deleteUser(@PathVariable("id") Long id) {
        User user = userService.deleteUser(id);
        return ResponseEntity.ok(
                new ResponseDTO<>(true, "delete user is success", new deleteUserResponse(user.getUser_id())));
    }
}
