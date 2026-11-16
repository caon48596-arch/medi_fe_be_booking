package com.emedical.booking.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "appoinments")
@Data
public class Appoinment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long appoinments_id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "Doctor_id")
    private Doctor doctor;

    @OneToOne
    @JoinColumn(name = "DoctorSlots_id")
    private DoctorSlot doctorSlot;

    @JsonIgnore
    @OneToOne(mappedBy = "appoinment")
    private Medical_records medical_records;

    @Column(name = "DoctorSlots_id", insertable = false, updatable = false)
    private Long doctorSlot_id;
    
    @Column(name = "Doctor_id", insertable = false, updatable = false)
    private Long doctor_id;
    
    private String patientName;
    private Long patient_id;
    private String doctorUsername;
    private LocalDate date;
    private String email;
    private Integer phone;
    private String created_at;
    private String reason;
    private String status;
    private LocalTime startTime;
    private boolean active;
}