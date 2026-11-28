package com.emedical.booking.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.emedical.booking.dto.Medical_recorDTO.Medical_recordReponse;
import com.emedical.booking.entity.Medical_records;
import com.emedical.booking.entity.User;
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
        User patient = null;
        if (medical_recordReponse.getPatient_id() != null) {
            patient = userRepository.findById(medical_recordReponse.getPatient_id())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bệnh nhân"));
        }

        String patientName = medical_recordReponse.getUsername();
        if ((patientName == null || patientName.isBlank()) && patient != null) {
            patientName = patient.getFull_name() != null ? patient.getFull_name() : patient.getUsername();
        }

        Medical_records medical_records = Medical_records.builder()
                .patient_id(medical_recordReponse.getPatient_id())
                .patientName(patientName)
                .doctor_id(medical_recordReponse.getDoctorId())
                .doctorName(medical_recordReponse.getDoctorUsername())
                .reason(medical_recordReponse.getReason())
                .diagnosis(medical_recordReponse.getDiagnosis())
                .treatment(medical_recordReponse.getTreatment())
                .prescription(medical_recordReponse.getPrescription())
                .note(medical_recordReponse.getNote())
                .created_at(LocalDate.now().toString())
                .build();

        return medical_recordsRepository.save(medical_records);
    }

    public List<Medical_records> readMedicalRecords(Long medicalRecordId) {
        if (medicalRecordId != null) {
            log.info("Reading medical record ID: {}", medicalRecordId);
            return medical_recordsRepository
                    .findById(medicalRecordId)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        }
        log.info("Reading all medical records");
        return medical_recordsRepository.findAll();
    }

    public List<Medical_records> readMedical_records(Medical_recordReponse medical_recordReponse) {
        return readMedicalRecords(medical_recordReponse != null ? medical_recordReponse.getMedical_recordId() : null);
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
        if (medical_recordReponse.getPatient_id() != null) medical_records.setPatient_id(medical_recordReponse.getPatient_id());
        if (medical_recordReponse.getUsername() != null) medical_records.setPatientName(medical_recordReponse.getUsername());
        if (medical_recordReponse.getDoctorId() != null) medical_records.setDoctor_id(medical_recordReponse.getDoctorId());
        if (medical_recordReponse.getDoctorUsername() != null) medical_records.setDoctorName(medical_recordReponse.getDoctorUsername());

        return medical_recordsRepository.save(medical_records);
    }

    public Medical_records deleteMedicalRecord(Long medicalRecordId) {
        log.info("Deleting medical record ID: {}", medicalRecordId);
        Medical_records medical_records = medical_recordsRepository
                .findById(medicalRecordId)
                .orElseThrow(() -> new RuntimeException("Hồ sơ bệnh án không tồn tại"));

        medical_recordsRepository.delete(medical_records);
        return medical_records;
    }

    public Medical_records deleteMedical_records(Medical_recordReponse medical_RecordReponse) {
        return deleteMedicalRecord(medical_RecordReponse != null ? medical_RecordReponse.getMedical_recordId() : null);
    }
}
