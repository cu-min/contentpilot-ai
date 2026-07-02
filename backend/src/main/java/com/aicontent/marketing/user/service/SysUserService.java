package com.aicontent.marketing.user.service;

import com.aicontent.marketing.user.dto.CreateUserRequest;
import com.aicontent.marketing.user.dto.UpdateUserRequest;
import com.aicontent.marketing.user.entity.SysUser;
import com.aicontent.marketing.user.vo.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SysUserService extends IService<SysUser> {

    SysUser getByUsername(String username);

    UserVO createUser(CreateUserRequest request);

    Page<UserVO> listUsers(long page, long size, String keyword);

    UserVO updateUser(Long id, UpdateUserRequest request, Long currentUserId);

    void updateStatus(Long id, Integer status, Long currentUserId);

    void validateRole(String role);
}
