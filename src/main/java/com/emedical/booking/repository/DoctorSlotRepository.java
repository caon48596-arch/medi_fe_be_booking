package com.emedical.booking.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.emedical.booking.entity.DoctorSlot;

@Repository
public interface DoctorSlotRepository extends JpaRepository<DoctorSlot, Long> {

    @Query("SELECT ds FROM DoctorSlot ds WHERE ds.doctor.doctor_id = :doctorId")
    List<DoctorSlot> findByDoctorId(@Param("doctorId") Long doctorId);

    @Query("SELECT ds FROM DoctorSlot ds WHERE ds.doctor.username = :username")
    List<DoctorSlot> findByDoctorUsername(@Param("username") String username);

    @Query("SELECT ds FROM DoctorSlot ds WHERE ds.DoctorSlot_Id = :doctorSlotId")
    Optional<DoctorSlot> findByDoctorSlot_Id(@Param("doctorSlotId") Long doctorSlotId);

    boolean existsByUsername(String username);

    @Query("SELECT ds FROM DoctorSlot ds WHERE ds.doctor.doctor_id != :doctorId AND ds.doctor.specialty = :specialty AND (ds.date = :date OR ds.workDate = :date) AND ds.book = :isBooked")
    List<DoctorSlot> findByDoctor_IdNotAndDoctor_SpecialtyAndDateAndIsBooked(
            @Param("doctorId") Long doctorId,
            @Param("specialty") String specialty,
            @Param("date") LocalDate date,
            @Param("isBooked") boolean isBooked);

}
