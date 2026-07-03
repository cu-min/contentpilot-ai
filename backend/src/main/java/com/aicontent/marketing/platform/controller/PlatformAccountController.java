package com.aicontent.marketing.platform.controller;

import com.aicontent.marketing.auth.security.LoginUser;
import com.aicontent.marketing.common.result.Result;
import com.aicontent.marketing.platform.dto.PlatformAccountQueryRequest;
import com.aicontent.marketing.platform.dto.PlatformAccountSaveRequest;
import com.aicontent.marketing.platform.dto.PlatformAccountStatusRequest;
import com.aicontent.marketing.platform.service.PlatformAccountService;
import com.aicontent.marketing.platform.vo.PlatformAccountVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/platform-accounts")
public class PlatformAccountController {

    private final PlatformAccountService platformAccountService;

    public PlatformAccountController(PlatformAccountService platformAccountService) {
        this.platformAccountService = platformAccountService;
    }

    @GetMapping
    public Result<List<PlatformAccountVO>> list(PlatformAccountQueryRequest request) {
        return Result.success(platformAccountService.listAccounts(request));
    }

    @GetMapping("/{id}")
    public Result<PlatformAccountVO> detail(@PathVariable Long id) {
        return Result.success(platformAccountService.getAccountDetail(id));
    }

    @PostMapping
    public Result<PlatformAccountVO> create(
            @Valid @RequestBody PlatformAccountSaveRequest request,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        return Result.success(platformAccountService.createAccount(request, loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    public Result<PlatformAccountVO> update(
            @PathVariable Long id,
            @Valid @RequestBody PlatformAccountSaveRequest request,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        return Result.success(platformAccountService.updateAccount(id, request, loginUser.getUserId()));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody PlatformAccountStatusRequest request,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        platformAccountService.updateStatus(id, request.getEnabled(), loginUser.getUserId());
        return Result.success();
    }
}
