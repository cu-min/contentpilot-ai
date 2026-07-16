package com.aicontent.marketing.platform.service;

import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.common.result.ResultCode;
import com.aicontent.marketing.platform.dto.PlatformAccountQueryRequest;
import com.aicontent.marketing.platform.dto.PlatformAccountSaveRequest;
import com.aicontent.marketing.platform.entity.PlatformAccount;
import com.aicontent.marketing.platform.mapper.PlatformAccountMapper;
import com.aicontent.marketing.platform.vo.PlatformAccountVO;
import com.aicontent.marketing.publish.publisher.browser.BrowserPublisherConfig;
import com.aicontent.marketing.publish.publisher.wechat.WechatAccessTokenCache;
import com.aicontent.marketing.publish.publisher.wechat.WechatAuthConfig;
import com.aicontent.marketing.publish.publisher.wechat.WechatClient;
import com.aicontent.marketing.publish.publisher.wechat.WechatMaterialUploadResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PlatformAccountServiceImpl extends ServiceImpl<PlatformAccountMapper, PlatformAccount>
        implements PlatformAccountService {

    private static final Set<String> PLATFORMS = Set.of("WECHAT_OFFICIAL", "ZHIHU", "CSDN", "JUEJIN");
    private static final Set<String> AUTH_TYPES = Set.of("APP_SECRET", "COOKIE", "BROWSER_PROFILE", "API_KEY", "MANUAL");
    private static final Set<String> PUBLISH_MODES = Set.of("OFFICIAL_API", "UNOFFICIAL_API", "BROWSER_AUTOMATION");
    private static final Set<String> BROWSER_PLATFORMS = Set.of("CSDN", "ZHIHU");
    private static final Set<String> BROWSER_PUBLISH_MODES = Set.of("BROWSER_AUTOMATION");
    private static final Map<String, Set<String>> PLATFORM_AUTH_TYPES = Map.of(
            "WECHAT_OFFICIAL", Set.of("APP_SECRET"),
            "JUEJIN", Set.of("COOKIE"),
            "CSDN", Set.of("BROWSER_PROFILE"),
            "ZHIHU", Set.of("BROWSER_PROFILE")
    );
    private static final Map<String, Set<String>> PLATFORM_PUBLISH_MODES = Map.of(
            "WECHAT_OFFICIAL", Set.of("OFFICIAL_API"),
            "JUEJIN", Set.of("UNOFFICIAL_API"),
            "CSDN", BROWSER_PUBLISH_MODES,
            "ZHIHU", BROWSER_PUBLISH_MODES
    );
    private static final Set<String> DEFAULT_COVER_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp");
    private static final long DEFAULT_COVER_MAX_SIZE = 2 * 1024 * 1024;
    private static final String AUTH_CONFIG_JSON_ERROR = "认证配置必须是合法 JSON，请检查双引号、逗号和字段格式";

    private final ObjectMapper objectMapper;
    private final WechatAccessTokenCache wechatAccessTokenCache;
    private final WechatClient wechatClient;

    public PlatformAccountServiceImpl(
            ObjectMapper objectMapper,
            WechatAccessTokenCache wechatAccessTokenCache,
            WechatClient wechatClient
    ) {
        this.objectMapper = objectMapper;
        this.wechatAccessTokenCache = wechatAccessTokenCache;
        this.wechatClient = wechatClient;
    }

    @Override
    public List<PlatformAccountVO> listAccounts(PlatformAccountQueryRequest request) {
        LambdaQueryWrapper<PlatformAccount> wrapper = new LambdaQueryWrapper<PlatformAccount>()
                .orderByAsc(PlatformAccount::getPlatform)
                .orderByDesc(PlatformAccount::getEnabled)
                .orderByDesc(PlatformAccount::getUpdatedAt)
                .orderByDesc(PlatformAccount::getId);

        if (StringUtils.hasText(request.getPlatform())) {
            validatePlatform(request.getPlatform());
            wrapper.eq(PlatformAccount::getPlatform, request.getPlatform());
        }
        if (request.getEnabled() != null) {
            validateEnabled(request.getEnabled());
            wrapper.eq(PlatformAccount::getEnabled, request.getEnabled());
        }

        return list(wrapper).stream().map(PlatformAccountVO::from).toList();
    }

    @Override
    public PlatformAccountVO getAccountDetail(Long id) {
        return PlatformAccountVO.from(getRequiredAccount(id));
    }

    @Override
    @Transactional
    public PlatformAccountVO createAccount(PlatformAccountSaveRequest request, Long currentUserId) {
        validateRequest(request, true);
        LocalDateTime now = LocalDateTime.now();
        PlatformAccount account = new PlatformAccount();
        fillAccount(account, request, currentUserId, now, true);
        account.setCreatedBy(currentUserId);
        account.setCreatedAt(now);
        save(account);
        disableOtherAccountsIfNeeded(account);
        return PlatformAccountVO.from(account);
    }

    @Override
    @Transactional
    public PlatformAccountVO updateAccount(Long id, PlatformAccountSaveRequest request, Long currentUserId) {
        validateRequest(request, false);
        PlatformAccount account = getRequiredAccount(id);
        fillAccount(account, request, currentUserId, LocalDateTime.now(), false);
        updateById(account);
        disableOtherAccountsIfNeeded(account);
        return PlatformAccountVO.from(account);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer enabled, Long currentUserId) {
        validateEnabled(enabled);
        PlatformAccount account = getRequiredAccount(id);
        account.setEnabled(enabled);
        account.setUpdatedBy(currentUserId);
        account.setUpdatedAt(LocalDateTime.now());
        updateById(account);
        disableOtherAccountsIfNeeded(account);
    }

    @Override
    @Transactional
    public PlatformAccountVO uploadWechatDefaultCover(Long id, MultipartFile file, Long currentUserId) {
        PlatformAccount account = getRequiredAccount(id);
        if (!"WECHAT_OFFICIAL".equals(account.getPlatform())) {
            throw new BusinessException("仅微信公众号账号支持上传默认封面");
        }
        validateDefaultCoverFile(file);

        WechatAuthConfig config = WechatAuthConfig.parseForDefaultCoverUpload(account.getAuthConfig(), objectMapper);
        String accessToken = wechatAccessTokenCache.getToken(config.appId(), config.appSecret(), wechatClient);
        WechatMaterialUploadResponse response = wechatClient.uploadPermanentImageMaterial(accessToken, file);

        account.setAuthConfig(updateDefaultThumbMediaId(account.getAuthConfig(), response.mediaId()));
        account.setUpdatedBy(currentUserId);
        account.setUpdatedAt(LocalDateTime.now());
        updateById(account);
        return PlatformAccountVO.from(account);
    }

    private void fillAccount(
            PlatformAccount account,
            PlatformAccountSaveRequest request,
            Long currentUserId,
            LocalDateTime now,
            boolean creating
    ) {
        account.setPlatform(request.getPlatform());
        account.setAccountName(request.getAccountName());
        account.setAuthType(request.getAuthType());
        if (creating || StringUtils.hasText(request.getAuthConfig())) {
            account.setAuthConfig(request.getAuthConfig());
        }
        account.setDefaultPublishMode(request.getDefaultPublishMode());
        account.setEnabled(request.getEnabled());
        account.setRemark(request.getRemark());
        account.setUpdatedBy(currentUserId);
        account.setUpdatedAt(now);
    }

    private void disableOtherAccountsIfNeeded(PlatformAccount account) {
        if (!Integer.valueOf(1).equals(account.getEnabled())) {
            return;
        }
        update(new LambdaUpdateWrapper<PlatformAccount>()
                .eq(PlatformAccount::getPlatform, account.getPlatform())
                .ne(PlatformAccount::getId, account.getId())
                .set(PlatformAccount::getEnabled, 0)
                .set(PlatformAccount::getUpdatedAt, LocalDateTime.now()));
    }

    private PlatformAccount getRequiredAccount(Long id) {
        PlatformAccount account = getById(id);
        if (account == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "平台账号不存在");
        }
        return account;
    }

    private void validateRequest(PlatformAccountSaveRequest request, boolean creating) {
        validatePlatform(request.getPlatform());
        if (!AUTH_TYPES.contains(request.getAuthType())) {
            throw new BusinessException("authType is invalid");
        }
        if (!PUBLISH_MODES.contains(request.getDefaultPublishMode())) {
            throw new BusinessException("defaultPublishMode is invalid");
        }
        if (!PLATFORM_AUTH_TYPES.get(request.getPlatform()).contains(request.getAuthType())) {
            throw new BusinessException("当前平台不支持该认证方式");
        }
        if (!PLATFORM_PUBLISH_MODES.get(request.getPlatform()).contains(request.getDefaultPublishMode())) {
            throw new BusinessException("当前平台不支持该发布方式");
        }
        validateEnabled(request.getEnabled());
        validateAuthConfig(request, creating);
    }

    private void validatePlatform(String platform) {
        if (!PLATFORMS.contains(platform)) {
            throw new BusinessException("platform is invalid");
        }
    }

    private void validateEnabled(Integer enabled) {
        if (!Integer.valueOf(0).equals(enabled) && !Integer.valueOf(1).equals(enabled)) {
            throw new BusinessException("enabled is invalid");
        }
    }

    private void validateAuthConfig(PlatformAccountSaveRequest request, boolean creating) {
        if (!StringUtils.hasText(request.getAuthConfig())) {
            if (creating) {
                throw new BusinessException("认证配置不能为空");
            }
            return;
        }
        JsonNode root = parseAuthConfig(request.getAuthConfig());
        if ("JUEJIN".equals(request.getPlatform()) && "UNOFFICIAL_API".equals(request.getDefaultPublishMode())) {
            validateJuejinAuthConfig(root);
        }
        if ("WECHAT_OFFICIAL".equals(request.getPlatform()) && "OFFICIAL_API".equals(request.getDefaultPublishMode())) {
            WechatAuthConfig.parse(request.getAuthConfig(), objectMapper);
        }
        if (BROWSER_PLATFORMS.contains(request.getPlatform()) && BROWSER_PUBLISH_MODES.contains(request.getDefaultPublishMode())) {
            BrowserPublisherConfig.parse(request.getAuthConfig(), objectMapper, defaultBrowserEditorUrl(request.getPlatform()));
        }
    }

    private String defaultBrowserEditorUrl(String platform) {
        if ("CSDN".equals(platform)) {
            return "https://editor.csdn.net/md/?not_checkout=1";
        }
        if ("ZHIHU".equals(platform)) {
            return "https://zhuanlan.zhihu.com/write";
        }
        return "";
    }

    private JsonNode parseAuthConfig(String authConfig) {
        try {
            return objectMapper.readTree(authConfig);
        } catch (Exception exception) {
            throw new BusinessException(AUTH_CONFIG_JSON_ERROR);
        }
    }

    private void validateJuejinAuthConfig(JsonNode root) {
        if (!hasTextField(root, "cookie")) {
            throw new BusinessException("掘金 Cookie 未配置，请在认证配置中填写 cookie");
        }
        if (!hasTextField(root, "defaultCategoryId")) {
            throw new BusinessException("掘金默认分类 ID 未配置，请从 article_draft/update 请求 Payload 的 category_id 中获取");
        }
        if (!root.path("defaultTagIds").isArray() || root.path("defaultTagIds").isEmpty()) {
            throw new BusinessException("掘金默认标签 ID 未配置，请至少填写一个 defaultTagIds");
        }
    }

    private boolean hasTextField(JsonNode root, String fieldName) {
        JsonNode value = root.path(fieldName);
        return value.isTextual() && StringUtils.hasText(value.asText());
    }

    private void validateDefaultCoverFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的封面图片");
        }
        if (file.getSize() > DEFAULT_COVER_MAX_SIZE) {
            throw new BusinessException("封面图片大小不能超过 2MB");
        }
        String extension = getFileExtension(file.getOriginalFilename());
        if (!DEFAULT_COVER_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 jpg、jpeg、png、gif、bmp 格式图片");
        }
    }

    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String updateDefaultThumbMediaId(String rawConfig, String mediaId) {
        try {
            JsonNode root = objectMapper.readTree(rawConfig);
            if (!root.isObject()) {
                throw new BusinessException("微信公众号认证配置格式错误");
            }
            ObjectNode objectNode = (ObjectNode) root;
            objectNode.put("defaultThumbMediaId", mediaId);
            return objectMapper.writeValueAsString(objectNode);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("微信公众号认证配置格式错误");
        }
    }
}
