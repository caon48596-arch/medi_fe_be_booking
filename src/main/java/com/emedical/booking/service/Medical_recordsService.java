package com.emedical.booking.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.emedical.booking.dto.Medical_recorDTO.Medical_recordReponse;
import com.emedical.booking.entity.Medical_records;
import com.emedical.booking.repository.Medical_recordsRepository;
import com.emedical.booking.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class Medical_recordsService {

    private final Medical_recordsRepository medical_recordsRepository;
    private final UserRepository userRepository;

    public Medical_records createMedical(Medical_recordReponse medical_recordReponse) {
        log.info("Creating new medical record for patient ID: {}", medical_recordReponse.getPatient_id());
        if (medical_recordReponse.getPatient_id() != null && !userRepository.existsById(medical_recordReponse.getPatient_id())) {
            throw new RuntimeException("Không tìm thấy thông tin bệnh nhân");
        }

        Medical_records medical_records = Medical_records.builder()
                .reason(medical_recordReponse.getReason())
                .diagnosis(medical_recordReponse.getDiagnosis())
                .treatment(medical_recordReponse.getTreatment())
                .prescription(medical_recordReponse.getPrescription())
                .note(medical_recordReponse.getNote())
                .build();

        return medical_recordsRepository.save(medical_records);
    }

    public List<Medical_records> readMedical_records(Medical_recordReponse medical_recordReponse) {
        log.info("Reading medical record ID: {}", medical_recordReponse.getMedical_recordId());
        if (medical_recordReponse.getMedical_recordId() != null) {
            return medical_recordsRepository
                    .findById(medical_recordReponse.getMedical_recordId())
                    .map(java.util.Collections::singletonList)
                    .orElse(java.util.Collections.emptyList());
        }
        return medical_recordsRepository.findAll();
    }

    public Medical_records updatMedical_records(Medical_recordReponse medical_recordReponse) {
        log.info("Updating medical record ID: {}", medical_recordReponse.getMedical_recordId());
        Medical_records medical_records = medical_recordsRepository
                .findById(medical_recordReponse.getMedical_recordId())
                .orElseThrow(() -> new RuntimeException("Hồ sơ bệnh án không tồn tại"));

        if (medical_recordReponse.getDiagnosis() != null) medical_records.setDiagnosis(medical_recordReponse.getDiagnosis());
        if (medical_recordReponse.getNote() != null) medical_records.setNote(medical_recordReponse.getNote());
        if (medical_recordReponse.getPrescription() != null) medical_records.setPrescription(medical_recordReponse.getPrescription());
        if (medical_recordReponse.getTreatment() != null) medical_records.setTreatment(medical_recordReponse.getTreatment());
        if (medical_recordReponse.getReason() != null) medical_records.setReason(medical_recordReponse.getReason());

        return medical_recordsRepository.save(medical_records);
    }

    public Medical_records deleteMedical_records(Medical_recordReponse medical_RecordReponse) {
        log.info("Deleting medical record ID: {}", medical_RecordReponse.getMedical_recordId());
        Medical_records medical_records = medical_recordsRepository
                .findById(medical_RecordReponse.getMedical_recordId())
                .orElseThrow(() -> new RuntimeException("Hồ sơ bệnh án không tồn tại"));

        medical_recordsRepository.delete(medical_records);
        return medical_records;
    }
}
