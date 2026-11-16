package com.emedical.booking.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // 1. Tạo lịch hẹn
    @PostMapping("/create")
    public ResponseEntity<ResponseDTO<Appoinment>> createAppointment(
            @RequestBody AppoinmentRequestDTO appoinmentRequestDTO) {
        try {
            Appoinment appoinment = appoinmentService.createAppointment(appoinmentRequestDTO);

            ResponseDTO<Appoinment> responseDTO = new ResponseDTO<>(
                    true,
                    "Creating appointment successfully",
                    appoinment);

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            ResponseDTO<Appoinment> responseDTO = new ResponseDTO<>(
                    false,
                    e.getMessage(),
                    null);

            return ResponseEntity.badRequest().body(responseDTO);
        }
    }

    // 2. Đọc danh sách lịch hẹn
    @PostMapping("/read")
    public ResponseEntity<ResponseDTO<List<Appoinment>>> readAppoinments(
            @RequestBody AppoinmentRequestDTO appoinmentRequestDTO) {
        try {
            List<Appoinment> appoinments = appoinmentService.readAppoinments(appoinmentRequestDTO);

            ResponseDTO<List<Appoinment>> responseDTO = new ResponseDTO<>(
                    true,
                    "Reading appointments successfully",
                    appoinments);

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            ResponseDTO<List<Appoinment>> responseDTO = new ResponseDTO<>(
                    false,
                    e.getMessage(),
                    null);

            return ResponseEntity.badRequest().body(responseDTO);
        }
    }

    // 3. Cập nhật lịch hẹn
    @PutMapping("/update")
    public ResponseEntity<ResponseDTO<Appoinment>> updateAppoinment(
            @RequestBody AppoinmentRequestDTO appoinmentRequestDTO) {
        try {
            Appoinment appoinment = appoinmentService.updateAppoinment(appoinmentRequestDTO);

            ResponseDTO<Appoinment> responseDTO = new ResponseDTO<>(
                    true,
                    "Updating appointment successfully",
                    appoinment);

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            ResponseDTO<Appoinment> responseDTO = new ResponseDTO<>(
                    false,
                    e.getMessage(),
                    null);

            return ResponseEntity.badRequest().body(responseDTO);
        }
    }
}