package com.emedical.booking.dto.UserDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    private Long userId;
    private String username;
    private String password;
    private String email;
    private String phoneNumber;
    private Integer phone;
    private String address;

}
