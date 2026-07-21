package com.aicontent.marketing.publish.controller;

import com.aicontent.marketing.auth.security.LoginUser;
import com.aicontent.marketing.common.result.Result;
import com.aicontent.marketing.publish.dto.PublishTaskQueryRequest;
import com.aicontent.marketing.publish.dto.PublishTaskSaveRequest;
import com.aicontent.marketing.publish.dto.PublishTaskSubmitRequest;
import com.aicontent.marketing.publish.service.PublishTaskService;
import com.aicontent.marketing.publish.vo.PublishTaskVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publish/tasks")
public class PublishTaskController {

    private final PublishTaskService publishTaskService;

    public PublishTaskController(PublishTaskService publishTaskService) {
        this.publishTaskService = publishTaskService;
    }

    @GetMapping
    public Result<Page<PublishTaskVO>> list(PublishTaskQueryRequest request) {
        return Result.success(publishTaskService.listTasks(request));
    }

    @GetMapping("/{id}")
    public Result<PublishTaskVO> detail(@PathVariable Long id) {
        return Result.success(publishTaskService.getTaskDetail(id));
    }

    @PostMapping
    public Result<PublishTaskVO> create(
            @Valid @RequestBody PublishTaskSaveRequest request,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        return Result.success(publishTaskService.createTask(request, loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    public Result<PublishTaskVO> update(
            @PathVariable Long id,
            @Valid @RequestBody PublishTaskSaveRequest request,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        return Result.success(publishTaskService.updateTask(id, request, loginUser.getUserId()));
    }

    @PutMapping("/{id}/submit")
    public Result<Void> submit(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) PublishTaskSubmitRequest request,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        publishTaskService.submitTask(id, request, loginUser.getUserId());
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        publishTaskService.cancelTask(id, loginUser.getUserId());
        return Result.success();
    }

    @PostMapping("/{id}/prepare")
    public Result<PublishTaskVO> prepare(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        return Result.success(publishTaskService.prepareTask(id, loginUser.getUserId()));
    }

}
