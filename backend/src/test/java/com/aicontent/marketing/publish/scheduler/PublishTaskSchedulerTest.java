package com.aicontent.marketing.publish.scheduler;

import com.aicontent.marketing.publish.service.PublishTaskService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PublishTaskSchedulerTest {

    @Test
    void scheduledTickDelegatesToPublishTaskService() {
        PublishTaskService service = mock(PublishTaskService.class);
        PublishTaskScheduler scheduler = new PublishTaskScheduler(service);

        scheduler.executeDueTasks();

        verify(service).executeDueScheduledTasks();
    }
}
