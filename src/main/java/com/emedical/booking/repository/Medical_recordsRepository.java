package com.emedical.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.emedical.booking.entity.Medical_records;

@Repository
public interface Medical_recordsRepository extends JpaRepository<Medical_records, Long> {
}
