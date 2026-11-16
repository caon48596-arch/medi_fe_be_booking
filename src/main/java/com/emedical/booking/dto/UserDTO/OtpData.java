package com.emedical.booking.dto.UserDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpData {
    private Integer otp;
    private Long expireTime;
    private Integer attempts;

    public OtpData(Integer otp) {
        this.otp = otp;
        this.expireTime = System.currentTimeMillis() + 60_000;
        this.attempts = 0;
    }

    public void increaseAttempt() {
        attempts++;
    }
}
