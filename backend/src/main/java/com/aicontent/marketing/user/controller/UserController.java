package com.aicontent.marketing.user.controller;

import com.aicontent.marketing.auth.security.LoginUser;
import com.aicontent.marketing.common.result.Result;
import com.aicontent.marketing.user.dto.CreateUserRequest;
import com.aicontent.marketing.user.dto.UpdateUserRequest;
import com.aicontent.marketing.user.dto.UpdateUserStatusRequest;
import com.aicontent.marketing.user.service.SysUserService;
import com.aicontent.marketing.user.vo.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final SysUserService sysUserService;

    public UserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @GetMapping
    public Result<Page<UserVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword
    ) {
        return Result.success(sysUserService.listUsers(page, size, keyword));
    }

    @PostMapping
    public Result<UserVO> create(@Valid @RequestBody CreateUserRequest request) {
        return Result.success(sysUserService.createUser(request));
    }

    @PutMapping("/{id}")
    public Result<UserVO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        return Result.success(sysUserService.updateUser(id, request, loginUser.getUserId()));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        sysUserService.updateStatus(id, request.getStatus(), loginUser.getUserId());
        return Result.success();
    }
}
