package com.aicontent.marketing.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "status is required")
    private Integer status;
}
