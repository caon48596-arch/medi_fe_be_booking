package com.emedical.booking.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.emedical.booking.dto.DoctorSlotDTO.GenerateSlotsDTO;
import com.emedical.booking.entity.Appoinment;
import com.emedical.booking.entity.Doctor;
import com.emedical.booking.entity.DoctorSlot;
import com.emedical.booking.repository.AppoinmentRepository;
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
    private final AppoinmentRepository appoinmentRepository;

    public List<DoctorSlot> createDoctorSlots(GenerateSlotsDTO generateSlotsDTO) {
        log.info("Creating new doctor slots for doctorId: {}", generateSlotsDTO.getDoctorId());
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
    }

    public List<DoctorSlot> readDoctorSlots(Long doctorId) {
        if (doctorId == null) {
            log.info("Reading all slots");
            return doctorSlotRepository.findAll();
        }
        log.info("Reading all slots for doctorId: {}", doctorId);
        if (!doctorRepository.existsById(doctorId)) {
            log.warn("Doctor not exists");
            return List.of();
        }
        return doctorSlotRepository.findByDoctorId(doctorId);
    }

    public List<DoctorSlot> readDoctorSlots(GenerateSlotsDTO generateSlotsDTO) {
        return readDoctorSlots(generateSlotsDTO != null ? generateSlotsDTO.getDoctorId() : null);
    }

    public List<DoctorSlot> getSlotsByDoctorUsername(String username) {
        log.info("Đang tải danh sách slot khám của bác sĩ {}", username);
        if (username == null || username.isBlank() || "all".equalsIgnoreCase(username)) {
            return doctorSlotRepository.findAll();
        }
        List<DoctorSlot> slots = doctorSlotRepository.findByDoctorUsername(username);
        log.info("Bác sĩ {} hiện có {} slot khám", username, slots.size());
        return slots;
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

    public DoctorSlot deleteDoctorSlot(Long doctorSlotId) {
        log.info("Deleting doctor slot ID: {}", doctorSlotId);
        DoctorSlot deleteDoctorSlot = doctorSlotRepository.findByDoctorSlot_Id(doctorSlotId)
                .orElseThrow(() -> new RuntimeException("Doctor slot is not found"));

        if (deleteDoctorSlot.getAppoinment() != null) {
            Appoinment app = deleteDoctorSlot.getAppoinment();
            app.setDoctorSlot(null);
            appoinmentRepository.save(app);
        }

        doctorSlotRepository.delete(deleteDoctorSlot);
        return deleteDoctorSlot;
    }

    public DoctorSlot deleteDoctorSlot(GenerateSlotsDTO generateSlotsDTO) {
        return deleteDoctorSlot(generateSlotsDTO != null ? generateSlotsDTO.getDoctorSlotId() : null);
    }

    public DoctorSlot occupySlot(Long slotId) {
        DoctorSlot slot = doctorSlotRepository.findByDoctorSlot_Id(slotId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy slot khám"));
        if (slot.isBook()) {
            throw new RuntimeException("Slot khám này đã được người khác đặt trước đó");
        }
        slot.setActive(true);
        slot.setBook(true);
        return doctorSlotRepository.save(slot);
    }

    public DoctorSlot occupySpecificSlot(DoctorSlot slot) {
        slot.setActive(true);
        slot.setBook(true);
        return doctorSlotRepository.save(slot);
    }

    public void releaseSlot(DoctorSlot slot) {
        if (slot != null) {
            slot.setBook(false);
            doctorSlotRepository.save(slot);
        }
    }

    public List<DoctorSlot> findAvailableReplacementSlots(Long excludedDoctorId, String specialty, LocalDate date) {
        return doctorSlotRepository.findByDoctor_IdNotAndDoctor_SpecialtyAndDateAndIsBooked(
                excludedDoctorId,
                specialty,
                date,
                false);
    }
}
