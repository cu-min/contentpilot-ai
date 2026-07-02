package com.aicontent.marketing.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    USER_DISABLED(1001, "user disabled"),
    USERNAME_OR_PASSWORD_ERROR(1002, "username or password error"),
    USERNAME_EXISTS(1003, "username already exists"),
    INTERNAL_ERROR(500, "internal server error");

    private final Integer code;

    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
