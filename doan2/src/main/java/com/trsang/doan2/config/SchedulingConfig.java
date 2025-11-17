package com.trsang.doan2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.trsang.doan2.services.interfaces.IRefreshTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig {
    private final IRefreshTokenService refreshTokenService;

    @Scheduled(cron = "0 0 0 * * ?") // Every day at midnight
    public void purgeExpiredTokens() {
        log.info("Running scheduled task: Purging expired refresh tokens");
        refreshTokenService.purgeExpiredTokens();
    }
}
