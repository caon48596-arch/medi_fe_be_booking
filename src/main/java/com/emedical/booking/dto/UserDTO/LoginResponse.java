package com.emedical.booking.dto.UserDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class LoginResponse {
    private String username;
    private List<String> roles;
    private boolean requireOtp;
    private String email;
    private Integer otp;

    public LoginResponse(String username, List<String> roles) {
        this.username = username;
        this.roles = roles;
        this.requireOtp = false;
    }

    public LoginResponse(String username, boolean requireOtp, String email, Integer otp) {
        this.username = username;
        this.roles = List.of();
        this.requireOtp = requireOtp;
        this.email = email;
        this.otp = otp;
    }
}
