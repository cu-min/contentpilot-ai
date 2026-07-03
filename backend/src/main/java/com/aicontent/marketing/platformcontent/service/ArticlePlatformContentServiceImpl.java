package com.aicontent.marketing.platformcontent.service;

import com.aicontent.marketing.ai.parser.AiJsonParser;
import com.aicontent.marketing.ai.service.AiModelService;
import com.aicontent.marketing.ai.vo.AiGeneratedArticle;
import com.aicontent.marketing.article.entity.Article;
import com.aicontent.marketing.article.mapper.ArticleMapper;
import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.common.result.ResultCode;
import com.aicontent.marketing.platformcontent.dto.PlatformContentGenerateRequest;
import com.aicontent.marketing.platformcontent.dto.PlatformContentUpdateRequest;
import com.aicontent.marketing.platformcontent.entity.ArticlePlatformContent;
import com.aicontent.marketing.platformcontent.mapper.ArticlePlatformContentMapper;
import com.aicontent.marketing.platformcontent.prompt.PlatformContentPromptBuilder;
import com.aicontent.marketing.platformcontent.vo.ArticlePlatformContentVO;
import com.aicontent.marketing.product.service.ProductConfigService;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ArticlePlatformContentServiceImpl extends ServiceImpl<ArticlePlatformContentMapper, ArticlePlatformContent>
        implements ArticlePlatformContentService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private static final Set<String> PLATFORMS = Set.of(
            "WECHAT_OFFICIAL",
            "ZHIHU",
            "CSDN",
            "JUEJIN"
    );

    private final ArticleMapper articleMapper;
    private final ProductConfigService productConfigService;
    private final AiModelService aiModelService;
    private final AiJsonParser aiJsonParser;
    private final PlatformContentPromptBuilder promptBuilder;

    public ArticlePlatformContentServiceImpl(
            ArticleMapper articleMapper,
            ProductConfigService productConfigService,
            AiModelService aiModelService,
            AiJsonParser aiJsonParser,
            PlatformContentPromptBuilder promptBuilder
    ) {
        this.articleMapper = articleMapper;
        this.productConfigService = productConfigService;
        this.aiModelService = aiModelService;
        this.aiJsonParser = aiJsonParser;
        this.promptBuilder = promptBuilder;
    }

    @Override
    @Transactional
    public List<ArticlePlatformContentVO> generate(Long articleId, PlatformContentGenerateRequest request, Long currentUserId) {
        Article article = getRequiredArticle(articleId);
        if (!StringUtils.hasText(article.getContent())) {
            throw new BusinessException("原始文章内容为空");
        }

        List<String> platforms = normalizePlatforms(request.getPlatforms());
        ProductConfigVO productConfig = productConfigService.getCurrentConfig();
        if (!StringUtils.hasText(productConfig.getProductName())) {
            throw new BusinessException("请先完成产品配置");
        }

        return platforms.stream()
                .map(platform -> generateOne(productConfig, article, platform, request.getExtraRequirement(), currentUserId))
                .map(ArticlePlatformContentVO::from)
                .toList();
    }

    @Override
    public List<ArticlePlatformContentVO> listByArticleId(Long articleId) {
        getRequiredArticle(articleId);
        return list(new LambdaQueryWrapper<ArticlePlatformContent>()
                .eq(ArticlePlatformContent::getArticleId, articleId)
                .orderByAsc(ArticlePlatformContent::getPlatform)
                .orderByDesc(ArticlePlatformContent::getUpdatedAt)
                .orderByDesc(ArticlePlatformContent::getId))
                .stream()
                .map(ArticlePlatformContentVO::from)
                .toList();
    }

    @Override
    public ArticlePlatformContentVO getDetail(Long id) {
        return ArticlePlatformContentVO.from(getRequiredContent(id));
    }

    @Override
    @Transactional
    public ArticlePlatformContentVO updateContent(Long id, PlatformContentUpdateRequest request, Long currentUserId) {
        ArticlePlatformContent content = getRequiredContent(id);
        content.setTitle(request.getTitle());
        content.setSummary(request.getSummary());
        content.setContent(request.getContent());
        content.setTags(request.getTags());
        content.setKeywords(request.getKeywords());
        content.setUpdatedBy(currentUserId);
        content.setUpdatedAt(LocalDateTime.now());
        updateById(content);
        return ArticlePlatformContentVO.from(content);
    }

    @Override
    @Transactional
    public void archive(Long id, Long currentUserId) {
        ArticlePlatformContent content = getRequiredContent(id);
        content.setStatus(STATUS_ARCHIVED);
        content.setUpdatedBy(currentUserId);
        content.setUpdatedAt(LocalDateTime.now());
        updateById(content);
    }

    @Override
    @Transactional
    public void restore(Long id, Long currentUserId) {
        ArticlePlatformContent content = getRequiredContent(id);
        content.setStatus(STATUS_DRAFT);
        content.setUpdatedBy(currentUserId);
        content.setUpdatedAt(LocalDateTime.now());
        updateById(content);
    }

    private ArticlePlatformContent generateOne(
            ProductConfigVO productConfig,
            Article article,
            String platform,
            String extraRequirement,
            Long currentUserId
    ) {
        String userPrompt = promptBuilder.buildUserPrompt(productConfig, article, platform, extraRequirement);
        String rawResult = aiModelService.chat(promptBuilder.buildSystemPrompt(), userPrompt);
        AiGeneratedArticle generatedArticle = aiJsonParser.parseGeneratedArticle(rawResult);

        LocalDateTime now = LocalDateTime.now();
        ArticlePlatformContent content = getOne(new LambdaQueryWrapper<ArticlePlatformContent>()
                .eq(ArticlePlatformContent::getArticleId, article.getId())
                .eq(ArticlePlatformContent::getPlatform, platform)
                .last("LIMIT 1"), false);

        if (content == null) {
            content = new ArticlePlatformContent();
            content.setArticleId(article.getId());
            content.setPlatform(platform);
            content.setCreatedBy(currentUserId);
            content.setCreatedAt(now);
        }

        content.setTitle(requiredText(generatedArticle.getTitle(), "AI 返回标题为空，请重试"));
        content.setSummary(generatedArticle.getSummary());
        content.setContent(requiredText(generatedArticle.getContent(), "AI 返回正文为空，请重试"));
        content.setTags(join(generatedArticle.getTags()));
        content.setKeywords(join(generatedArticle.getKeywords()));
        content.setStatus(STATUS_DRAFT);
        content.setUpdatedBy(currentUserId);
        content.setUpdatedAt(now);
        saveOrUpdate(content);
        return content;
    }

    private Article getRequiredArticle(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "原始文章不存在");
        }
        return article;
    }

    private ArticlePlatformContent getRequiredContent(Long id) {
        ArticlePlatformContent content = getById(id);
        if (content == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "平台发布稿不存在");
        }
        return content;
    }

    private List<String> normalizePlatforms(List<String> platforms) {
        if (platforms == null || platforms.isEmpty()) {
            throw new BusinessException("请选择生成平台");
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String platform : platforms) {
            if (!PLATFORMS.contains(platform)) {
                throw new BusinessException("platform is invalid");
            }
            normalized.add(platform);
        }
        return normalized.stream().toList();
    }

    private String requiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value;
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(",", values);
    }
}
