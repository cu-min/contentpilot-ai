package com.aicontent.marketing.platform.vo;

import com.aicontent.marketing.platform.entity.PlatformAccount;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Data
public class PlatformAccountVO {

    private Long id;

    private String platform;

    private String accountName;

    private String authType;

    private Boolean authConfigConfigured;

    private String authConfigMasked;

    private String defaultPublishMode;

    private Integer enabled;

    private String remark;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static PlatformAccountVO from(PlatformAccount account) {
        PlatformAccountVO vo = new PlatformAccountVO();
        vo.setId(account.getId());
        vo.setPlatform(account.getPlatform());
        vo.setAccountName(account.getAccountName());
        vo.setAuthType(account.getAuthType());
        vo.setAuthConfigConfigured(StringUtils.hasText(account.getAuthConfig()));
        vo.setAuthConfigMasked(StringUtils.hasText(account.getAuthConfig()) ? "******" : "");
        vo.setDefaultPublishMode(account.getDefaultPublishMode());
        vo.setEnabled(account.getEnabled());
        vo.setRemark(account.getRemark());
        vo.setCreatedBy(account.getCreatedBy());
        vo.setUpdatedBy(account.getUpdatedBy());
        vo.setCreatedAt(account.getCreatedAt());
        vo.setUpdatedAt(account.getUpdatedAt());
        return vo;
    }
}
