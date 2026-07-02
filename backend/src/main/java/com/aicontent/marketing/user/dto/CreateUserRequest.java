package com.aicontent.marketing.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "username is required")
    private String username;

    @NotBlank(message = "password is required")
    private String password;

    private String nickname;

    @Email(message = "email format is invalid")
    private String email;

    @NotNull(message = "role is required")
    private String role;
}
