package com.emedical.booking.dto.UserDTO;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequest {
    private String username;
    private String specialty;
    private LocalDate date;
    private String reason;
    private Long doctorId;
    private Long slotId;
    private String startTime;
    private String patientName;
    private String phone;
    private String email;
}