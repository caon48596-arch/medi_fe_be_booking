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

    @PostMapping("/create")
    public ResponseEntity<ResponseDTO<Medical_records>> createMedical(
            @RequestBody Medical_recordReponse medical_recordReponse) {
        Medical_records medical_records = medical_recordsService.createMedical(medical_recordReponse);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Creating medical record successfully", medical_records));
    }

    @RequestMapping(value = {"/read", "/list"}, method = { RequestMethod.POST, RequestMethod.GET })
    public ResponseEntity<ResponseDTO<List<Medical_records>>> readMedical_records(
            @RequestBody(required = false) Medical_recordReponse medical_recordReponse,
            @RequestParam(required = false) Long medicalRecordId) {
        Long targetRecordId = medicalRecordId != null
                ? medicalRecordId
                : (medical_recordReponse != null ? medical_recordReponse.getMedical_recordId() : null);
        List<Medical_records> records = medical_recordsService.readMedicalRecords(targetRecordId);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Reading medical records successfully", records));
    }

    @PutMapping("/update")
    public ResponseEntity<ResponseDTO<Medical_records>> updateMedical_records(
            @RequestBody Medical_recordReponse medical_recordReponse) {
        Medical_records medical_records = medical_recordsService.updatMedical_records(medical_recordReponse);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Updating medical record successfully", medical_records));
    }

    @RequestMapping(value = {"/delete", "/delete/{id}"}, method = { RequestMethod.DELETE, RequestMethod.POST })
    public ResponseEntity<ResponseDTO<Medical_records>> deleteMedical_records(
            @PathVariable(required = false) Long id,
            @RequestBody(required = false) Medical_recordReponse medical_recordReponse) {
        Long targetRecordId = id != null
                ? id
                : (medical_recordReponse != null ? medical_recordReponse.getMedical_recordId() : null);
        Medical_records medical_records = medical_recordsService.deleteMedicalRecord(targetRecordId);
        return ResponseEntity.ok(new ResponseDTO<>(true, "Deleting medical record successfully", medical_records));
    }
}
