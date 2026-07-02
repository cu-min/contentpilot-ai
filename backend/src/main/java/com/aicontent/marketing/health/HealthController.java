package com.aicontent.marketing.health;

import com.aicontent.marketing.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("AI内容营销系统后端启动成功");
    }
}
