package com.trsang.doan2.config;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.trsang.doan2.entities.Role;
import com.trsang.doan2.entities.User;
import com.trsang.doan2.repositories.IRoleRepository;
import com.trsang.doan2.repositories.IUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {
    private final IRoleRepository roleRepository;
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            initRoles();
            initAdminUser();
        };
    }

    private void initRoles() {
        if (!roleRepository.existsByName("ROLE_ADMIN")) {
            roleRepository.save(Role.builder()
                                .name("ROLE_ADMIN")
                                .description("Administrator role").build());
            log.info("Quyền Admin đã được khởi tạo.");
        }

        if (!roleRepository.existsByName("ROLE_USER")) {
            roleRepository.save(Role.builder()
                                .name("ROLE_USER")
                                .description("User role").build());
            log.info("Quyền User đã được khởi tạo.");
        }
    }

    private void initAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            try {
                UUID adminRoleId = roleRepository.findByName("ROLE_ADMIN")
                                .map(Role::getId)
                                .orElseThrow(() -> new RuntimeException("Admin role not found!"));
                
                Role adminRole = Role.builder().id(adminRoleId)
                                                .name("ROLE_ADMIN")
                                                .build();

                Set<Role> roles = new HashSet<>();
                roles.add(adminRole);

                User admin = User.builder()
                                .username("admin")
                                .email("admin@trsangne.com")
                                .password(passwordEncoder.encode("admin123"))
                                .phoneNumber("0987654321")
                                .roles(roles)
                                .build();

                userRepository.save(admin);
                log.info("Khởi tạo người dùng admin thành công.");
            }
            catch (Exception e) {
                log.error("Lỗi khi khởi tạo người dùng admin: {}", e.getMessage(), e);
            }
        }
    }
}
