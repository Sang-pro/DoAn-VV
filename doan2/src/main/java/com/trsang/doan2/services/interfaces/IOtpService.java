package com.trsang.doan2.services.interfaces;

import com.trsang.doan2.entities.Otp;
import com.trsang.doan2.entities.User;

public interface IOtpService {
    Otp generateOtp(User user);
    String verifyOtp(User user, String otpCode);
    boolean validateResetToken(User user, String resetToken);
}
