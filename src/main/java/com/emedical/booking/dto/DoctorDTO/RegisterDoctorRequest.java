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
    private String confirmPassword;
    private String email;
    private String specialty;
    private String room_number;
    private Integer price;

    public String getEffectiveConfirmPassword() {
        return confirmPassword != null && !confirmPassword.isBlank() ? confirmPassword : confirePassword;
    }
}
