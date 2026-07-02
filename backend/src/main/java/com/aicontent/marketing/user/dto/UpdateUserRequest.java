package com.aicontent.marketing.user.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {

    private String nickname;

    @Email(message = "email format is invalid")
    private String email;

    private String role;

    private Integer status;
}
