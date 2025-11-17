package com.trsang.doan2.services.interfaces;

import java.util.UUID;

import com.trsang.doan2.dtos.requests.ChangePasswordRequest;
import com.trsang.doan2.dtos.requests.UpdateProfileRequest;
import com.trsang.doan2.dtos.responses.MessageResponse;
import com.trsang.doan2.dtos.responses.UserProfileResponse;
import com.trsang.doan2.entities.User;

public interface IUserService {
    User findByUsername (String username);
    User findById (UUID id);

    UserProfileResponse getUserProfile (String username);
    UserProfileResponse updateProfile(String username, UpdateProfileRequest request);

    MessageResponse changePassword(String username, ChangePasswordRequest request);
    MessageResponse updateAvatar(String username, String avatarUrl);
    
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
