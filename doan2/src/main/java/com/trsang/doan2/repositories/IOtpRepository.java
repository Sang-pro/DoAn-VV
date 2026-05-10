package com.trsang.doan2.repositories;

import com.trsang.doan2.entities.Otp;
import com.trsang.doan2.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IOtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByUserAndOtpCodeAndIsUsedFalse(User user, String otpCode);
    Optional<Otp> findByResetToken(String resetToken);
}
