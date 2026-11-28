package com.emedical.booking.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/create")
    public ResponseEntity<ResponseDTO<List<DoctorSlot>>> createDoctorSlots(
            @RequestBody GenerateSlotsDTO generateSlotsDTO) {
        List<DoctorSlot> doctorSlots = doctorSlotsService.createDoctorSlots(generateSlotsDTO);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Creating doctor slots successfully", doctorSlots));
    }

    @RequestMapping(value = "/read", method = { RequestMethod.POST, RequestMethod.GET })
    public ResponseEntity<ResponseDTO<List<DoctorSlot>>> readDoctorSlots(
            @RequestBody(required = false) GenerateSlotsDTO generateSlotsDTO,
            @RequestParam(required = false) Long doctorId) {
        Long targetDoctorId = doctorId != null
                ? doctorId
                : (generateSlotsDTO != null ? generateSlotsDTO.getDoctorId() : null);
        List<DoctorSlot> doctorSlots = doctorSlotsService.readDoctorSlots(targetDoctorId);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Reading doctor slots successfully", doctorSlots));
    }

    @PutMapping("/update")
    public ResponseEntity<ResponseDTO<DoctorSlot>> updateDoctorSlots(
            @RequestBody GenerateSlotsDTO generateSlotsDTO) {
        DoctorSlot doctorSlot = doctorSlotsService.updateDoctorSlots(generateSlotsDTO);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Updating doctor slot successfully", doctorSlot));
    }

    @RequestMapping(value = {"/delete", "/delete/{id}"}, method = { RequestMethod.DELETE, RequestMethod.POST })
    public ResponseEntity<ResponseDTO<DoctorSlot>> deleteDoctorSlot(
            @PathVariable(required = false) Long id,
            @RequestBody(required = false) GenerateSlotsDTO generateSlotsDTO) {
        Long targetSlotId = id != null
                ? id
                : (generateSlotsDTO != null ? generateSlotsDTO.getDoctorSlotId() : null);
        DoctorSlot doctorSlot = doctorSlotsService.deleteDoctorSlot(targetSlotId);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Deleting doctor slot successfully", doctorSlot));
    }
}
