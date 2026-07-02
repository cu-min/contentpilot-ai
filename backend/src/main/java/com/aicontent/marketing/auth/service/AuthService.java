package com.aicontent.marketing.auth.service;

import com.aicontent.marketing.auth.dto.LoginRequest;
import com.aicontent.marketing.auth.dto.LoginResponse;
import com.aicontent.marketing.user.vo.UserVO;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UserVO getCurrentUser();
}
