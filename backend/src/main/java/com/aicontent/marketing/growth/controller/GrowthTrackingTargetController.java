package com.aicontent.marketing.growth.controller;

import com.aicontent.marketing.auth.security.LoginUser;
import com.aicontent.marketing.common.result.Result;
import com.aicontent.marketing.growth.dto.GrowthTrackingTargetQueryRequest;
import com.aicontent.marketing.growth.dto.GrowthTrackingTargetSaveRequest;
import com.aicontent.marketing.growth.dto.GrowthTrackingTargetStatusRequest;
import com.aicontent.marketing.growth.service.GrowthTrackingTargetService;
import com.aicontent.marketing.growth.vo.GrowthTrackingTargetVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/growth/tracking-targets")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public class GrowthTrackingTargetController {

    private final GrowthTrackingTargetService growthTrackingTargetService;

    public GrowthTrackingTargetController(GrowthTrackingTargetService growthTrackingTargetService) {
        this.growthTrackingTargetService = growthTrackingTargetService;
    }

    @GetMapping
    public Result<List<GrowthTrackingTargetVO>> list(GrowthTrackingTargetQueryRequest request) {
        return Result.success(growthTrackingTargetService.listTargets(request));
    }

    @PostMapping
    public Result<GrowthTrackingTargetVO> create(
            @Valid @RequestBody GrowthTrackingTargetSaveRequest request,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        return Result.success(growthTrackingTargetService.createTarget(request, loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    public Result<GrowthTrackingTargetVO> update(
            @PathVariable Long id,
            @Valid @RequestBody GrowthTrackingTargetSaveRequest request
    ) {
        return Result.success(growthTrackingTargetService.updateTarget(id, request));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody GrowthTrackingTargetStatusRequest request
    ) {
        growthTrackingTargetService.updateStatus(id, request.getEnabled());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        growthTrackingTargetService.deleteTarget(id);
        return Result.success();
    }

    @PostMapping("/{id}/check")
    public Result<GrowthTrackingTargetVO> check(@PathVariable Long id) {
        return Result.success(growthTrackingTargetService.checkTarget(id));
    }
}
