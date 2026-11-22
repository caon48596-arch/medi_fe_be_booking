package com.emedical.booking.entity;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "medical_records")
public class Medical_records implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long medical_id;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "medical_records")
    private Appoinment appoinment;

    private Long patient_id;
    private String patientName;
    private Long doctor_id;
    private String doctorName;
    private String created_at;

    private String diagnosis;
    private String reason;
    private String treatment;
    private String prescription;
    private String note;

}
