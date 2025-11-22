package com.trsang.doan2.services.implementation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trsang.doan2.dtos.admin.UserAdminResponse;
import com.trsang.doan2.dtos.admin.UserStatusUpdateRequest;
import com.trsang.doan2.dtos.requests.CreateUserRequest;
import com.trsang.doan2.dtos.requests.UpdateUserRequest;
import com.trsang.doan2.dtos.responses.MessageResponse;
import com.trsang.doan2.entities.Role;
import com.trsang.doan2.entities.User;
import com.trsang.doan2.exceptions.ResourceNotFoundException;
import com.trsang.doan2.exceptions.ServiceException;
import com.trsang.doan2.repositories.IRefreshTokenRepository;
import com.trsang.doan2.repositories.IRoleRepository;
import com.trsang.doan2.repositories.IUserRepository;
import com.trsang.doan2.services.interfaces.IAdminUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService implements IAdminUserService {
    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IRefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserAdminResponse> findAllUsers(String search, Boolean isActive, Pageable pageable) {
        log.info("Searching for all users with search: {}, isActive: {}, page = {}", search, isActive, pageable);
        Page<User> users;
        if (search != null && !search.isBlank()) {
            users = userRepository.findByUsernameContainingOrEmailContainingOrFirstNameContainingOrLastNameContaining(
                    search, search, search, search, pageable);
        } else if (isActive != null) {
            users = userRepository.findByIsActive(isActive, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(this::mapToUserAdminResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAdminResponse getUserDetails(UUID userid) {
        log.info("Getting user details for userId: {}", userid);
        User user = userRepository.findById(userid)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        return mapToUserAdminResponse(user);
    }

    @Override
    @Transactional
    public MessageResponse updateUserStatus(UUID userId, UserStatusUpdateRequest request) {
        log.info("Updating user status for userId: {}, request: {}", userId, request);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean hasAdminRole = user.getRoles().stream()
                    .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

            if (hasAdminRole && Boolean.FALSE.equals(request.getIsActive())) {
                throw new ServiceException("Khong thể vô hiệu hóa tài khoản admin");
            }
            if (request.getIsActive() != null) {
                user.setActive(request.getIsActive());
            }

            if (request.getLockedUntil() != null) {
                user.setLockedUntil(request.getLockedUntil());
            }

            userRepository.save(user);

            String statusMessage = Boolean.TRUE.equals(request.getIsActive()) ? "Kích hoạt" : "Vô hiệu hóa";
            return MessageResponse.builder()
                    .message(statusMessage)
                    .success(true)
                    .build();
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi cập nhập trạng thái", e);
            throw new ServiceException("Lỗi khi cập nhập trạng thái người dùng", e);
        }
    }

    @Override
    @Transactional
    public MessageResponse deleteUser(UUID userId) {
        log.info("Deleting user with userId: {}", userId);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            boolean hasAdminRole = user.getRoles().stream()
                    .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

            if (hasAdminRole) {
                throw new ServiceException("Cannot delete an admin account.");
            }

            refreshTokenRepository.deleteByUser(user);
            userRepository.delete(user);

            return MessageResponse.builder()
                    .message("User deleted successfully.")
                    .success(true)
                    .build();
        } catch (ResourceNotFoundException e) {
            log.error("Error deleting user: {}", e.getMessage());
            throw e;
        } catch (ServiceException e) {
            log.error("Error deleting user: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("An unexpected error occurred while deleting user with id: {}", userId, e);
            throw new ServiceException("An unexpected error occurred while deleting the user.", e);
        }
    }

    @Override
    @Transactional
    public UserAdminResponse createUser(CreateUserRequest request) {
        log.info("Creating a new user with username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ServiceException("Username is already taken.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ServiceException("Email is already in use.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .isActive(true)
                .build();

        Set<String> strRoles = request.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new ServiceException("Default role not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                Role foundRole = roleRepository.findByName(role)
                        .orElseThrow(() -> new ServiceException("Role not found: " + role));
                roles.add(foundRole);
            });
        }
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        log.info("User created successfully with id: {}", savedUser.getId());
        return mapToUserAdminResponse(savedUser);
    }

    @Override
    @Transactional
    public UserAdminResponse updateUser(UUID userId, UpdateUserRequest request) {
        log.info("Updating user with userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            request.getRoles().forEach(role -> {
                Role foundRole = roleRepository.findByName(role)
                        .orElseThrow(() -> new ServiceException("Role not found: " + role));
                roles.add(foundRole);
            });
            user.setRoles(roles);
        }

        User updatedUser = userRepository.save(user);

        log.info("User updated successfully with id: {}", updatedUser.getId());
        return mapToUserAdminResponse(updatedUser);
    }

    private UserAdminResponse mapToUserAdminResponse(User user) {
        return UserAdminResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .displayName(user.getDisplayName())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.isActive())
                .lockedUntil(user.getLockedUntil())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

