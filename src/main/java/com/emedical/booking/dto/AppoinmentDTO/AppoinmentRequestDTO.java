package com.emedical.booking.dto.AppoinmentDTO;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AppoinmentRequestDTO {

    private Long appoinmentId;
    private Long patienId;
    private Long slotId;
    private String reason;
    private LocalDate workDate;
    private LocalTime starTime;
    private LocalTime startTime;
    private Long doctorId;
    private String status;
}
