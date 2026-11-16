package com.emedical.booking.dto.DoctorDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDoctorRequest {

    private String username;
    private String password;
    private String confirePassword;
    private String email;

}