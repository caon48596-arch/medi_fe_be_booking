package com.emedical.booking.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.emedical.booking.dto.AppoinmentDTO.AppoinmentRequestDTO;
import com.emedical.booking.dto.ResponseDTO.ResponseDTO;
import com.emedical.booking.entity.Appoinment;
import com.emedical.booking.service.AppoinmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/appointment")
@RequiredArgsConstructor
public class AppoinmentController {

    private final AppoinmentService appoinmentService;

    @PostMapping("/create")
    public ResponseEntity<ResponseDTO<Appoinment>> createAppointment(
            @RequestBody AppoinmentRequestDTO appoinmentRequestDTO) {
        Appoinment appoinment = appoinmentService.createAppointment(appoinmentRequestDTO);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Creating appointment successfully", appoinment));
    }

    @RequestMapping(value = {"/read", "/list"}, method = { RequestMethod.POST, RequestMethod.GET })
    public ResponseEntity<ResponseDTO<List<Appoinment>>> readAppoinments(
            @RequestBody(required = false) AppoinmentRequestDTO appoinmentRequestDTO,
            @RequestParam(required = false) Long patientId) {
        Long targetPatientId = patientId != null
                ? patientId
                : (appoinmentRequestDTO != null ? appoinmentRequestDTO.getPatienId() : null);
        List<Appoinment> appoinments = appoinmentService.readAppointments(targetPatientId);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Reading appointments successfully", appoinments));
    }

    @PutMapping("/update")
    public ResponseEntity<ResponseDTO<Appoinment>> updateAppoinment(
            @RequestBody AppoinmentRequestDTO appoinmentRequestDTO) {
        Appoinment appoinment = appoinmentService.updateAppoinment(appoinmentRequestDTO);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Updating appointment successfully", appoinment));
    }

    @RequestMapping(value = "/cancel/{id}", method = { RequestMethod.POST, RequestMethod.PUT })
    public ResponseEntity<ResponseDTO<Appoinment>> cancelAppointment(@PathVariable Long id) {
        Appoinment appoinment = appoinmentService.cancelAppointment(id);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Hủy lịch hẹn thành công", appoinment));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ResponseDTO<Appoinment>> deleteAppointment(@PathVariable Long id) {
        Appoinment appoinment = appoinmentService.deleteAppointment(id);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Xóa lịch hẹn thành công", appoinment));
    }
}
