package com.aicontent.marketing.common.exception;

import com.aicontent.marketing.common.result.Result;
import com.aicontent.marketing.common.result.ResultCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        return new Result<>(exception.getCode(), exception.getMessage(), null);
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException() {
        return Result.failed(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public Result<Void> handleBadRequest(Exception exception) {
        return new Result<>(ResultCode.BAD_REQUEST.getCode(), exception.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        return Result.failed(exception.getMessage());
    }
}
