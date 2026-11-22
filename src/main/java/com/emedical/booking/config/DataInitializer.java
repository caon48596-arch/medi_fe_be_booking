package com.emedical.booking.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.emedical.booking.repository.AppoinmentRepository;
import com.emedical.booking.repository.DoctorRepository;
import com.emedical.booking.repository.DoctorSlotRepository;
import com.emedical.booking.repository.Medical_recordsRepository;
import com.emedical.booking.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorSlotRepository doctorSlotRepository;
    private final AppoinmentRepository appoinmentRepository;
    private final Medical_recordsRepository medicalRecordsRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Bắt đầu kiểm tra và dọn dẹp toàn bộ dữ liệu mẫu/ảo trong cơ sở dữ liệu...");

        try {
            // 1. Dọn dẹp dữ liệu lịch hẹn ảo cũ (nếu có từ trước)
            appoinmentRepository.findAll().forEach(app -> {
                String patient = app.getPatientName();
                String reason = app.getReason();
                if ("tranminhquan".equalsIgnoreCase(patient)
                        || "Trần Minh Quân".equalsIgnoreCase(patient)
                        || (reason != null && (
                            reason.contains("Khám định kỳ huyết áp")
                            || reason.contains("Tư vấn liệu trình dị ứng")
                            || reason.contains("Khám Thần Kinh")))) {
                    log.info("Xóa lịch hẹn mẫu: ID #{}", app.getAppoinments_id());
                    appoinmentRepository.delete(app);
                }
            });

            // 2. Dọn dẹp bệnh án ảo cũ (nếu có từ trước)
            medicalRecordsRepository.findAll().forEach(rec -> {
                String patient = rec.getPatientName();
                String diag = rec.getDiagnosis();
                if ("Trần Minh Quân".equalsIgnoreCase(patient)
                        || "tranminhquan".equalsIgnoreCase(patient)
                        || (diag != null && (
                            diag.contains("Tăng huyết áp vô căn")
                            || diag.contains("Rối loạn lipid máu")))) {
                    log.info("Xóa hồ sơ bệnh án mẫu: ID #{}", rec.getMedical_id());
                    medicalRecordsRepository.delete(rec);
                }
            });

            // 3. Dọn dẹp tài khoản bệnh nhân ảo cũ (tranminhquan)
            userRepository.findByUsername("tranminhquan").ifPresent(user -> {
                log.info("Xóa tài khoản bệnh nhân mẫu: tranminhquan");
                userRepository.delete(user);
            });

            // 4. Dọn dẹp các bác sĩ ảo mặc định và các slots ảo kèm theo nếu không có lịch hẹn thật
            List<String> mockDoctors = List.of("bacsi_an", "bacsi_mai", "bacsi_tung");
            for (String mockDoc : mockDoctors) {
                doctorRepository.findByUsername(mockDoc).ifPresent(doc -> {
                    doctorSlotRepository.findAll().forEach(slot -> {
                        if (slot.getDoctor() != null && mockDoc.equals(slot.getDoctor().getUsername())) {
                            doctorSlotRepository.delete(slot);
                        }
                    });
                    log.info("Xóa tài khoản bác sĩ mẫu: {}", mockDoc);
                    doctorRepository.delete(doc);
                });
            }

            log.info("Hoàn tất: Cơ sở dữ liệu đã sạch 100% dữ liệu ảo. Hệ thống sẵn sàng nhận dữ liệu thật từ người dùng.");
        } catch (Exception e) {
            log.warn("Lưu ý khi dọn dẹp dữ liệu ảo: {}", e.getMessage());
        }
    }
}
