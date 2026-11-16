package com.emedical.booking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.emedical.booking.entity.Doctor;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findBySpecialty(String name);

    Optional<Doctor> findByUsername(String name);

    boolean existsByUsername(String username);

   
}
