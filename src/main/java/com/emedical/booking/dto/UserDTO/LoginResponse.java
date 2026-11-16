package com.emedical.booking.dto.UserDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data

public class LoginResponse {
    private String username;
    private List<String> roles;
}
