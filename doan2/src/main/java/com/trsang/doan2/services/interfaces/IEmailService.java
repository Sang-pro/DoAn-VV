package com.trsang.doan2.services.interfaces;

public interface IEmailService {
    void sendOtpEmail(String to, String otpCode);
}
