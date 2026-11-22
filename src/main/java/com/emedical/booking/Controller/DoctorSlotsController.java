package com.emedical.booking.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.emedical.booking.dto.DoctorSlotDTO.GenerateSlotsDTO;
import com.emedical.booking.dto.ResponseDTO.ResponseDTO;
import com.emedical.booking.entity.DoctorSlot;
import com.emedical.booking.service.DoctorSlotsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/doctor-slots")
@RequiredArgsConstructor
public class DoctorSlotsController {

    private final DoctorSlotsService doctorSlotsService;

    // 1. Tạo danh sách slot khám
    @PostMapping("/create")
    public ResponseEntity<ResponseDTO<List<DoctorSlot>>> createDoctorSlots(
            @RequestBody GenerateSlotsDTO generateSlotsDTO) {
        try {
            List<DoctorSlot> doctorSlots = doctorSlotsService.createDoctorSlots(generateSlotsDTO);

            ResponseDTO<List<DoctorSlot>> responseDTO = new ResponseDTO<>(
                    true,
                    "Creating doctor slots successfully",
                    doctorSlots);

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            ResponseDTO<List<DoctorSlot>> responseDTO = new ResponseDTO<>(
                    false,
                    e.getMessage(),
                    null);

            return ResponseEntity.badRequest().body(responseDTO);
        }
    }

    // 2. Đọc danh sách slot khám (Hỗ trợ cả POST và GET)
    @RequestMapping(value = "/read", method = { RequestMethod.POST, RequestMethod.GET })
    public ResponseEntity<ResponseDTO<List<DoctorSlot>>> readDoctorSlots(
            @RequestBody(required = false) GenerateSlotsDTO generateSlotsDTO,
            @RequestParam(required = false) Long doctorId) {
        try {
            if (generateSlotsDTO == null) {
                generateSlotsDTO = new GenerateSlotsDTO();
            }
            if (doctorId != null) {
                generateSlotsDTO.setDoctorId(doctorId);
            }
            List<DoctorSlot> doctorSlots = doctorSlotsService.readDoctorSlots(generateSlotsDTO);

            ResponseDTO<List<DoctorSlot>> responseDTO = new ResponseDTO<>(
                    true,
                    "Reading doctor slots successfully",
                    doctorSlots);

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            ResponseDTO<List<DoctorSlot>> responseDTO = new ResponseDTO<>(
                    false,
                    e.getMessage(),
                    null);

            return ResponseEntity.badRequest().body(responseDTO);
        }
    }

    // 3. Cập nhật slot khám
    @PutMapping("/update")
    public ResponseEntity<ResponseDTO<DoctorSlot>> updateDoctorSlots(
            @RequestBody GenerateSlotsDTO generateSlotsDTO) {
        try {
            DoctorSlot doctorSlot = doctorSlotsService.updateDoctorSlots(generateSlotsDTO);

            ResponseDTO<DoctorSlot> responseDTO = new ResponseDTO<>(
                    true,
                    "Updating doctor slot successfully",
                    doctorSlot);

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            ResponseDTO<DoctorSlot> responseDTO = new ResponseDTO<>(
                    false,
                    e.getMessage(),
                    null);

            return ResponseEntity.badRequest().body(responseDTO);
        }
    }

    // 4. Xóa slot khám
    @RequestMapping(value = {"/delete", "/delete/{id}"}, method = { RequestMethod.DELETE, RequestMethod.POST })
    public ResponseEntity<ResponseDTO<DoctorSlot>> deleteDoctorSlot(
            @PathVariable(required = false) Long id,
            @RequestBody(required = false) GenerateSlotsDTO generateSlotsDTO) {
        try {
            if (generateSlotsDTO == null) {
                generateSlotsDTO = new GenerateSlotsDTO();
            }
            if (id != null) {
                generateSlotsDTO.setDoctorSlotId(id);
            }
            DoctorSlot doctorSlot = doctorSlotsService.deleteDoctorSlot(generateSlotsDTO);

            ResponseDTO<DoctorSlot> responseDTO = new ResponseDTO<>(
                    true,
                    "Deleting doctor slot successfully",
                    doctorSlot);

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            ResponseDTO<DoctorSlot> responseDTO = new ResponseDTO<>(
                    false,
                    e.getMessage(),
                    null);

            return ResponseEntity.badRequest().body(responseDTO);
        }
    }
}