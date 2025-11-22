package com.trsang.doan2.services.interfaces;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.trsang.doan2.dtos.admin.UserAdminResponse;
import com.trsang.doan2.dtos.admin.UserStatusUpdateRequest;
import com.trsang.doan2.dtos.requests.CreateUserRequest;
import com.trsang.doan2.dtos.responses.MessageResponse;

import com.trsang.doan2.dtos.requests.UpdateUserRequest;

public interface IAdminUserService {
    Page<UserAdminResponse> findAllUsers(String search, Boolean isActive, Pageable pageable);
    UserAdminResponse getUserDetails(UUID userid);
    MessageResponse updateUserStatus(UUID userId, UserStatusUpdateRequest request);
    MessageResponse deleteUser(UUID userId);
    UserAdminResponse createUser(CreateUserRequest request);
    UserAdminResponse updateUser(UUID userId, UpdateUserRequest request);
}
