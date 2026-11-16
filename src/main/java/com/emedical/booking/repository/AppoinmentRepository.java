package com.emedical.booking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.emedical.booking.entity.Appoinment;

@Repository
public interface AppoinmentRepository extends JpaRepository<Appoinment, Long> {

    @Query("SELECT a FROM Appoinment a WHERE a.user.user_id = :userId")
    List<Appoinment> findByUser_Id(@Param("userId") Long userId);

    @Query("SELECT a FROM Appoinment a WHERE a.doctor.doctor_id = :doctorId")
    List<Appoinment> findByDoctorId(@Param("doctorId") Long doctorId);

    List<Appoinment> findByPatientName(String patientName);

    List<Appoinment> findByDoctorUsername(String doctorUsername);

    @Query("SELECT a FROM Appoinment a WHERE a.doctor.doctor_id = :doctorId AND a.status = :status")
    List<Appoinment> findByDoctor_IdAndStatus(@Param("doctorId") Long doctorId, @Param("status") String status);

}
