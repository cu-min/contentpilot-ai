package com.aicontent.marketing.platform.service;

import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.common.result.ResultCode;
import com.aicontent.marketing.platform.dto.PlatformAccountQueryRequest;
import com.aicontent.marketing.platform.dto.PlatformAccountSaveRequest;
import com.aicontent.marketing.platform.entity.PlatformAccount;
import com.aicontent.marketing.platform.mapper.PlatformAccountMapper;
import com.aicontent.marketing.platform.vo.PlatformAccountVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class PlatformAccountServiceImpl extends ServiceImpl<PlatformAccountMapper, PlatformAccount>
        implements PlatformAccountService {

    private static final Set<String> PLATFORMS = Set.of("WECHAT_OFFICIAL", "ZHIHU", "CSDN", "JUEJIN");
    private static final Set<String> AUTH_TYPES = Set.of("APP_SECRET", "COOKIE", "BROWSER_PROFILE", "API_KEY", "MANUAL");
    private static final Set<String> PUBLISH_MODES = Set.of("OFFICIAL_API", "UNOFFICIAL_API", "BROWSER_AUTOMATION", "MANUAL_CONFIRM");

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
        validateRequest(request);
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
        validateRequest(request);
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

    private void validateRequest(PlatformAccountSaveRequest request) {
        validatePlatform(request.getPlatform());
        if (!AUTH_TYPES.contains(request.getAuthType())) {
            throw new BusinessException("authType is invalid");
        }
        if (!PUBLISH_MODES.contains(request.getDefaultPublishMode())) {
            throw new BusinessException("defaultPublishMode is invalid");
        }
        validateEnabled(request.getEnabled());
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
}
