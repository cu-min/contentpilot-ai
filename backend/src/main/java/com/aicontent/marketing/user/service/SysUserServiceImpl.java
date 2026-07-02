package com.aicontent.marketing.user.service;

import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.common.result.ResultCode;
import com.aicontent.marketing.user.dto.CreateUserRequest;
import com.aicontent.marketing.user.dto.UpdateUserRequest;
import com.aicontent.marketing.user.entity.SysUser;
import com.aicontent.marketing.user.mapper.SysUserMapper;
import com.aicontent.marketing.user.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;

    private final PasswordEncoder passwordEncoder;

    public SysUserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public SysUser getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username), false);
    }

    @Override
    public UserVO createUser(CreateUserRequest request) {
        validateRole(request.getRole());
        if (getByUsername(request.getUsername()) != null) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setStatus(STATUS_ENABLED);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        save(user);
        return UserVO.from(user);
    }

    @Override
    public Page<UserVO> listUsers(long page, long size, String keyword) {
        Page<SysUser> userPage = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getNickname, keyword));
        }

        Page<SysUser> result = page(userPage, wrapper);
        List<UserVO> records = result.getRecords().stream().map(UserVO::from).toList();
        Page<UserVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public UserVO updateUser(Long id, UpdateUserRequest request, Long currentUserId) {
        SysUser user = getRequiredUser(id);
        if (request.getRole() != null) {
            validateRole(request.getRole());
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            validateStatus(request.getStatus());
            if (Objects.equals(id, currentUserId) && request.getStatus() == STATUS_DISABLED) {
                throw new BusinessException("不允许管理员禁用自己");
            }
            user.setStatus(request.getStatus());
        }
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
        return UserVO.from(user);
    }

    @Override
    public void updateStatus(Long id, Integer status, Long currentUserId) {
        validateStatus(status);
        if (Objects.equals(id, currentUserId) && status == STATUS_DISABLED) {
            throw new BusinessException("不允许管理员禁用自己");
        }

        SysUser user = getRequiredUser(id);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
    }

    @Override
    public void validateRole(String role) {
        if (!"ADMIN".equals(role) && !"OPERATOR".equals(role)) {
            throw new BusinessException("role must be ADMIN or OPERATOR");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED)) {
            throw new BusinessException("status must be 0 or 1");
        }
    }

    private SysUser getRequiredUser(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "user not found");
        }
        return user;
    }
}
