package com.aicontent.marketing.auth.controller;

import com.aicontent.marketing.auth.dto.LoginRequest;
import com.aicontent.marketing.auth.dto.LoginResponse;
import com.aicontent.marketing.auth.service.AuthService;
import com.aicontent.marketing.common.result.Result;
import com.aicontent.marketing.user.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.getCurrentUser());
    }
}
