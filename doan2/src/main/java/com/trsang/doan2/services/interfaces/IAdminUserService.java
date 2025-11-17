package com.trsang.doan2.services.interfaces;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.trsang.doan2.dtos.admin.UserAdminResponse;
import com.trsang.doan2.dtos.admin.UserStatusUpdateRequest;
import com.trsang.doan2.dtos.responses.MessageResponse;

public interface IAdminUserService {
    Page<UserAdminResponse> findAllUsers(String search, Boolean isActive, Pageable pageable);
    UserAdminResponse getUserDetails(UUID userid);
    MessageResponse updateUserStatus(UUID userId, UserStatusUpdateRequest request);
}
