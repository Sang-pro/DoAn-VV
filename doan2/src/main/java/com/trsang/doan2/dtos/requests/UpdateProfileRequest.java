package com.trsang.doan2.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {
    @Size(max = 50, message = "Name must be less than 50 characters")
    private String firstName;
    
    @Size(max = 50, message = "Name must be less than 50 characters")
    private String lastName;

    @Email(message = "Email should be valid")
    private String email;

    @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$", message = "Phone number is not valid")
    private String phoneNumber;
}
