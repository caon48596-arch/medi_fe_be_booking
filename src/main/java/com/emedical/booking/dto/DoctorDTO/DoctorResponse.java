package com.emedical.booking.dto.DoctorDTO;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorResponse {

    private Long id;
    private String username;
    private String password;
    private String email;;
    private Integer phone;
    private String status;
    private String specilty;
    private LocalDateTime date;

}
