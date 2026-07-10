package com.aicontent.marketing.publish.scheduler;

import com.aicontent.marketing.publish.service.PublishTaskService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PublishTaskScheduler {

    private final PublishTaskService publishTaskService;

    public PublishTaskScheduler(PublishTaskService publishTaskService) {
        this.publishTaskService = publishTaskService;
    }

    @Scheduled(fixedDelayString = "${publish.scheduler.fixed-delay-ms:10000}")
    public void executeDueTasks() {
        publishTaskService.executeDueScheduledTasks();
    }
}
