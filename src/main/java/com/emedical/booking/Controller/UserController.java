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
import com.emedical.booking.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.context.SecurityContextRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @PostMapping("/register")
    public ResponseEntity<ResponseDTO<CreateUserResponse>> createUser(@RequestBody RegisterRequest registerRequest) {
        try {
            User user = userService.register(registerRequest);
            CreateUserResponse createUserResponse = new CreateUserResponse(
                    user.getUser_id(),
                    user.getUsername(),
                    user.getEmail());

            ResponseDTO<CreateUserResponse> responseDTO = new ResponseDTO<>(true, "create user successfully",
                    createUserResponse);

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
        }
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

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<LoginResponse>> loginUser(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            // 1. Xác thực username và password qua Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            // 2. Tạo hoặc đổi Session ID để chống Session Fixation
            HttpSession session = request.getSession(false);
            if (session != null) {
                request.changeSessionId();
            } else {
                session = request.getSession(true);
            }

            // 3. Tạo SecurityContext và gán authentication
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // 4. Lưu SecurityContext vào Redis Session
            securityContextRepository.saveContext(context, request, response);

            // 5. Lấy danh sách roles
            List<String> roles = authentication.getAuthorities()
                    .stream()
                    .map(authority -> authority.getAuthority())
                    .toList();

            LoginResponse loginResponse = new LoginResponse(authentication.getName(), roles);
            ResponseDTO<LoginResponse> responseDTO = new ResponseDTO<>(true, "login success", loginResponse);

            return ResponseEntity.ok(responseDTO);

        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            ResponseDTO<LoginResponse> responseDTO = new ResponseDTO<>(
                    false,
                    "Username hoặc password không đúng",
                    null);
            return ResponseEntity.status(401).body(responseDTO);
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

            // Hủy Session phía server (Redis)
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            securityContextRepository.saveContext(SecurityContextHolder.createEmptyContext(), request, response);

            ResponseDTO<LogoutResponse> responseDTO = new ResponseDTO<>(true, "logout success", null);
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

            ResponseDTO<List<Appoinment>> responseDTO = new ResponseDTO<>(true, "get appointments success",
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
            ResponseDTO<Appoinment> responseDTO = new ResponseDTO<>(true, "booking is success", appoinment);
            return ResponseEntity.ok(responseDTO);
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
}
