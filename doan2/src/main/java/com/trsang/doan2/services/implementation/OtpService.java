package com.trsang.doan2.services.implementation;

import com.trsang.doan2.entities.Otp;
import com.trsang.doan2.entities.User;
import com.trsang.doan2.repositories.IOtpRepository;
import com.trsang.doan2.services.interfaces.IOtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService implements IOtpService {

    private final IOtpRepository otpRepository;
    private static final int OTP_EXPIRY_MINUTES = 5;

    @Override
    public Otp generateOtp(User user) {
        // Generate a 6-digit OTP
        Random random = new Random();
        int otpValue = 100000 + random.nextInt(900000);
        String otpCode = String.valueOf(otpValue);

        Otp otp = Otp.builder()
                .user(user)
                .otpCode(otpCode)
                .expiryDate(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .isUsed(false)
                .build();

        return otpRepository.save(otp);
    }

    @Override
    public String verifyOtp(User user, String otpCode) {
        Optional<Otp> otpOptional = otpRepository.findByUserAndOtpCodeAndIsUsedFalse(user, otpCode);

        if (otpOptional.isEmpty()) {
            log.warn("Invalid OTP or already used for user {}", user.getEmail());
            return null;
        }

        Otp otp = otpOptional.get();

        // Check if expired
        if (otp.getExpiryDate().isBefore(Instant.now())) {
            log.warn("OTP expired for user {}", user.getEmail());
            return null;
        }

        // Mark as used and generate reset token
        otp.setUsed(true);
        String resetToken = UUID.randomUUID().toString();
        otp.setResetToken(resetToken);
        otpRepository.save(otp);
        return resetToken;
    }

    @Override
    public boolean validateResetToken(User user, String resetToken) {
        Optional<Otp> otpOptional = otpRepository.findByResetToken(resetToken);
        if (otpOptional.isEmpty()) {
            return false;
        }
        Otp otp = otpOptional.get();
        // Check if the token belongs to the user
        if (!otp.getUser().getId().equals(user.getId())) {
            return false;
        }
        return true;
    }
}
