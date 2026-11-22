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

    // 2. Đọc danh sách lịch hẹn (Hỗ trợ cả POST và GET)
    @RequestMapping(value = {"/read", "/list"}, method = { RequestMethod.POST, RequestMethod.GET })
    public ResponseEntity<ResponseDTO<List<Appoinment>>> readAppoinments(
            @RequestBody(required = false) AppoinmentRequestDTO appoinmentRequestDTO,
            @RequestParam(required = false) Long patientId) {
        try {
            if (appoinmentRequestDTO == null) {
                appoinmentRequestDTO = new AppoinmentRequestDTO();
            }
            if (patientId != null) {
                appoinmentRequestDTO.setPatienId(patientId);
            }
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

    // 4. Hủy lịch hẹn (giải phóng slot)
    @RequestMapping(value = "/cancel/{id}", method = { RequestMethod.POST, RequestMethod.PUT })
    public ResponseEntity<ResponseDTO<Appoinment>> cancelAppointment(@PathVariable Long id) {
        try {
            Appoinment appoinment = appoinmentService.cancelAppointment(id);
            return ResponseEntity.ok(new ResponseDTO<>(true, "Hủy lịch hẹn thành công", appoinment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

    // 5. Xóa lịch hẹn
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ResponseDTO<Appoinment>> deleteAppointment(@PathVariable Long id) {
        try {
            Appoinment appoinment = appoinmentService.deleteAppointment(id);
            return ResponseEntity.ok(new ResponseDTO<>(true, "Xóa lịch hẹn thành công", appoinment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
        }
    }
}