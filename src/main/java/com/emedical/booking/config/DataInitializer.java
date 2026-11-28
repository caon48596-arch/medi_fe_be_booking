package com.emedical.booking.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.emedical.booking.entity.DoctorSlot;
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

    private static final List<String> MOCK_DOCTOR_USERNAMES = List.of("bacsi_an", "bacsi_mai", "bacsi_tung");

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorSlotRepository doctorSlotRepository;
    private final AppoinmentRepository appoinmentRepository;
    private final Medical_recordsRepository medicalRecordsRepository;

    @Override
    public void run(String... args) {
        log.info("Bắt đầu kiểm tra và dọn dẹp toàn bộ dữ liệu mẫu/ảo trong cơ sở dữ liệu...");

        try {
            appoinmentRepository.findAll().forEach(app -> {
                String patient = app.getPatientName();
                String reason = app.getReason();
                if ("tranminhquan".equalsIgnoreCase(patient)
                        || "Trần Minh Quân".equalsIgnoreCase(patient)
                        || (reason != null && (reason.contains("Khám định kỳ huyết áp")
                                || reason.contains("Tư vấn liệu trình dị ứng")
                                || reason.contains("Khám Thần Kinh")))) {
                    log.info("Xóa lịch hẹn mẫu: ID #{}", app.getAppoinments_id());
                    appoinmentRepository.delete(app);
                }
            });

            medicalRecordsRepository.findAll().forEach(rec -> {
                String patient = rec.getPatientName();
                String diag = rec.getDiagnosis();
                if ("Trần Minh Quân".equalsIgnoreCase(patient)
                        || "tranminhquan".equalsIgnoreCase(patient)
                        || (diag != null && (diag.contains("Tăng huyết áp vô căn")
                                || diag.contains("Rối loạn lipid máu")))) {
                    log.info("Xóa hồ sơ bệnh án mẫu: ID #{}", rec.getMedical_id());
                    medicalRecordsRepository.delete(rec);
                }
            });

            userRepository.findByUsername("tranminhquan").ifPresent(user -> {
                log.info("Xóa tài khoản bệnh nhân mẫu: tranminhquan");
                userRepository.delete(user);
            });

            for (String mockDoc : MOCK_DOCTOR_USERNAMES) {
                doctorRepository.findByUsername(mockDoc).ifPresent(doc -> {
                    List<DoctorSlot> slots = doctorSlotRepository.findByDoctorUsername(mockDoc);
                    if (!slots.isEmpty()) {
                        doctorSlotRepository.deleteAll(slots);
                    }
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
