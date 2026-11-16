package com.emedical.booking.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.emedical.booking.dto.AppoinmentDTO.AppoinmentRequestDTO;
import com.emedical.booking.entity.Appoinment;
import com.emedical.booking.entity.DoctorSlot;
import com.emedical.booking.entity.User;
import com.emedical.booking.repository.AppoinmentRepository;
import com.emedical.booking.repository.DoctorSlotRepository;
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
    private final DoctorSlotRepository doctorSlotRepository;

    public Appoinment createAppointment(AppoinmentRequestDTO appoinmentRequestDTO) {
        log.info("Creating appointment for patient ID: {}", appoinmentRequestDTO.getPatienId());
        User user = userRepository.findById(appoinmentRequestDTO.getPatienId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bệnh nhân"));

        DoctorSlot doctorSlot = doctorSlotRepository.findByDoctorSlot_Id(appoinmentRequestDTO.getSlotId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy slot khám"));

        if (doctorSlot.isBook()) {
            throw new RuntimeException("Slot khám này đã được người khác đặt trước đó");
        }

        // Đánh dấu slot đã được đặt
        doctorSlot.setBook(true);
        doctorSlotRepository.save(doctorSlot);

        Appoinment appoinment = Appoinment.builder()
                .user(user)
                .doctor(doctorSlot.getDoctor())
                .doctorSlot(doctorSlot)
                .patientName(user.getUsername())
                .patient_id(user.getUser_id())
                .doctorUsername(doctorSlot.getDoctor() != null ? doctorSlot.getDoctor().getUsername() : null)
                .email(user.getEmail())
                .phone(user.getPhone())
                .date(doctorSlot.getWorkDate() != null ? doctorSlot.getWorkDate() : doctorSlot.getDate())
                .startTime(doctorSlot.getStartTime())
                .reason(appoinmentRequestDTO.getReason())
                .status("CONFIRMED")
                .active(true)
                .build();

        return appoinmentRepository.save(appoinment);
    }

    public List<Appoinment> readAppoinments(AppoinmentRequestDTO appoinmentRequestDTO) {
        log.info("Reading appointments for patient ID: {}", appoinmentRequestDTO.getPatienId());
        User user = userRepository.findById(appoinmentRequestDTO.getPatienId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bệnh nhân"));

        return appoinmentRepository.findByUser_Id(user.getUser_id());
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

        if (appoinmentRequestDTO.getSlotId() != null && !appoinmentRequestDTO.getSlotId().equals(existingAppointment.getDoctorSlot_id())) {
            DoctorSlot newSlot = doctorSlotRepository.findByDoctorSlot_Id(appoinmentRequestDTO.getSlotId())
                    .orElseThrow(() -> new RuntimeException("Slot mới không tồn tại"));

            if (newSlot.isBook()) {
                throw new RuntimeException("Slot mới đã được người khác đặt");
            }

            // Giải phóng slot cũ
            if (existingAppointment.getDoctorSlot() != null) {
                existingAppointment.getDoctorSlot().setBook(false);
                doctorSlotRepository.save(existingAppointment.getDoctorSlot());
            }

            // Chiếm slot mới
            newSlot.setBook(true);
            doctorSlotRepository.save(newSlot);

            existingAppointment.setDoctorSlot(newSlot);
            existingAppointment.setDoctor(newSlot.getDoctor());
            existingAppointment.setDate(newSlot.getWorkDate() != null ? newSlot.getWorkDate() : newSlot.getDate());
            existingAppointment.setStartTime(newSlot.getStartTime());
        }

        return appoinmentRepository.save(existingAppointment);
    }
}
