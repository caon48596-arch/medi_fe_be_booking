package com.emedical.booking;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.emedical.booking.dto.DoctorDTO.RegisterDoctorRequest;
import com.emedical.booking.dto.UserDTO.BookingRequest;
import com.emedical.booking.dto.UserDTO.LoginRequest;
import com.emedical.booking.dto.UserDTO.RegisterRequest;
import com.emedical.booking.entity.Appoinment;
import com.emedical.booking.entity.Doctor;
import com.emedical.booking.entity.User;
import com.emedical.booking.repository.AppoinmentRepository;
import com.emedical.booking.repository.DoctorRepository;
import com.emedical.booking.repository.UserRepository;
import com.emedical.booking.service.DoctorService;
import com.emedical.booking.service.UserService;

@SpringBootTest
public class AuthOtpAndHospitalTest {

    @Autowired
    private UserService userService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppoinmentRepository appoinmentRepository;

    @BeforeEach
    void setUp() {
        appoinmentRepository.deleteAll();
        userRepository.deleteAll();
        doctorRepository.deleteAll();
    }

    @Test
    void testPatientRegisterWithoutOtpAndLoginWithOtp() {
        // 1. Đăng ký bệnh nhân thành công trực tiếp, không bắt OTP
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("patient_nguyen");
        registerRequest.setPassword("password123");
        registerRequest.setConfirmPassword("password123");
        registerRequest.setEmail("patient_nguyen@test.com");
        registerRequest.setFullName("Nguyễn Văn Bệnh Nhân");
        registerRequest.setPhone("0987654321");

        User registeredUser = userService.register(registerRequest);
        assertNotNull(registeredUser.getUser_id(), "User ID phải được tạo trong CSDL");
        assertTrue(registeredUser.isActive(), "Tài khoản người dùng phải active");
        assertEquals("patient_nguyen", registeredUser.getUsername());

        // 2. Đăng nhập Bước 1: Nhập đúng username + password -> Tạo mã OTP xác nhận đăng nhập
        LoginRequest loginStep1 = new LoginRequest();
        loginStep1.setUsername("patient_nguyen");
        loginStep1.setPassword("password123");

        User userAfterStep1 = userService.validateCredentialsAndCreateOtp(loginStep1);
        assertEquals("patient_nguyen", userAfterStep1.getUsername());

        Integer otp = userService.getLoginOtp("patient_nguyen");
        assertNotNull(otp, "Hệ thống phải sinh mã OTP đăng nhập");
        assertTrue(otp >= 100000 && otp <= 999999, "Mã OTP phải có 6 chữ số");

        // 3. Đăng nhập Bước 2: Thử nhập sai OTP -> Báo lỗi
        assertThrows(RuntimeException.class, () -> {
            userService.verifyLoginOtp("patient_nguyen", 999999 == otp ? 111111 : 999999);
        }, "Nhập sai OTP phải ném ra lỗi");

        // 4. Đăng nhập Bước 2: Nhập đúng OTP -> Xác thực đăng nhập thành công
        User loggedInUser = userService.verifyLoginOtp("patient_nguyen", otp);
        assertNotNull(loggedInUser);
        assertEquals("patient_nguyen", loggedInUser.getUsername());
    }

    @Test
    void testDoctorRegisterInternalHospitalAndLoginWithOtp() {
        // 1. Đăng ký bác sĩ nội bộ của 1 bệnh viện duy nhất với chuyên khoa nội bộ
        RegisterDoctorRequest docRequest = new RegisterDoctorRequest();
        docRequest.setUsername("bacsi_timmach");
        docRequest.setPassword("doctorPass123");
        docRequest.setConfirmPassword("doctorPass123");
        docRequest.setEmail("bs_timmach@hospital.vn");
        docRequest.setSpecialty("Khoa Tim Mạch");
        docRequest.setRoom_number("Phòng 205 - Khu B");
        docRequest.setPrice(300000);

        Doctor registeredDoc = doctorService.register(docRequest);
        assertNotNull(registeredDoc.getDoctor_id());
        assertTrue(registeredDoc.isActive());
        assertEquals("Khoa Tim Mạch", registeredDoc.getSpecialty());
        assertEquals("Phòng 205 - Khu B", registeredDoc.getRoom_number());

        // 2. Bác sĩ đăng nhập Bước 1: Kiểm tra mật khẩu & sinh mã OTP
        LoginRequest docLoginStep1 = new LoginRequest();
        docLoginStep1.setUsername("bacsi_timmach");
        docLoginStep1.setPassword("doctorPass123");

        Doctor docAfterStep1 = doctorService.validateCredentialsAndCreateOtp(docLoginStep1);
        assertEquals("bacsi_timmach", docAfterStep1.getUsername());

        Integer docOtp = doctorService.getLoginOtp("bacsi_timmach");
        assertNotNull(docOtp, "Phải sinh mã OTP đăng nhập cho bác sĩ");

        // 3. Bác sĩ đăng nhập Bước 2: Nhập đúng mã OTP -> Thành công
        Doctor loggedInDoc = doctorService.verifyLoginOtp("bacsi_timmach", docOtp);
        assertNotNull(loggedInDoc);
        assertEquals("bacsi_timmach", loggedInDoc.getUsername());
    }

    @Test
    void testSingleHospitalAppointmentBooking() {
        // Tạo bác sĩ nội bộ
        RegisterDoctorRequest docRequest = new RegisterDoctorRequest();
        docRequest.setUsername("bacsi_nhi");
        docRequest.setPassword("pass123");
        docRequest.setConfirmPassword("pass123");
        docRequest.setEmail("bs_nhi@hospital.vn");
        docRequest.setSpecialty("Khoa Nhi & Tiêm Chủng");
        Doctor doc = doctorService.register(docRequest);

        // Tạo bệnh nhân
        RegisterRequest userReq = new RegisterRequest();
        userReq.setUsername("me_be_bong");
        userReq.setPassword("pass123");
        userReq.setConfirmPassword("pass123");
        userReq.setEmail("me_bong@example.com");
        userReq.setFullName("Lê Thị Bông");
        User user = userService.register(userReq);

        // Đặt lịch khám nội bộ tại Bệnh viện MediSession
        BookingRequest bookingReq = new BookingRequest();
        bookingReq.setUsername("me_be_bong");
        bookingReq.setDoctorId(doc.getDoctor_id());
        bookingReq.setSpecialty("Khoa Nhi & Tiêm Chủng");
        bookingReq.setDate(LocalDate.now());
        bookingReq.setStartTime("09:00:00");
        bookingReq.setReason("Bé bị sốt nhẹ và cần tiêm mũi nhắc lại");

        Appoinment appointment = userService.booking(bookingReq);
        assertNotNull(appointment.getAppoinments_id());
        assertEquals("CONFIRMED", appointment.getStatus());
        assertEquals("bacsi_nhi", appointment.getDoctor().getUsername());
        assertEquals("me_be_bong", appointment.getUser().getUsername());
    }
}
