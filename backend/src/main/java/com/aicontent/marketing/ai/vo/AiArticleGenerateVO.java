package com.aicontent.marketing.ai.vo;

import com.aicontent.marketing.article.vo.ArticleDetailVO;
import lombok.Data;

@Data
public class AiArticleGenerateVO {

    private Long articleId;

    private String title;

    private String summary;

    private String content;

    private String type;

    private String language;

    private String status;

    private String tags;

    private String keywords;

    public static AiArticleGenerateVO from(ArticleDetailVO article) {
        AiArticleGenerateVO vo = new AiArticleGenerateVO();
        vo.setArticleId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        vo.setType(article.getType());
        vo.setLanguage(article.getLanguage());
        vo.setStatus(article.getStatus());
        vo.setTags(article.getTags());
        vo.setKeywords(article.getKeywords());
        return vo;
    }
}
