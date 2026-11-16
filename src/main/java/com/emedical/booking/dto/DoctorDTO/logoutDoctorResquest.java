package com.emedical.booking.dto.DoctorDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class logoutDoctorResquest {
    private String username;
    private String password;
}
