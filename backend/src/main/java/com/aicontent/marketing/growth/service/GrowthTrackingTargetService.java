package com.aicontent.marketing.growth.service;

import com.aicontent.marketing.growth.dto.GrowthTrackingTargetQueryRequest;
import com.aicontent.marketing.growth.dto.GrowthTrackingTargetSaveRequest;
import com.aicontent.marketing.growth.entity.GrowthTrackingTarget;
import com.aicontent.marketing.growth.vo.GrowthTrackingTargetVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface GrowthTrackingTargetService extends IService<GrowthTrackingTarget> {

    List<GrowthTrackingTargetVO> listTargets(GrowthTrackingTargetQueryRequest request);

    GrowthTrackingTargetVO createTarget(GrowthTrackingTargetSaveRequest request, Long currentUserId);

    GrowthTrackingTargetVO updateTarget(Long id, GrowthTrackingTargetSaveRequest request);

    void updateStatus(Long id, Integer enabled);

    void deleteTarget(Long id);

    GrowthTrackingTargetVO checkTarget(Long id);
}
