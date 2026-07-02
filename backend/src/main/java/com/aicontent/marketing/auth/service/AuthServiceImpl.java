package com.aicontent.marketing.auth.service;

import com.aicontent.marketing.auth.dto.LoginRequest;
import com.aicontent.marketing.auth.dto.LoginResponse;
import com.aicontent.marketing.auth.security.LoginUser;
import com.aicontent.marketing.auth.util.JwtUtil;
import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.common.result.ResultCode;
import com.aicontent.marketing.user.entity.SysUser;
import com.aicontent.marketing.user.service.SysUserService;
import com.aicontent.marketing.user.vo.UserVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(
            SysUserService sysUserService,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.sysUserService = sysUserService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserService.getByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, UserVO.from(user));
    }

    @Override
    public UserVO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return UserVO.from(loginUser.getUser());
    }
}
