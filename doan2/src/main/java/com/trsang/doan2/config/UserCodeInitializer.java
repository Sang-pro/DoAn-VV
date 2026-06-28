package com.trsang.doan2.config;

import com.trsang.doan2.entities.User;
import com.trsang.doan2.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCodeInitializer implements CommandLineRunner {

    private final IUserRepository userRepository;
    private final Random random = new Random();

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking for users without userCode...");
        List<User> allUsers = userRepository.findAll();
        long updatedCount = 0;

        for (User user : allUsers) {
            if (user.getUserCode() == null || user.getUserCode().trim().isEmpty()) {
                String newCode;
                do {
                    newCode = "LIB" + (10000 + random.nextInt(90000));
                } while (userRepository.existsByUserCode(newCode));

                user.setUserCode(newCode);
                userRepository.save(user);
                updatedCount++;
                log.info("Assigned userCode {} to user {}", newCode, user.getUsername());
            }
        }

        if (updatedCount > 0) {
            log.info("Successfully initialized userCode for {} legacy users.", updatedCount);
        } else {
            log.info("All users already have a userCode.");
        }
    }
}
