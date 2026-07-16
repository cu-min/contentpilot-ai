package com.aicontent.marketing.publish.service;

import com.aicontent.marketing.publish.dto.PublishTaskQueryRequest;
import com.aicontent.marketing.publish.dto.PublishTaskSaveRequest;
import com.aicontent.marketing.publish.dto.PublishTaskSubmitRequest;
import com.aicontent.marketing.publish.entity.PublishTask;
import com.aicontent.marketing.publish.vo.PublishTaskVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PublishTaskService extends IService<PublishTask> {

    Page<PublishTaskVO> listTasks(PublishTaskQueryRequest request);

    PublishTaskVO getTaskDetail(Long id);

    PublishTaskVO createTask(PublishTaskSaveRequest request, Long currentUserId);

    PublishTaskVO updateTask(Long id, PublishTaskSaveRequest request, Long currentUserId);

    void submitTask(Long id, PublishTaskSubmitRequest request, Long currentUserId);

    void cancelTask(Long id, Long currentUserId);

    PublishTaskVO prepareTask(Long id, Long currentUserId);

    PublishTaskVO executeTask(Long id, Long currentUserId);

}
