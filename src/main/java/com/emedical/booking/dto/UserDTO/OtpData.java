package com.emedical.booking.dto.UserDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpData {

    public static final long DEFAULT_TTL_MS = 5 * 60_000L;
    public static final int MAX_ATTEMPTS = 5;

    private Integer otp;
    private Long expireTime;
    private Integer attempts;

    public OtpData(Integer otp) {
        this.otp = otp;
        this.expireTime = System.currentTimeMillis() + DEFAULT_TTL_MS;
        this.attempts = 0;
    }

    public boolean isExpired() {
        return expireTime == null || System.currentTimeMillis() > expireTime;
    }

    public void increaseAttempt() {
        this.attempts = (this.attempts == null ? 0 : this.attempts) + 1;
    }
}
