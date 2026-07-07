package com.aicontent.marketing.platform.service;

import com.aicontent.marketing.platform.dto.PlatformAccountQueryRequest;
import com.aicontent.marketing.platform.dto.PlatformAccountSaveRequest;
import com.aicontent.marketing.platform.entity.PlatformAccount;
import com.aicontent.marketing.platform.vo.PlatformAccountVO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PlatformAccountService extends IService<PlatformAccount> {

    List<PlatformAccountVO> listAccounts(PlatformAccountQueryRequest request);

    PlatformAccountVO getAccountDetail(Long id);

    PlatformAccountVO createAccount(PlatformAccountSaveRequest request, Long currentUserId);

    PlatformAccountVO updateAccount(Long id, PlatformAccountSaveRequest request, Long currentUserId);

    void updateStatus(Long id, Integer enabled, Long currentUserId);

    PlatformAccountVO uploadWechatDefaultCover(Long id, MultipartFile file, Long currentUserId);
}
