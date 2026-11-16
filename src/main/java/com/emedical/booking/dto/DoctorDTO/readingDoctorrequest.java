package com.emedical.booking.dto.DoctorDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class readingDoctorrequest {
    private String username;
    private String password;
    private String email;
    private Integer phone;
    private String status;
    private String specilty;
    private LocalDateTime date;

}
