package com.emedical.booking.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.emedical.booking.dto.AppoinmentDTO.AppoinmentRequestDTO;
import com.emedical.booking.dto.UserDTO.BookingRequest;
import com.emedical.booking.entity.Appoinment;
import com.emedical.booking.entity.AppointmentStatus;
import com.emedical.booking.entity.Doctor;
import com.emedical.booking.entity.DoctorSlot;
import com.emedical.booking.entity.User;
import com.emedical.booking.repository.AppoinmentRepository;
import com.emedical.booking.repository.DoctorRepository;
import com.emedical.booking.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class AppoinmentService {

    private final AppoinmentRepository appoinmentRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorSlotsService doctorSlotsService;

    public Appoinment createAppointment(AppoinmentRequestDTO appoinmentRequestDTO) {
        log.info("Creating appointment for patient ID: {}", appoinmentRequestDTO.getPatienId());
        User user = userRepository.findById(appoinmentRequestDTO.getPatienId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bệnh nhân"));

        DoctorSlot doctorSlot = doctorSlotsService.occupySlot(appoinmentRequestDTO.getSlotId());
        Doctor doctor = doctorSlot.getDoctor();

        Appoinment appoinment = Appoinment.builder()
                .user(user)
                .doctor(doctor)
                .doctorSlot(doctorSlot)
                .patientName(user.getUsername())
                .patient_id(user.getUser_id())
                .doctorUsername(doctor != null ? doctor.getUsername() : null)
                .email(user.getEmail())
                .phone(user.getPhone())
                .date(resolveSlotDate(doctorSlot))
                .startTime(doctorSlot.getStartTime())
                .reason(appoinmentRequestDTO.getReason())
                .status(AppointmentStatus.CONFIRMED.name())
                .active(true)
                .build();

        return appoinmentRepository.save(appoinment);
    }

    public Appoinment bookAppointment(BookingRequest bookingRequest) {
        log.info("User {} đang đặt lịch với chuyên khoa: {}", bookingRequest.getUsername(), bookingRequest.getSpecialty());

        User user = null;
        if (bookingRequest.getUsername() != null && !bookingRequest.getUsername().isBlank()) {
            user = userRepository.findByUsername(bookingRequest.getUsername().trim()).orElse(null);
        }

        DoctorSlot doctorSlot = null;
        if (bookingRequest.getSlotId() != null) {
            doctorSlot = doctorSlotsService.occupySlot(bookingRequest.getSlotId());
        }

        Doctor doctor = resolveDoctorForBooking(bookingRequest, doctorSlot);
        LocalTime startTime = resolveStartTime(bookingRequest, doctorSlot);
        LocalDate date = resolveBookingDate(bookingRequest, doctorSlot);
        String patientName = resolvePatientName(bookingRequest, user);
        String email = resolveEmail(bookingRequest, user);
        Integer phone = resolvePhone(bookingRequest, user);

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
                .status(AppointmentStatus.CONFIRMED.name())
                .active(true)
                .build();

        return appoinmentRepository.save(appoinment);
    }

    public List<Appoinment> readAppointments(Long patientId) {
        if (patientId == null) {
            log.info("Reading all appointments");
            return appoinmentRepository.findAll();
        }
        log.info("Reading appointments for patient ID: {}", patientId);
        return userRepository.findById(patientId)
                .map(u -> appoinmentRepository.findByUser_Id(u.getUser_id()))
                .orElseGet(() -> appoinmentRepository.findByUser_Id(patientId));
    }

    public List<Appoinment> readAppoinments(AppoinmentRequestDTO appoinmentRequestDTO) {
        return readAppointments(appoinmentRequestDTO != null ? appoinmentRequestDTO.getPatienId() : null);
    }

    public List<Appoinment> getAppointmentsForUser(String username, Long userId) {
        log.info("Đang tải danh sách lịch hẹn khám bệnh: {}", username);
        if (username != null && !username.isBlank()) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                Long targetUserId = userId != null ? userId : userOpt.get().getUser_id();
                List<Appoinment> list = appoinmentRepository.findByUser_Id(targetUserId);
                if (list.isEmpty()) {
                    list = appoinmentRepository.findByPatientName(username);
                }
                return list;
            }
            return appoinmentRepository.findByPatientName(username);
        }

        if (userId != null) {
            return appoinmentRepository.findByUser_Id(userId);
        }

        return appoinmentRepository.findAll();
    }

    public List<Appoinment> getAppointmentsByDoctor(Long doctorId) {
        log.info("Đang tải dữ liệu appointment của bác sĩ ID {}", doctorId);
        if (doctorId == null) {
            return appoinmentRepository.findAll();
        }
        List<Appoinment> list = appoinmentRepository.findByDoctorId(doctorId);
        if (list.isEmpty()) {
            doctorRepository.findById(doctorId)
                    .ifPresent(d -> list.addAll(appoinmentRepository.findByDoctorUsername(d.getUsername())));
        }
        return list;
    }

    public Appoinment updateAppoinment(AppoinmentRequestDTO appoinmentRequestDTO) {
        log.info("Updating appointment ID: {}", appoinmentRequestDTO.getAppoinmentId());
        if (appoinmentRequestDTO.getAppoinmentId() == null) {
            throw new RuntimeException("Cần cung cấp appoinmentId để cập nhật lịch hẹn");
        }

        Appoinment existingAppointment = appoinmentRepository.findById(appoinmentRequestDTO.getAppoinmentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));

        if (appoinmentRequestDTO.getReason() != null) {
            existingAppointment.setReason(appoinmentRequestDTO.getReason());
        }
        if (appoinmentRequestDTO.getStatus() != null) {
            existingAppointment.setStatus(appoinmentRequestDTO.getStatus());
        }
        if (appoinmentRequestDTO.getWorkDate() != null) {
            existingAppointment.setDate(appoinmentRequestDTO.getWorkDate());
        }
        if (appoinmentRequestDTO.getStartTime() != null) {
            existingAppointment.setStartTime(appoinmentRequestDTO.getStartTime());
        } else if (appoinmentRequestDTO.getStarTime() != null) {
            existingAppointment.setStartTime(appoinmentRequestDTO.getStarTime());
        }

        if (appoinmentRequestDTO.getSlotId() != null
                && !appoinmentRequestDTO.getSlotId().equals(existingAppointment.getDoctorSlot_id())) {
            DoctorSlot newSlot = doctorSlotsService.occupySlot(appoinmentRequestDTO.getSlotId());
            doctorSlotsService.releaseSlot(existingAppointment.getDoctorSlot());

            existingAppointment.setDoctorSlot(newSlot);
            existingAppointment.setDoctor(newSlot.getDoctor());
            existingAppointment.setDoctorUsername(
                    newSlot.getDoctor() != null ? newSlot.getDoctor().getUsername() : existingAppointment.getDoctorUsername());
            existingAppointment.setDate(resolveSlotDate(newSlot));
            existingAppointment.setStartTime(newSlot.getStartTime());
        }

        return appoinmentRepository.save(existingAppointment);
    }

    public Appoinment cancelAppointment(Long appointmentId) {
        log.info("Cancelling appointment ID: {}", appointmentId);
        Appoinment appointment = appoinmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn ID: " + appointmentId));

        appointment.setStatus(AppointmentStatus.CANCELLED.name());
        doctorSlotsService.releaseSlot(appointment.getDoctorSlot());

        return appoinmentRepository.save(appointment);
    }

    public Appoinment deleteAppointment(Long appointmentId) {
        log.info("Deleting appointment ID: {}", appointmentId);
        Appoinment appointment = appoinmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn ID: " + appointmentId));

        doctorSlotsService.releaseSlot(appointment.getDoctorSlot());
        appoinmentRepository.delete(appointment);
        return appointment;
    }

    public List<Appoinment> rejectAndReassignAppointment(Long doctorId, Long appointmentId) {
        log.info("Bác sĩ ID {} yêu cầu từ chối và chuyển giao lịch hẹn ID: {}", doctorId, appointmentId);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bác sĩ ID: " + doctorId));

        Appoinment appointment = appoinmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn ID: " + appointmentId));

        List<DoctorSlot> replacementSlots = doctorSlotsService.findAvailableReplacementSlots(
                doctorId,
                doctor.getSpecialty(),
                appointment.getDate());

        if (replacementSlots.isEmpty()) {
            log.warn("Không tìm thấy bác sĩ thay thế cùng chuyên khoa {} trong ngày {}",
                    doctor.getSpecialty(), appointment.getDate());
            doctorSlotsService.releaseSlot(appointment.getDoctorSlot());
            appointment.setStatus(AppointmentStatus.REJECTED_NO_DOCTOR_AVAILABLE.name());
            return List.of(appoinmentRepository.save(appointment));
        }

        DoctorSlot newSlot = replacementSlots.get(0);
        Doctor newDoctor = newSlot.getDoctor();

        doctorSlotsService.releaseSlot(appointment.getDoctorSlot());
        doctorSlotsService.occupySpecificSlot(newSlot);

        appointment.setDoctor(newDoctor);
        appointment.setDoctorUsername(newDoctor != null ? newDoctor.getUsername() : null);
        appointment.setDoctorSlot(newSlot);
        if (newSlot.getStartTime() != null) {
            appointment.setStartTime(newSlot.getStartTime());
        }
        appointment.setStatus(AppointmentStatus.REASSIGNED_PENDING.name());

        Appoinment savedAppointment = appoinmentRepository.save(appointment);
        log.info("Lịch hẹn ID {} đã được chuyển thành công sang Bác sĩ {} (ID: {})",
                appointmentId,
                newDoctor != null ? newDoctor.getUsername() : "N/A",
                newDoctor != null ? newDoctor.getDoctor_id() : null);

        return List.of(savedAppointment);
    }

    private Doctor resolveDoctorForBooking(BookingRequest request, DoctorSlot slot) {
        if (slot != null && slot.getDoctor() != null) {
            return slot.getDoctor();
        }
        if (request.getDoctorId() != null) {
            return doctorRepository.findById(request.getDoctorId()).orElse(null);
        }
        if (request.getSpecialty() != null && !request.getSpecialty().isBlank()) {
            return doctorRepository.findBySpecialty(request.getSpecialty()).orElse(null);
        }
        return null;
    }

    private LocalTime resolveStartTime(BookingRequest request, DoctorSlot slot) {
        if (slot != null && slot.getStartTime() != null) {
            return slot.getStartTime();
        }
        if (request.getStartTime() != null && !request.getStartTime().isBlank()) {
            try {
                String st = request.getStartTime();
                if (st.contains(" - ")) {
                    st = st.split(" - ")[0].trim();
                }
                if (st.length() == 5) {
                    st += ":00";
                }
                return LocalTime.parse(st);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private LocalDate resolveBookingDate(BookingRequest request, DoctorSlot slot) {
        if (request.getDate() != null) {
            return request.getDate();
        }
        if (slot != null) {
            LocalDate slotDate = resolveSlotDate(slot);
            if (slotDate != null) {
                return slotDate;
            }
        }
        return LocalDate.now();
    }

    private LocalDate resolveSlotDate(DoctorSlot slot) {
        return slot.getWorkDate() != null ? slot.getWorkDate() : slot.getDate();
    }

    private String resolvePatientName(BookingRequest request, User user) {
        if (request.getPatientName() != null && !request.getPatientName().isBlank()) {
            return request.getPatientName();
        }
        if (user != null) {
            return user.getFull_name() != null ? user.getFull_name() : user.getUsername();
        }
        return request.getUsername();
    }

    private String resolveEmail(BookingRequest request, User user) {
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            return request.getEmail();
        }
        return user != null ? user.getEmail() : null;
    }

    private Integer resolvePhone(BookingRequest request, User user) {
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            try {
                return Integer.parseInt(request.getPhone().replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {
                return user != null ? user.getPhone() : null;
            }
        }
        return user != null ? user.getPhone() : null;
    }
}
