package com.emedical.booking.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.emedical.booking.dto.DoctorSlotDTO.GenerateSlotsDTO;
import com.emedical.booking.entity.Doctor;
import com.emedical.booking.entity.DoctorSlot;
import com.emedical.booking.repository.DoctorRepository;
import com.emedical.booking.repository.DoctorSlotRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DoctorSlotsService {

    private final DoctorSlotRepository doctorSlotRepository;
    private final DoctorRepository doctorRepository;

    public List<DoctorSlot> createDoctorSlots(GenerateSlotsDTO generateSlotsDTO) {
        log.info("Creating new doctor slots for doctorId: {}", generateSlotsDTO.getDoctorId());
        try {
            Doctor doctor = doctorRepository.findById(generateSlotsDTO.getDoctorId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ"));

            LocalTime currentTime = generateSlotsDTO.getStartShift();
            Integer durationTime = generateSlotsDTO.getDurationMinutes();
            LocalTime endTime = generateSlotsDTO.getEndShift();
            LocalDate workDate = generateSlotsDTO.getWorkdate();

            if (currentTime == null || endTime == null || durationTime == null || durationTime <= 0) {
                throw new RuntimeException("Thông tin ca làm việc hoặc thời lượng slot không hợp lệ");
            }

            List<DoctorSlot> doctorSlots = new ArrayList<>();
            while (currentTime.isBefore(endTime)) {
                LocalTime slotEndTime = currentTime.plusMinutes(durationTime);
                if (slotEndTime.isAfter(endTime)) {
                    break;
                }

                DoctorSlot doctorSlot = new DoctorSlot();
                doctorSlot.setDoctor(doctor);
                doctorSlot.setWorkDate(workDate);
                doctorSlot.setDate(workDate);
                doctorSlot.setStartTime(currentTime);
                doctorSlot.setEndTime(slotEndTime);
                doctorSlot.setActive(true);
                doctorSlot.setBook(false);

                doctorSlots.add(doctorSlot);
                currentTime = slotEndTime;
            }

            return doctorSlotRepository.saveAll(doctorSlots);

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            log.error("Lỗi khi tạo slots khám: {}", e.getMessage(), e);
            throw new RuntimeException("Error creating doctor slots: " + e.getMessage());
        }
    }

    public List<DoctorSlot> readDoctorSlots(GenerateSlotsDTO generateSlotsDTO) {
        log.info("Reading all slots for doctorId: {}", generateSlotsDTO.getDoctorId());
        if (!doctorRepository.existsById(generateSlotsDTO.getDoctorId())) {
            log.warn("Doctor not exists");
            throw new RuntimeException("Doctor is not found");
        }
        return doctorSlotRepository.findByDoctorId(generateSlotsDTO.getDoctorId());
    }

    public DoctorSlot updateDoctorSlots(GenerateSlotsDTO generateSlotsDTO) {
        log.info("Updating doctor slot ID: {}", generateSlotsDTO.getDoctorSlotId());
        DoctorSlot fixDoctorSlot = doctorSlotRepository.findByDoctorSlot_Id(generateSlotsDTO.getDoctorSlotId())
                .orElseThrow(() -> new RuntimeException("Doctor slot is not found"));

        if (generateSlotsDTO.getStartShift() != null) {
            fixDoctorSlot.setStartTime(generateSlotsDTO.getStartShift());
        }
        if (generateSlotsDTO.getEndShift() != null) {
            fixDoctorSlot.setEndTime(generateSlotsDTO.getEndShift());
        }
        if (generateSlotsDTO.getWorkdate() != null) {
            fixDoctorSlot.setWorkDate(generateSlotsDTO.getWorkdate());
            fixDoctorSlot.setDate(generateSlotsDTO.getWorkdate());
        }

        return doctorSlotRepository.save(fixDoctorSlot);
    }

    public DoctorSlot deleteDoctorSlot(GenerateSlotsDTO generateSlotsDTO) {
        log.info("Deleting doctor slot ID: {}", generateSlotsDTO.getDoctorSlotId());
        DoctorSlot deleteDoctorSlot = doctorSlotRepository.findByDoctorSlot_Id(generateSlotsDTO.getDoctorSlotId())
                .orElseThrow(() -> new RuntimeException("Doctor slot is not found"));

        doctorSlotRepository.delete(deleteDoctorSlot);
        return deleteDoctorSlot;
    }
}
