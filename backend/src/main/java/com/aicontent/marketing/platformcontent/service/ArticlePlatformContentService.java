package com.aicontent.marketing.platformcontent.service;

import com.aicontent.marketing.platformcontent.dto.PlatformContentGenerateRequest;
import com.aicontent.marketing.platformcontent.dto.PlatformContentUpdateRequest;
import com.aicontent.marketing.platformcontent.vo.ArticlePlatformContentVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.aicontent.marketing.platformcontent.entity.ArticlePlatformContent;

import java.util.List;

public interface ArticlePlatformContentService extends IService<ArticlePlatformContent> {

    List<ArticlePlatformContentVO> generate(Long articleId, PlatformContentGenerateRequest request, Long currentUserId);

    List<ArticlePlatformContentVO> listByArticleId(Long articleId);

    ArticlePlatformContentVO getDetail(Long id);

    ArticlePlatformContentVO updateContent(Long id, PlatformContentUpdateRequest request, Long currentUserId);

    void archive(Long id, Long currentUserId);

    void restore(Long id, Long currentUserId);
}
