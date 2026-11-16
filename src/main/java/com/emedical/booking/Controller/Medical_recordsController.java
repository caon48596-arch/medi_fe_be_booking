package com.emedical.booking.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.emedical.booking.dto.Medical_recorDTO.Medical_recordReponse;
import com.emedical.booking.dto.ResponseDTO.ResponseDTO;
import com.emedical.booking.entity.Medical_records;
import com.emedical.booking.service.Medical_recordsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/medical-records")
@RequiredArgsConstructor
public class Medical_recordsController {

    private final Medical_recordsService medical_recordsService;

    // 1. Tạo hồ sơ bệnh án
    @PostMapping("/create")
    public ResponseEntity<ResponseDTO<Medical_records>> createMedical(
            @RequestBody Medical_recordReponse medical_recordReponse) {
        try {
            Medical_records medical_records = medical_recordsService.createMedical(medical_recordReponse);

            ResponseDTO<Medical_records> responseDTO = new ResponseDTO<>(
                    true,
                    "Creating medical record successfully",
                    medical_records);

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            ResponseDTO<Medical_records> responseDTO = new ResponseDTO<>(
                    false,
                    e.getMessage(),
                    null);

            return ResponseEntity.badRequest().body(responseDTO);
        }
    }

    // 2. Đọc hồ sơ bệnh án
    @PostMapping("/read")
    public ResponseEntity<ResponseDTO<List<Medical_records>>> readMedical_records(
            @RequestBody Medical_recordReponse medical_recordReponse) {
        try {
            List<Medical_records> medical_recordses = medical_recordsService.readMedical_records(
                    medical_recordReponse);

            ResponseDTO<List<Medical_records>> responseDTO = new ResponseDTO<>(
                    true,
                    "Reading medical records successfully",
                    medical_recordses);

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            ResponseDTO<List<Medical_records>> responseDTO = new ResponseDTO<>(
                    false,
                    e.getMessage(),
                    null);

            return ResponseEntity.badRequest().body(responseDTO);
        }
    }

    // 3. Cập nhật hồ sơ bệnh án
    @PutMapping("/update")
    public ResponseEntity<ResponseDTO<Medical_records>> updateMedical_records(
            @RequestBody Medical_recordReponse medical_recordReponse) {
        try {
            Medical_records medical_records = medical_recordsService.updatMedical_records(
                    medical_recordReponse);

            ResponseDTO<Medical_records> responseDTO = new ResponseDTO<>(
                    true,
                    "Updating medical record successfully",
                    medical_records);

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            ResponseDTO<Medical_records> responseDTO = new ResponseDTO<>(
                    false,
                    e.getMessage(),
                    null);

            return ResponseEntity.badRequest().body(responseDTO);
        }
    }

    // 4. Xóa hồ sơ bệnh án
    @DeleteMapping("/delete")
    public ResponseEntity<ResponseDTO<Medical_records>> deleteMedical_records(
            @RequestBody Medical_recordReponse medical_recordReponse) {
        try {
            Medical_records medical_records = medical_recordsService.deleteMedical_records(
                    medical_recordReponse);

            ResponseDTO<Medical_records> responseDTO = new ResponseDTO<>(
                    true,
                    "Deleting medical record successfully",
                    medical_records);

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            ResponseDTO<Medical_records> responseDTO = new ResponseDTO<>(
                    false,
                    e.getMessage(),
                    null);

            return ResponseEntity.badRequest().body(responseDTO);
        }
    }
}